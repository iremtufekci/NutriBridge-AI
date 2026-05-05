package com.example.nightbrate

import java.util.regex.Pattern

/** Eski kayıtlarda ASCII veya İngilizce kalan aktivite açıklamalarını düzgün Türkçe gösterime çevirir. */
object ActivityDescriptionNormalize {
    private val aiMutfakPrefix = Pattern.compile("^AI Mutfak:\\s*", Pattern.CASE_INSENSITIVE)
    private val secilenWord = Pattern.compile("\\bsecilen\\b")
    private val paylasildiWord = Pattern.compile("paylasildi", Pattern.CASE_INSENSITIVE)
    private val paylasmakWord = Pattern.compile("paylasmak", Pattern.CASE_INSENSITIVE)
    private val ogunTam = Pattern.compile("Ogun tamamlandi:")
    private val ogunFoto = Pattern.compile("Ogun fotografi yuklendi \\(AI analizi\\)")
    private val aiAnal = Pattern.compile("\\bAI analizi\\b")

    fun toDisplay(desc: String?): String {
        if (desc.isNullOrEmpty()) return ""
        when (desc) {
            "AI Mutfak: secilen tarifler diyetisyenle paylasildi",
            "AI Mutfak: seçilen tarifler diyetisyenle paylaşıldı",
            "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı" ->
                return "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı"
            "Kisisel profil bilgilerini guncelledi" ->
                return "Kişisel profil bilgilerini güncelledi"
        }
        var s = aiMutfakPrefix.matcher(desc).replaceFirst("Yapay zeka mutfak: ")
        s = secilenWord.matcher(s).replaceAll("seçilen")
        s = paylasildiWord.matcher(s).replaceAll("paylaşıldı")
        s = paylasmakWord.matcher(s).replaceAll("paylaşmak")
        s = ogunTam.matcher(s).replaceAll("Öğün tamamlandı:")
        s = ogunFoto.matcher(s).replaceAll("Öğün fotoğrafı yüklendi (yapay zeka analizi)")
        s = aiAnal.matcher(s).replaceAll("yapay zeka analizi")
        return s
    }
}
