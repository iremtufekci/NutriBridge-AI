package com.example.nightbrate // Paket tanımı

import java.util.regex.Pattern // Regex desen eşleştirme

/** Eski kayıtlarda ASCII veya İngilizce kalan aktivite açıklamalarını düzgün Türkçe gösterime çevirir. */
object ActivityDescriptionNormalize { // Aktivite metni normalleştirici
    private val aiMutfakPrefix = Pattern.compile("^AI Mutfak:\\s*", Pattern.CASE_INSENSITIVE) // AI Mutfak öneki
    private val secilenWord = Pattern.compile("\\bsecilen\\b") // ASCII "secilen" kelimesi
    private val paylasildiWord = Pattern.compile("paylasildi", Pattern.CASE_INSENSITIVE) // ASCII paylaşıldı
    private val paylasmakWord = Pattern.compile("paylasmak", Pattern.CASE_INSENSITIVE) // ASCII paylaşmak
    private val ogunTam = Pattern.compile("Ogun tamamlandi:") // ASCII öğün tamamlandı
    private val ogunFoto = Pattern.compile("Ogun fotografi yuklendi \\(AI analizi\\)") // ASCII öğün fotoğrafı
    private val aiAnal = Pattern.compile("\\bAI analizi\\b") // AI analizi ifadesi

    fun toDisplay(desc: String?): String { // Ekranda gösterilecek metni üret
        if (desc.isNullOrEmpty()) return "" // Boşsa boş döndür
        when (desc) { // Bilinen sabit eşleşmeler
            "AI Mutfak: secilen tarifler diyetisyenle paylasildi",
            "AI Mutfak: seçilen tarifler diyetisyenle paylaşıldı",
            "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı" ->
                return "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı" // Standart paylaşım metni
            "Kisisel profil bilgilerini guncelledi" ->
                return "Kişisel profil bilgilerini güncelledi" // Profil güncelleme metni
        }
        var s = aiMutfakPrefix.matcher(desc).replaceFirst("Yapay zeka mutfak: ") // Önek düzeltmesi
        s = secilenWord.matcher(s).replaceAll("seçilen") // Türkçe seçilen
        s = paylasildiWord.matcher(s).replaceAll("paylaşıldı") // Türkçe paylaşıldı
        s = paylasmakWord.matcher(s).replaceAll("paylaşmak") // Türkçe paylaşmak
        s = ogunTam.matcher(s).replaceAll("Öğün tamamlandı:") // Türkçe öğün tamamlandı
        s = ogunFoto.matcher(s).replaceAll("Öğün fotoğrafı yüklendi (yapay zeka analizi)") // Foto yükleme metni
        s = aiAnal.matcher(s).replaceAll("yapay zeka analizi") // AI analizi → yapay zeka analizi
        return s // Dönüştürülmüş metin
    }
}
