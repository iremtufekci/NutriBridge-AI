/** Danışan ayarları: gizlilik ve hakkında metinleri (Türkçe). */

export const CLIENT_PRIVACY_POLICY_TEXT = `Gizlilik ve veri güvenliği

NutriBridge olarak kişisel verilerinizin gizliliğine saygı duyuyoruz. Bu metin, uygulamayı danışan olarak kullanırken verilerinizin nasıl işlendiğini özetler.

1. Toplanan veriler
Hesap oluştururken ve uygulamayı kullanırken; ad, soyad, boy, kilo, hedef kalori, öğün / su / kilo kayıtları, diyet programı bilgileri ve (varsa) diyetisyen eşleştirme verileri gibi sizin sağladığınız bilgiler sistemde tutulur.

2. Amaç
Verileriniz yalnızca beslenme takibi, diyetisyen-danışan sürecinin yürütülmesi ve hizmetin sunulması amacıyla işlenir.

3. Paylaşım
Verileriniz, yalnızca atandığınız onaylı diyetisyenin sizi takip edebilmesi için gerekli ölçüde görünür; üçüncü taraflara satılmaz ve pazarlama amacıyla paylaşılmaz.

4. Güvenlik
Verileriniz uygulama altyapısında erişim kontrolleriyle korunmaya çalışılır. İnternette hiçbir sistem %100 risk içermez; güçlü bir şifre kullanmanız önerilir.

5. Haklarınız
KVKK kapsamındaki başvuru ve bilgi talepleriniz için uygulama yöneticisi / veri sorumlusu ile iletişime geçebilirsiniz.

Bu metin bilgilendirme amaçlıdır; ayrıntılı sözleşme ve resmi belgeler için lütfen hizmet sağlayıcınıza danışın.`;

export const CLIENT_ABOUT_TEXT = `Hakkında — NutriBridge

NutriBridge, danışanlar ile onaylı diyetisyenler arasında beslenme programı takibini, günlük kayıtları ve diyet günlerini yönetmeyi kolaylaştırmak için tasarlanmıştır.

Özellikler arasında profil yönetimi, tema tercihiniz, diyetisyeninizle güvenli eşleştirme, diyet programı görüntüleme ve ilerlemenizi kaydetme bulunur.

Geri bildirim ve destek talepleri için lütfen hizmet sağlayıcınız veya yönetici kanalları üzerinden iletişime geçin.

Teşekkür ederiz.`;

export function resolveGoalLabelFromCalories(targetCalories: number): string {
  if (targetCalories <= 1800) return "Kilo Ver";
  if (targetCalories >= 2300) return "Kilo Al";
  return "Formu Koru";
}
