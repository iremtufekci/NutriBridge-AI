package com.example.nightbrate.ui.home // Ana sayfa fragment paketi

import android.os.Bundle // Fragment durum verisi
import android.view.LayoutInflater // XML şişirme
import android.view.View // Görünüm referansı
import android.view.ViewGroup // Konteyner grup
import android.widget.TextView // Metin etiketi
import androidx.fragment.app.Fragment // Temel fragment sınıfı
import androidx.lifecycle.ViewModelProvider // ViewModel fabrikası
import com.example.nightbrate.databinding.FragmentHomeBinding // View binding

class HomeFragment : Fragment() { // Ana sayfa ekran parçası

    private var _binding: FragmentHomeBinding? = null // Nullable binding referansı

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!! // Güvenli binding erişimi

    override fun onCreateView( // Görünüm oluşturulurken
        inflater: LayoutInflater, // Layout şişirici
        container: ViewGroup?, // Üst konteyner
        savedInstanceState: Bundle? // Kayıtlı durum
    ): View {
        val homeViewModel = // ViewModel örneği al
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false) // Binding şişir
        val root: View = binding.root // Kök görünüm

        val textView: TextView = binding.textHome // Metin alanı referansı
        homeViewModel.text.observe(viewLifecycleOwner) { // Metin değişimini izle
            textView.text = it // UI'ya yansıt
        }
        return root // Oluşturulan kök görünümü döndür
    }

    override fun onDestroyView() { // Görünüm yok edilirken
        super.onDestroyView() // Üst sınıf temizliği
        _binding = null // Bellek sızıntısını önle
    }
}
