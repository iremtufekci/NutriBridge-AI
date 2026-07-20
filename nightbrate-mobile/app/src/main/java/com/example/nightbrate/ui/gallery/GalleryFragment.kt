package com.example.nightbrate.ui.gallery // Galeri fragment paketi

import android.os.Bundle // Fragment durum verisi
import android.view.LayoutInflater // XML şişirme
import android.view.View // Görünüm referansı
import android.view.ViewGroup // Konteyner grup
import android.widget.TextView // Metin etiketi
import androidx.fragment.app.Fragment // Temel fragment sınıfı
import androidx.lifecycle.ViewModelProvider // ViewModel fabrikası
import com.example.nightbrate.databinding.FragmentGalleryBinding // View binding

class GalleryFragment : Fragment() { // Galeri ekran parçası

    private var _binding: FragmentGalleryBinding? = null // Nullable binding referansı

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!! // Güvenli binding erişimi

    override fun onCreateView( // Görünüm oluşturulurken
        inflater: LayoutInflater, // Layout şişirici
        container: ViewGroup?, // Üst konteyner
        savedInstanceState: Bundle? // Kayıtlı durum
    ): View {
        val galleryViewModel = // ViewModel örneği al
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false) // Binding şişir
        val root: View = binding.root // Kök görünüm

        val textView: TextView = binding.textGallery // Metin alanı referansı
        galleryViewModel.text.observe(viewLifecycleOwner) { // Metin değişimini izle
            textView.text = it // UI'ya yansıt
        }
        return root // Oluşturulan kök görünümü döndür
    }

    override fun onDestroyView() { // Görünüm yok edilirken
        super.onDestroyView() // Üst sınıf temizliği
        _binding = null // Bellek sızıntısını önle
    }
}
