package com.example.nightbrate.ui.slideshow // Slayt gösterisi ViewModel paketi

import androidx.lifecycle.LiveData // Gözlemlenebilir veri
import androidx.lifecycle.MutableLiveData // Değiştirilebilir LiveData
import androidx.lifecycle.ViewModel // MVVM ViewModel tabanı

class SlideshowViewModel : ViewModel() { // Slayt fragment veri sağlayıcısı

    private val _text = MutableLiveData<String>().apply { // Dahili metin durumu
        value = "This is slideshow Fragment" // Varsayılan gösterim metni
    }
    val text: LiveData<String> = _text // Dışarıya salt okunur metin
}
