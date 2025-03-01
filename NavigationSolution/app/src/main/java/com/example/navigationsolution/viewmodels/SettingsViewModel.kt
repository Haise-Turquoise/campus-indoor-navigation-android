package com.example.navigationsolution.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel: ViewModel() {
    private val _altColours = MutableLiveData(false)
    val altColours: LiveData<Boolean> = _altColours

    fun swapColours() {
        _altColours.value = !_altColours.value!!
    }

    private val _textScale = MutableLiveData(1f)
    val textScale: LiveData<Float> = _textScale

    fun updateTextScale(scale: Float) {
        _textScale.value = scale
    }
}