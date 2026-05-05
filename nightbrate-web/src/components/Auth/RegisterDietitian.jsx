import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Upload, AlertCircle } from "lucide-react";
import { api } from "../../api/http";

export function RegisterDietitian() {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  
  // Form Verileri
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    diplomaNo: "",
    clinicName: "",
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Backend'e gönderilecek paket
    const payload = {
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      password: formData.password,
      diplomaNo: formData.diplomaNo,
      clinicName: formData.clinicName
    };

    try {
      // CMD'de gördüğümüz 5231 portuna ve yeni oluşturduğumuz diyetisyen endpointine istek atıyoruz
      const response = await api.post("/api/Auth/register-dietitian", payload);
      if (response.status >= 200 && response.status < 300) {
        alert(
          "Kaydınız alındı. Yönetici onayından sonra aynı e-posta ve şifreyle giriş yapabilirsiniz."
        );
        navigate("/");
      }
    } catch (error) {
      console.error("Bağlantı Hatası:", error);
      alert("Kayıt işlemi başarısız: " + (error.response?.data?.message || "Sunucuya ulaşılamadı."));
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <style dangerouslySetInnerHTML={{ __html: `
        .input-style {
          width: 100%;
          padding: 0.75rem;
          background-color: #ffffff;
          border: 1px solid #e2e8f0;
          border-radius: 0.75rem;
          color: #0f172a;
          outline: none;
          transition: all 0.2s;
          margin-bottom: 1rem;
        }
        .input-style:focus { border-color: #22C55E; }
        .label-style { display: block; color: #64748B; font-size: 0.875rem; margin-bottom: 0.5rem; margin-left: 0.25rem; }
      `}} />

      <div className="w-full max-w-2xl bg-white rounded-2xl p-8 border border-slate-200 shadow-2xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#22C55E] mb-2">Diyetisyen Kaydı</h1>
          <p className="text-slate-600">Platforma katılmak için bilgilerinizi eksiksiz doldurun</p>
        </div>

        <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4 mb-6 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-blue-400 flex-shrink-0 mt-0.5" />
          <p className="text-sm text-blue-200">
            Güvenlik gereği, diyetisyen hesapları admin tarafından diploma kontrolü yapıldıktan sonra aktif edilir.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="label-style">Ad</label>
              <input 
                name="firstName" 
                type="text" 
                className="input-style" 
                placeholder="Adınız"
                value={formData.firstName} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div>
              <label className="label-style">Soyad</label>
              <input 
                name="lastName" 
                type="text" 
                className="input-style" 
                placeholder="Soyadınız"
                value={formData.lastName} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div>
              <label className="label-style">Diploma No</label>
              <input 
                name="diplomaNo" 
                type="text" 
                className="input-style" 
                placeholder="Diploma numaranız"
                value={formData.diplomaNo} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div>
              <label className="label-style">Klinik Adı</label>
              <input 
                name="clinicName" 
                type="text" 
                className="input-style" 
                placeholder="Kurum adı"
                value={formData.clinicName} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div className="md:col-span-2">
              <label className="label-style">E-posta</label>
              <input 
                name="email" 
                type="email" 
                className="input-style" 
                placeholder="örnek@ornek.com"
                value={formData.email} 
                onChange={handleChange} 
                required 
              />
            </div>
            <div className="md:col-span-2">
              <label className="label-style">Şifre</label>
              <input 
                name="password" 
                type="password" 
                className="input-style" 
                placeholder="••••••••"
                value={formData.password} 
                onChange={handleChange} 
                required 
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="label-style">Diploma/Sertifika Belgesi (PDF, JPG)</label>
            <div className="relative">
              <input 
                type="file" 
                accept="image/*,.pdf" 
                onChange={handleFileChange} 
                className="hidden" 
                id="diploma-upload" 
                required 
              />
              <label
                htmlFor="diploma-upload"
                className="flex flex-col items-center justify-center w-full h-28 border-2 border-dashed border-slate-600 rounded-lg cursor-pointer hover:border-[#22C55E] hover:bg-slate-800 transition-all"
              >
                <Upload className="w-6 h-6 text-slate-400 mb-2" />
                <p className="text-sm text-slate-300">
                  {file ? file.name : "Belge seçmek için tıklayın"}
                </p>
              </label>
            </div>
          </div>

          <div className="flex gap-4 pt-4">
            <button type="submit" className="flex-1 py-3 bg-[#22C55E] text-white font-bold rounded-xl hover:bg-[#16A34A] transition-all transform hover:scale-[1.02]">
              Kayıt Ol
            </button>
            <Link to="/" className="flex-1">
              <button type="button" className="w-full py-3 border border-slate-600 text-white font-bold rounded-xl hover:bg-slate-800 transition-all">
                İptal
              </button>
            </Link>
          </div>
        </form>

        <div className="mt-6 text-center">
          <p className="text-sm text-slate-400">
            Zaten hesabınız var mı?{" "}
            <Link to="/" className="text-[#22C55E] font-bold hover:underline">
              Giriş Yapın
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}