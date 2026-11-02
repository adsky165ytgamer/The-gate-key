package com.example.camera.core

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** What a tool needs from the camera. */
data class CameraConfig(
    val capture: Boolean = false,
    val analyzer: ImageAnalysis.Analyzer? = null,
    val analysisSize: Size = Size(1280, 720),
    val implementationMode: PreviewView.ImplementationMode = PreviewView.ImplementationMode.PERFORMANCE
)

/**
 * One reusable camera engine for every Camera Tool.
 *
 * It owns the CameraX [LifecycleCameraController] (preview, still capture, frame analysis, zoom, torch,
 * exposure, focus) and publishes the live camera state as [capabilities]. Tools never touch CameraX directly.
 */
class CameraSession(context: Context) {

    private val appContext = context.applicationContext
    private val controller = LifecycleCameraController(appContext)
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    private var previewView: PreviewView? = null
    private var attached = false

    private val zoomObserver = Observer<ZoomState> { z ->
        _capabilities.update {
            it.copy(
                isBound = true,
                zoomRatio = z.zoomRatio,
                minZoomRatio = z.minZoomRatio,
                maxZoomRatio = z.maxZoomRatio
            )
        }
    }
    private val torchObserver = Observer<Int> { state ->
        _capabilities.update { it.copy(torchOn = state == TorchState.ON) }
    }

    init {
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // Gestures are handled by CameraPreview so tap rings and zoom stay consistent with the app UI.
        controller.isPinchToZoomEnabled = false
        controller.isTapToFocusEnabled = false
    }

    fun attach(owner: LifecycleOwner, view: PreviewView, config: CameraConfig) {
        if (attached) detach()
        previewView = view
        view.implementationMode = config.implementationMode
        view.scaleType = PreviewView.ScaleType.FILL_CENTER

        var useCases = 0
        if (config.capture) useCases = useCases or CameraController.IMAGE_CAPTURE
        if (config.analyzer != null) useCases = useCases or CameraController.IMAGE_ANALYSIS
        controller.setEnabledUseCases(useCases)

        if (config.capture) {
            controller.imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
        if (config.analyzer != null) {
            controller.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            controller.imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(config.analysisSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            controller.setImageAnalysisAnalyzer(analysisExecutor, config.analyzer)
        }

        view.controller = controller
        controller.bindToLifecycle(owner)

        controller.zoomState.observeForever(zoomObserver)
        controller.torchState.observeForever(torchObserver)
        attached = true

        // Capability info that is not exposed as LiveData is refreshed when the camera becomes available.
        mainExecutor.execute { refreshStaticCapabilities() }
        view.postDelayed({ refreshStaticCapabilities() }, 400)
    }

    fun detach() {
        if (!attached) return
        attached = false
        runCatching { controller.zoomState.removeObserver(zoomObserver) }
        runCatching { controller.torchState.removeObserver(torchObserver) }
        runCatching { controller.clearImageAnalysisAnalyzer() }
        runCatching { controller.unbind() }
        previewView?.controller = null
        previewView = null
        _capabilities.value = CameraCapabilities()
    }

    fun release() {
        detach()
        analysisExecutor.shutdown()
    }

    private fun refreshStaticCapabilities() {
        val info = controller.cameraInfo ?: return
        val exposure = info.exposureState
        val range = exposure.exposureCompensationRange
        _capabilities.update {
            it.copy(
                isBound = true,
                hasFlash = info.hasFlashUnit(),
                exposureSupported = exposure.isExposureCompensationSupported,
                exposureIndex = exposure.exposureCompensationIndex,
                exposureMin = range.lower,
                exposureMax = range.upper
            )
        }
    }

    // ── controls ──────────────────────────────────────────────────────────────

    fun setZoomRatio(ratio: Float) {
        val caps = _capabilities.value
        controller.setZoomRatio(ratio.coerceIn(caps.minZoomRatio, caps.maxZoomRatio.coerceAtLeast(caps.minZoomRatio)))
    }

    fun setTorch(enabled: Boolean) {
        if (_capabilities.value.hasFlash) controller.enableTorch(enabled)
    }

    fun setExposureIndex(index: Int) {
        val caps = _capabilities.value
        if (!caps.exposureSupported) return
        val clamped = index.coerceIn(caps.exposureMin, caps.exposureMax)
        controller.cameraControl?.setExposureCompensationIndex(clamped)
        _capabilities.update { it.copy(exposureIndex = clamped) }
    }

    /** Focus + meter at a view-space point. */
    fun focusAt(x: Float, y: Float) {
        val view = previewView ?: return
        val point = view.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()
        controller.cameraControl?.startFocusAndMetering(action)
    }

    fun focusCenter() {
        val view = previewView ?: return
        focusAt(view.width / 2f, view.height / 2f)
    }

    /** Frozen copy of what the preview currently shows, or null if the preview has not started. */
    fun previewBitmap() = previewView?.bitmap

    /** Takes a full-resolution still and writes it (with EXIF orientation) to [file]. */
    suspend fun capture(file: File): Result<File> = suspendCancellableCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        controller.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                if (cont.isActive) cont.resume(Result.success(file))
            }

            override fun onError(exception: ImageCaptureException) {
                if (cont.isActive) cont.resume(Result.failure(exception))
            }
        })
    }
}
