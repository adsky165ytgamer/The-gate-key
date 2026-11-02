package com.example.camera.scanner

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.core.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ScannerStage { Camera, Crop, Review }

data class ScannerMessage(val text: String, val isError: Boolean)

data class ScannerUiState(
    val stage: ScannerStage = ScannerStage.Camera,
    val pages: List<ScanPage> = emptyList(),
    val selectedId: Long? = null,
    val editing: ScanPage? = null,
    val editingIsNew: Boolean = false,
    val editingInsertAt: Int? = null,
    val busyLabel: String? = null,
    val message: ScannerMessage? = null,
    val autoCapture: Boolean = true
) {
    val selected: ScanPage? get() = pages.firstOrNull { it.id == selectedId } ?: pages.firstOrNull()
}

/** What the live detector currently sees. Kept separate so the preview overlay updates without recomposing the screen. */
data class LiveDetection(
    val quad: Quad? = null,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val stability: Float = 0f
)

class ScannerViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext
    private val detector = DocumentDetector()
    private val stabilizer = QuadStabilizer()
    val renderer = ScanRenderer()

    private val scansDir = File(context.cacheDir, "scans").apply { mkdirs() }
    private var nextId = System.currentTimeMillis()

    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    private val _live = MutableStateFlow(LiveDetection())
    val live: StateFlow<LiveDetection> = _live.asStateFlow()

    private val _captureRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val captureRequests: SharedFlow<Unit> = _captureRequests.asSharedFlow()

    @Volatile private var captureInFlight = false
    @Volatile private var autoCooldownUntil = 0L

    // ── live detection ───────────────────────────────────────────────────────

    /** Called from the analysis thread. */
    fun onFrame(detected: Quad?, frameWidth: Int, frameHeight: Int) {
        val smoothed = stabilizer.update(detected)
        val stability = (stabilizer.stableFrames / AUTO_CAPTURE_FRAMES.toFloat()).coerceIn(0f, 1f)
        _live.value = LiveDetection(smoothed, frameWidth, frameHeight, if (smoothed == null) 0f else stability)

        val s = _state.value
        if (s.autoCapture && s.stage == ScannerStage.Camera && smoothed != null &&
            stabilizer.stableFrames >= AUTO_CAPTURE_FRAMES &&
            !captureInFlight && SystemClock.elapsedRealtime() >= autoCooldownUntil
        ) {
            captureInFlight = true
            stabilizer.reset()
            _captureRequests.tryEmit(Unit)
        }
    }

    fun setAutoCapture(enabled: Boolean) = _state.update { it.copy(autoCapture = enabled) }

    fun newCaptureFile(): File = File(scansDir, "capture_${nextId++}.jpg")

    /** The UI is about to take a photo manually. */
    fun markCaptureStarted(): Boolean {
        if (captureInFlight) return false
        captureInFlight = true
        return true
    }

    fun onCaptureFailed(message: String) {
        captureInFlight = false
        showMessage(message, isError = true)
    }

    fun onPhotoCaptured(file: File) = beginEditing(file)

    fun importImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(busyLabel = "Importing") }
            val copied = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        val target = newCaptureFile()
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            target.outputStream().use { input.copyTo(it) }
                        } ?: return@runCatching null
                        target
                    }.getOrNull()
                }
            }
            _state.update { it.copy(busyLabel = null) }
            if (copied.isEmpty()) {
                showMessage("Could not read the selected image.", isError = true)
                return@launch
            }
            // Keep the first selected image in the existing crop/confirm workflow, but reserve
            // its original position so confirming it cannot append it after later selections.
            val insertionIndex = _state.value.pages.size
            copied.drop(1).forEach { file -> addPageDirect(file) }
            beginEditing(copied.first(), insertionIndex)
        }
    }

    private suspend fun addPageDirect(file: File) {
        val quad = withContext(Dispatchers.IO) {
            BitmapLoader.decode(file, DETECT_EDGE)?.let { bmp -> detector.detect(bmp).also { bmp.recycle() } }
        } ?: Quad.inset()
        val page = ScanPage(nextId++, file, quad)
        _state.update { it.copy(pages = it.pages + page, selectedId = page.id) }
    }

    private fun beginEditing(file: File, insertAt: Int? = null) {
        viewModelScope.launch {
            _state.update { it.copy(busyLabel = "Finding page edges") }
            val quad = withContext(Dispatchers.IO) {
                BitmapLoader.decode(file, DETECT_EDGE)?.let { bmp -> detector.detect(bmp).also { bmp.recycle() } }
            } ?: Quad.inset()
            val page = ScanPage(nextId++, file, quad)
            captureInFlight = false
            autoCooldownUntil = SystemClock.elapsedRealtime() + 1500
            stabilizer.reset()
            _state.update {
                it.copy(
                    stage = ScannerStage.Crop,
                    editing = page,
                    editingIsNew = true,
                    editingInsertAt = insertAt,
                    busyLabel = null
                )
            }
        }
    }

    // ── crop ─────────────────────────────────────────────────────────────────

    fun updateEditingQuad(quad: Quad) = _state.update { s ->
        s.copy(editing = s.editing?.copy(quad = quad))
    }

    fun redetectEditing() {
        val page = _state.value.editing ?: return
        viewModelScope.launch {
            val quad = withContext(Dispatchers.IO) {
                BitmapLoader.decode(page.file, DETECT_EDGE)?.let { bmp -> detector.detect(bmp).also { bmp.recycle() } }
            }
            _state.update { s ->
                s.copy(editing = s.editing?.copy(quad = quad ?: Quad.inset()))
            }
            if (quad == null) showMessage("No page edges found. Drag the corners to fit the page.", isError = false)
        }
    }

    fun useFullImage() = updateEditingQuad(Quad.FULL)

    fun confirmEditing() {
        val s = _state.value
        val page = s.editing ?: return
        renderer.evict(page.id)
        if (s.editingIsNew) {
            _state.update { state ->
                val pages = state.pages.toMutableList()
                val index = (state.editingInsertAt ?: pages.size).coerceIn(0, pages.size)
                pages.add(index, page)
                state.copy(pages = pages, selectedId = page.id, editing = null, editingInsertAt = null, stage = ScannerStage.Review)
            }
        } else {
            _state.update { st ->
                st.copy(
                    pages = st.pages.map { if (it.id == page.id) page else it },
                    editing = null,
                    editingInsertAt = null,
                    stage = ScannerStage.Review
                )
            }
        }
    }

    fun cancelEditing() {
        val s = _state.value
        val page = s.editing
        if (s.editingIsNew && page != null) page.file.delete()
        _state.update {
            it.copy(
                editing = null,
                editingInsertAt = null,
                stage = if (it.pages.isEmpty()) ScannerStage.Camera else ScannerStage.Review
            )
        }
    }

    // ── review ───────────────────────────────────────────────────────────────

    fun showReview() {
        if (_state.value.pages.isNotEmpty()) _state.update { it.copy(stage = ScannerStage.Review) }
    }

    fun backToCamera() {
        stabilizer.reset()
        autoCooldownUntil = SystemClock.elapsedRealtime() + 1200
        _state.update { it.copy(stage = ScannerStage.Camera, message = null) }
    }

    fun selectPage(id: Long) = _state.update { it.copy(selectedId = id) }

    fun setFilter(id: Long, filter: ScanFilter) = updatePage(id) { it.copy(filter = filter) }

    fun rotate(id: Long) = updatePage(id) { it.copy(rotation = (it.rotation + 90) % 360) }

    fun adjustCrop(id: Long) {
        val page = _state.value.pages.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(stage = ScannerStage.Crop, editing = page, editingIsNew = false) }
    }

    fun deletePage(id: Long) {
        val page = _state.value.pages.firstOrNull { it.id == id } ?: return
        renderer.evict(id)
        page.file.delete()
        _state.update { s ->
            val remaining = s.pages.filterNot { it.id == id }
            s.copy(
                pages = remaining,
                selectedId = remaining.firstOrNull { it.id == s.selectedId }?.id ?: remaining.lastOrNull()?.id,
                stage = if (remaining.isEmpty()) ScannerStage.Camera else s.stage
            )
        }
    }

    fun movePage(id: Long, delta: Int) = _state.update { s ->
        val index = s.pages.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in s.pages.indices) return@update s
        val list = s.pages.toMutableList()
        list.add(target, list.removeAt(index))
        s.copy(pages = list)
    }

    fun discardAll() {
        _state.value.pages.forEach { it.file.delete() }
        _state.value.editing?.file?.delete()
        renderer.clear()
        stabilizer.reset()
        _state.value = ScannerUiState(autoCapture = _state.value.autoCapture)
    }

    private fun updatePage(id: Long, transform: (ScanPage) -> ScanPage) = _state.update { s ->
        s.copy(pages = s.pages.map { if (it.id == id) transform(it).also { _ -> renderer.evict(id) } else it })
    }

    // ── export ───────────────────────────────────────────────────────────────

    fun exportPdf(uri: Uri) {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        runExport("Creating PDF", "PDF saved.") {
            context.contentResolver.openOutputStream(uri)?.use { PdfExport.writePdf(pages, renderer, it) }
                ?: error("Cannot open destination")
        }
    }

    fun exportImage(uri: Uri) {
        val page = _state.value.selected ?: return
        runExport("Saving image", "Image saved.") {
            val ok = context.contentResolver.openOutputStream(uri)?.use { PdfExport.writeJpeg(page, renderer, it) }
                ?: error("Cannot open destination")
            if (!ok) error("Encoding failed")
        }
    }

    private fun runExport(label: String, success: String, work: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busyLabel = label, message = null) }
            val result = withContext(Dispatchers.IO) { runCatching(work) }
            _state.update { it.copy(busyLabel = null) }
            result.onSuccess { showMessage(success, isError = false) }
                .onFailure { showMessage("Export failed. ${it.message.orEmpty()}".trim(), isError = true) }
        }
    }

    suspend fun preview(page: ScanPage, maxEdge: Int): Bitmap? =
        withContext(Dispatchers.IO) { renderer.preview(page, maxEdge) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun showMessage(text: String, isError: Boolean) =
        _state.update { it.copy(message = ScannerMessage(text, isError)) }

    override fun onCleared() {
        renderer.clear()
        super.onCleared()
    }

    private companion object {
        const val AUTO_CAPTURE_FRAMES = 7
        const val DETECT_EDGE = 1600
    }
}
