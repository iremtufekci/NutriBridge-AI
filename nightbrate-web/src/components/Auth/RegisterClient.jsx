import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Eye, EyeOff, ArrowRight } from "lucide-react";

export function RegisterClient() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
    height: "",
    weight: "",
    goal: "",
    activityLevel: "",
    birthDate: ""
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleNextStep = (e) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      alert("Şifreler eşleşmiyor!");
      return;
    }
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      ...formData,
      height: parseInt(formData.height),
      weight: parseInt(formData.weight),
      birthDate: new Date(formData.birthDate).toISOString()
    };

    try {
     const response = await fetch("http://localhost:5231/api/Auth/register-client", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      const result = await response.json();
      if (response.ok) {
        alert("Kayıt Başarılı! " + result.message);
        navigate("/");
      } else {
        alert("Hata: " + result.message);
      }
    } catch (error) {
      console.error("Bağlantı Hatası:", error);
      alert("Backend sunucusuna bağlanılamadı!");
    }
  };

  return (
    <div className="min-h-screen bg-[#0F172A] flex items-center justify-center p-4">
      {/* Stil etiketini doğrudan buraya ekledik */}
      <style dangerouslySetInnerHTML={{ __html: `
        .input-style {
          width: 100%;
          padding: 0.75rem;
          background-color: #0F172A;
          border: 1px solid #334155;
          border-radius: 0.75rem;
          color: white;
          outline: none;
          transition: all 0.2s;
          margin-bottom: 1rem;
        }
        .input-style:focus {
          border-color: #22C55E;
        }
        .btn-primary {
          width: 100%;
          padding: 0.75rem;
          background-color: #22C55E;
          color: white;
          font-weight: bold;
          border-radius: 0.75rem;
          border: none;
          cursor: pointer;
          transition: all 0.2s;
        }
        .btn-primary:hover {
          background-color: #16A34A;
          transform: scale(1.02);
        }
      `}} />

      <div className="w-full max-w-md bg-[#1E293B] rounded-2xl p-8 border border-slate-700 shadow-2xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#22C55E] mb-2">NutriBridge AI</h1>
          <div className="flex justify-center gap-2 mt-4">
            <div className={`w-8 h-1 rounded-full ${step >= 1 ? 'bg-[#22C55E]' : 'bg-slate-600'}`} />
            <div className={`w-8 h-1 rounded-full ${step >= 2 ? 'bg-[#22C55E]' : 'bg-slate-600'}`} />
          </div>
          <p className="text-slate-400 mt-2 text-sm">{step === 1 ? "Kişisel Bilgiler" : "Vücut & Hedef Bilgileri"}</p>
        </div>

        {step === 1 ? (
          <form onSubmit={handleNextStep}>
            <div className="flex gap-4">
              <input name="firstName" placeholder="Ad" className="input-style" onChange={handleChange} required />
              <input name="lastName" placeholder="Soyad" className="input-style" onChange={handleChange} required />
            </div>
            <input name="email" type="email" placeholder="E-posta" className="input-style" onChange={handleChange} required />
            <div className="relative">
              <input name="password" type={showPassword ? "text" : "password"} placeholder="Şifre" className="input-style" onChange={handleChange} required />
              <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-3 text-slate-500 bg-transparent border-none cursor-pointer">
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            <input name="confirmPassword" type="password" placeholder="Şifre Tekrar" className="input-style" onChange={handleChange} required />
            <button type="submit" className="btn-primary" style={{display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px'}}>
              Sonraki Adım <ArrowRight size={18} />
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="flex gap-4">
              <input name="height" type="number" placeholder="Boy (cm)" className="input-style" onChange={handleChange} required />
              <input name="weight" type="number" placeholder="Kilo (kg)" className="input-style" onChange={handleChange} required />
            </div>
            <select name="goal" className="input-style" onChange={handleChange} required>
              <option value="">Hedef Seçiniz</option>
              <option value="lose">Kilo Vermek</option>
              <option value="gain">Kilo Almak</option>
              <option value="maintain">Formu Korumak</option>
            </select>
            <select name="activityLevel" className="input-style" onChange={handleChange} required>
              <option value="">Aktivite Seviyesi</option>
              <option value="sedentary">Hareketsiz</option>
              <option value="moderate">Orta Hareketli</option>
              <option value="active">Çok Aktif</option>
            </select>
            <input name="birthDate" type="date" className="input-style" onChange={handleChange} required />
            <div className="flex gap-4">
              <button type="button" onClick={() => setStep(1)} className="btn-primary" style={{backgroundColor: '#334155', flex: 1}}>Geri</button>
              <button type="submit" className="btn-primary" style={{flex: 2}}>Kaydı Tamamla</button>
            </div>
          </form>
        )}

        <p className="text-center text-slate-400 mt-6 text-sm">
          Zaten hesabınız var mı? <Link to="/" className="text-[#22C55E] font-bold hover:underline">Giriş Yap</Link>
        </p>
      </div>
    </div>
  );
}