package com.example.camera.recognition

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecognitionUiState(
    val current: RecognitionResult? = null,
    val history: List<RecognitionResult> = emptyList(),
    val decodingImage: Boolean = false,
    val message: String? = null
)

class RecognitionViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ScanHistoryStore(app)
    private val _state = MutableStateFlow(RecognitionUiState())
    val state: StateFlow<RecognitionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { store.load() }
            _state.update { it.copy(history = items) }
        }
    }

    /** Called from the analysis thread. */
    fun onDetected(result: RecognitionResult) {
        if (result.text.isBlank()) return
        var accepted = false
        _state.update { s ->
            if (s.current != null) s else {
                accepted = true
                s.copy(current = result, history = (listOf(result) + s.history).take(200), message = null)
            }
        }
        if (accepted) persist()
    }

    fun dismissCurrent() = _state.update { it.copy(current = null) }

    fun show(item: RecognitionResult) = _state.update { it.copy(current = item) }

    fun delete(item: RecognitionResult) {
        _state.update { s -> s.copy(history = s.history.filterNot { it.timestamp == item.timestamp && it.text == item.text }) }
        persist()
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
        persist()
    }

    fun decodeImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(decodingImage = true, message = null) }
            val result = withContext(Dispatchers.Default) {
                runCatching { ImageBarcodeDecoder.decode(getApplication(), uri) }.getOrNull()
            }
            if (result == null) {
                _state.update { it.copy(decodingImage = false, message = "No QR code or barcode found in that image.") }
            } else {
                _state.update { s -> s.copy(decodingImage = false, current = result, history = (listOf(result) + s.history).take(200)) }
                persist()
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun persist() {
        val items = _state.value.history
        viewModelScope.launch(Dispatchers.IO) { store.save(items) }
    }
}
