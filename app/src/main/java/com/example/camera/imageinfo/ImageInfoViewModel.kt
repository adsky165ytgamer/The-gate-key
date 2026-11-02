package com.example.camera.imageinfo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImageInfoState {
    data object Idle : ImageInfoState
    data object Loading : ImageInfoState
    data class Loaded(val info: ImageInfo) : ImageInfoState
    data class Failed(val message: String) : ImageInfoState
}

class ImageInfoViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ImageInfoState>(ImageInfoState.Idle)
    val state: StateFlow<ImageInfoState> = _state.asStateFlow()

    fun load(uri: Uri) {
        _state.value = ImageInfoState.Loading
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { runCatching { ImageMetadataReader.read(getApplication(), uri) }.getOrNull() }
            _state.value = info?.let { ImageInfoState.Loaded(it) }
                ?: ImageInfoState.Failed("That file could not be read as an image.")
        }
    }

    fun reset() { _state.value = ImageInfoState.Idle }
}
