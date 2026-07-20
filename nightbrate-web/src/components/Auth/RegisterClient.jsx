import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft, ArrowRight, Eye, EyeOff, Loader2 } from "lucide-react";
import { api } from "../../api/http";
import {
  AuthError,
  AuthField,
  AuthLayout,
  AuthPageHeader,
  authBtnPrimaryClass,
  authBtnSecondaryClass,
  authInputClass,
  authSelectClass,
} from "./AuthLayout";

export function RegisterClient() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
    birthDate: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNextStep = (e) => {
    e.preventDefault();
    setError("");
    if (formData.password !== formData.confirmPassword) {
      setError("Şifreler eşleşmiyor.");
      return;
    }
    if (formData.password.length < 6) {
      setError("Şifre en az 6 karakter olmalıdır.");
      return;
    }
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const payload = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      email: formData.email.trim(),
      password: formData.password,
      height: parseFloat(formData.height),
      weight: parseFloat(formData.weight),
      targetCalories:
        formData.goal === "gain" ? 2400 : formData.goal === "maintain" ? 2100 : 1800,
    };

    try {
      const response = await api.post("/api/Auth/register-client", payload);
      if (response.status === 200 || response.status === 201) {
        navigate("/login", {
          state: { message: response.data.message || "Kayıt başarılı. Giriş yapabilirsiniz." },
        });
      }
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || "Sunucu hatası oluştu.");
      } else if (err.request) {
        setError("Sunucuya ulaşılamıyor. Bağlantınızı ve sunucunun çalıştığını kontrol edin.");
      } else {
        setError("Beklenmedik bir hata oluştu.");
      }
    } finally {
      setLoading(false);
    }
  };

  const steps = ["Hesap bilgileri", "Vücut ve hedef"];

  return (
    <AuthLayout wide>
      <AuthPageHeader
        title="Danışan kaydı"
        subtitle={
          step === 1
            ? "Adım 1 / 2 — Hesap bilgilerinizi oluşturun"
            : "Adım 2 / 2 — Hedeflerinizi belirleyin"
        }
      />

      <div className="mb-8 grid grid-cols-2 gap-4">
        {steps.map((label, i) => {
          const n = i + 1;
          const active = step >= n;
          const current = step === n;
          return (
            <div
              key={label}
              className={`rounded-lg border px-4 py-3 ${
                current
                  ? "border-[#2ECC71] bg-emerald-50/60"
                  : active
                    ? "border-emerald-200 bg-white"
                    : "border-slate-200 bg-white"
              }`}
            >
              <p className="text-xs font-medium uppercase tracking-wide text-slate-400">Adım {n}</p>
              <p className={`mt-1 text-sm font-medium ${active ? "text-slate-800" : "text-slate-400"}`}>
                {label}
              </p>
            </div>
          );
        })}
      </div>

      <AuthError message={error} />

      {step === 1 ? (
        <form className="space-y-5" onSubmit={handleNextStep}>
          <div className="grid gap-5 sm:grid-cols-2">
            <AuthField id="firstName" label="Ad">
              <input
                id="firstName"
                name="firstName"
                type="text"
                autoComplete="given-name"
                placeholder="Adınız"
                className={authInputClass}
                value={formData.firstName}
                onChange={handleChange}
                required
              />
            </AuthField>
            <AuthField id="lastName" label="Soyad">
              <input
                id="lastName"
                name="lastName"
                type="text"
                autoComplete="family-name"
                placeholder="Soyadınız"
                className={authInputClass}
                value={formData.lastName}
                onChange={handleChange}
                required
              />
            </AuthField>
          </div>

          <AuthField id="email" label="E-posta adresi">
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="ornek@nutribridge.ai"
              className={authInputClass}
              value={formData.email}
              onChange={handleChange}
              required
            />
          </AuthField>

          <div className="grid gap-5 sm:grid-cols-2">
            <AuthField id="password" label="Şifre">
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  placeholder="En az 6 karakter"
                  className={`${authInputClass} pr-11`}
                  value={formData.password}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"}
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 border-0 bg-transparent p-1 text-slate-400 hover:text-slate-600"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </AuthField>
            <AuthField id="confirmPassword" label="Şifre tekrar">
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                placeholder="Tekrar girin"
                className={authInputClass}
                value={formData.confirmPassword}
                onChange={handleChange}
                required
              />
            </AuthField>
          </div>

          <button type="submit" className={`${authBtnPrimaryClass} gap-2`}>
            Sonraki adım
            <ArrowRight size={18} />
          </button>
        </form>
      ) : (
        <form className="space-y-5" onSubmit={handleSubmit}>
          <div className="grid gap-5 sm:grid-cols-2">
            <AuthField id="height" label="Boy (cm)">
              <input
                id="height"
                name="height"
                type="number"
                min={50}
                max={250}
                placeholder="170"
                className={authInputClass}
                value={formData.height}
                onChange={handleChange}
                required
              />
            </AuthField>
            <AuthField id="weight" label="Kilo (kg)">
              <input
                id="weight"
                name="weight"
                type="number"
                min={20}
                max={400}
                placeholder="70"
                className={authInputClass}
                value={formData.weight}
                onChange={handleChange}
                required
              />
            </AuthField>
            <AuthField id="goal" label="Hedef">
              <select
                id="goal"
                name="goal"
                className={authSelectClass}
                value={formData.goal}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz</option>
                <option value="lose">Kilo vermek</option>
                <option value="maintain">Formu korumak</option>
                <option value="gain">Kilo almak</option>
              </select>
            </AuthField>
            <AuthField id="activityLevel" label="Aktivite seviyesi">
              <select
                id="activityLevel"
                name="activityLevel"
                className={authSelectClass}
                value={formData.activityLevel}
                onChange={handleChange}
                required
              >
                <option value="">Seçiniz</option>
                <option value="sedentary">Hareketsiz</option>
                <option value="moderate">Orta hareketli</option>
                <option value="active">Çok aktif</option>
              </select>
            </AuthField>
          </div>

          <AuthField id="birthDate" label="Doğum tarihi">
            <input
              id="birthDate"
              name="birthDate"
              type="date"
              className={authInputClass}
              value={formData.birthDate}
              onChange={handleChange}
              required
            />
          </AuthField>

          <div className="grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              onClick={() => {
                setError("");
                setStep(1);
              }}
              className={`${authBtnSecondaryClass} gap-2`}
            >
              <ArrowLeft size={18} />
              Geri
            </button>
            <button type="submit" disabled={loading} className={authBtnPrimaryClass}>
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Kaydediliyor…
                </>
              ) : (
                "Kaydı tamamla"
              )}
            </button>
          </div>
        </form>
      )}

      <p className="mt-10 border-t border-slate-200 pt-8 text-sm text-slate-600">
        Zaten hesabınız var mı?{" "}
        <Link to="/login" className="font-semibold text-[#2ECC71] hover:underline">
          Giriş yapın
        </Link>
      </p>
    </AuthLayout>
  );
}
