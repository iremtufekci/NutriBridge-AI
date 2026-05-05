/** Eski kayıtlarda ASCII veya İngilizce kalan aktivite açıklamalarını düzgün Türkçe gösterime çevirir. */
export function normalizeActivityDescription(desc: string | null | undefined): string {
  if (!desc) return "";
  const exact: Record<string, string> = {
    "AI Mutfak: secilen tarifler diyetisyenle paylasildi":
      "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı",
    "AI Mutfak: seçilen tarifler diyetisyenle paylaşıldı":
      "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı",
    "Kisisel profil bilgilerini guncelledi": "Kişisel profil bilgilerini güncelledi",
  };
  if (exact[desc]) return exact[desc];

  let s = desc;
  s = s.replace(/^AI Mutfak:\s*/i, "Yapay zeka mutfak: ");
  s = s.replace(/\bsecilen\b/g, "seçilen");
  s = s.replace(/\bpaylasildi\b/gi, "paylaşıldı");
  s = s.replace(/\bpaylasmak\b/gi, "paylaşmak");
  s = s.replace(/Ogun tamamlandi:/g, "Öğün tamamlandı:");
  s = s.replace(/Ogun fotografi yuklendi \(AI analizi\)/g, "Öğün fotoğrafı yüklendi (yapay zeka analizi)");
  s = s.replace(/\bAI analizi\b/g, "yapay zeka analizi");
  return s;
}
