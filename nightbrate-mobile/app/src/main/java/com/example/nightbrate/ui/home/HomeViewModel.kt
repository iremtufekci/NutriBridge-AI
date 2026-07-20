package com.example.nightbrate.ui.home // Ana sayfa ViewModel paketi

import androidx.lifecycle.LiveData // Gözlemlenebilir veri
import androidx.lifecycle.MutableLiveData // Değiştirilebilir LiveData
import androidx.lifecycle.ViewModel // MVVM ViewModel tabanı

class HomeViewModel : ViewModel() { // Ana sayfa fragment veri sağlayıcısı

    private val _text = MutableLiveData<String>().apply { // Dahili metin durumu
        value = "This is home Fragment" // Varsayılan gösterim metni
    }
    val text: LiveData<String> = _text // Dışarıya salt okunur metin
}
