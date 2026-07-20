import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { AlertCircle, Loader2, Upload } from "lucide-react";
import { api } from "../../api/http";
import {
  AuthError,
  AuthField,
  AuthLayout,
  AuthPageHeader,
  authBtnPrimaryClass,
  authBtnSecondaryClass,
  authInputClass,
} from "./AuthLayout";

export function RegisterDietitian() {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    if (e.target.files?.[0]) setFile(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!file) {
      setError("Diploma veya sertifika belgesi yüklemeniz zorunludur.");
      return;
    }

    setLoading(true);

    const fd = new FormData();
    fd.append("firstName", formData.firstName.trim());
    fd.append("lastName", formData.lastName.trim());
    fd.append("email", formData.email.trim());
    fd.append("password", formData.password);
    fd.append("diplomaNo", formData.diplomaNo.trim());
    fd.append("clinicName", formData.clinicName.trim());
    fd.append("diploma", file);

    try {
      const response = await api.post("/api/Auth/register-dietitian", fd);
      if (response.status >= 200 && response.status < 300) {
        navigate("/login", {
          state: {
            message:
              "Kaydınız alındı. Yönetici onayından sonra aynı e-posta ve şifreyle giriş yapabilirsiniz.",
          },
        });
      }
    } catch (err) {
      setError(err.response?.data?.message || "Sunucuya ulaşılamadı.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout wide>
      <AuthPageHeader
        title="Diyetisyen kaydı"
        subtitle="Platforma katılmak için bilgilerinizi eksiksiz doldurun"
      />

      <div className="mb-8 flex gap-3 rounded-lg border border-blue-100 bg-blue-50 px-5 py-4 text-sm text-blue-900">
        <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-blue-600" />
        <p>
          Güvenlik gereği diyetisyen hesapları admin tarafından diploma kontrolü yapıldıktan sonra
          aktif edilir.
        </p>
      </div>

      <AuthError message={error} />

      <form className="space-y-6" onSubmit={handleSubmit}>
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
          <AuthField id="diplomaNo" label="Diploma no">
            <input
              id="diplomaNo"
              name="diplomaNo"
              type="text"
              placeholder="Diploma numaranız"
              className={authInputClass}
              value={formData.diplomaNo}
              onChange={handleChange}
              required
            />
          </AuthField>
          <AuthField id="clinicName" label="Klinik / kurum adı">
            <input
              id="clinicName"
              name="clinicName"
              type="text"
              placeholder="Kurum adı"
              className={authInputClass}
              value={formData.clinicName}
              onChange={handleChange}
              required
            />
          </AuthField>
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
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
          <AuthField id="password" label="Şifre">
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="new-password"
              placeholder="En az 6 karakter"
              className={authInputClass}
              value={formData.password}
              onChange={handleChange}
              required
              minLength={6}
            />
          </AuthField>
        </div>

        <AuthField
          id="diploma-upload"
          label="Diploma / sertifika belgesi"
          hint="Zorunlu — PDF veya JPG/PNG. Yönetici onayı sırasında incelenecektir."
        >
          <input
            type="file"
            accept="image/*,.pdf"
            onChange={handleFileChange}
            className="hidden"
            id="diploma-upload-input"
          />
          <label
            htmlFor="diploma-upload-input"
            className="flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-slate-300 bg-white px-4 py-10 shadow-sm transition-colors hover:border-[#2ECC71]/60 hover:bg-emerald-50/30"
          >
            <Upload className="mb-2 h-6 w-6 text-slate-400" />
            <p className="text-sm font-medium text-slate-700">
              {file ? file.name : "Belge seçmek için tıklayın veya sürükleyin"}
            </p>
            <p className="mt-1 text-xs text-slate-400">PDF, JPG — maks. 10 MB</p>
          </label>
        </AuthField>

        <div className="grid gap-3 sm:grid-cols-2">
          <Link to="/login" className="block">
            <button type="button" className={authBtnSecondaryClass}>
              İptal
            </button>
          </Link>
          <button type="submit" disabled={loading} className={authBtnPrimaryClass}>
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Gönderiliyor…
              </>
            ) : (
              "Kayıt ol"
            )}
          </button>
        </div>
      </form>

      <p className="mt-10 border-t border-slate-200 pt-8 text-sm text-slate-600">
        Zaten hesabınız var mı?{" "}
        <Link to="/login" className="font-semibold text-[#2ECC71] hover:underline">
          Giriş yapın
        </Link>
      </p>
    </AuthLayout>
  );
}
