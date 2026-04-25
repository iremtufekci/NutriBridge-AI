import { useCallback, useEffect, useMemo, useState } from "react";
import { ChevronRight, Info, Lock, Moon, Stethoscope, UserRound } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";

type ClientProfileData = {
  firstName: string;
  lastName: string;
  weight: number;
  height: number;
  goalText: string;
  dietitianName: string;
  programStartDate: string;
  themePreference: "light" | "dark";
};

const defaultProfile: ClientProfileData = {
  firstName: "Danisan",
  lastName: "",
  weight: 0,
  height: 0,
  goalText: "Formu Koru",
  dietitianName: "Atanmadi",
  programStartDate: new Date().toISOString(),
  themePreference: "light",
};

type PreviewDiet = { displayName: string; firstName?: string; lastName?: string };

export function ClientProfile() {
  const [profile, setProfile] = useState<ClientProfileData>(defaultProfile);
  const [infoText, setInfoText] = useState("");
  const [dietCode, setDietCode] = useState("");
  const [verifiedCode, setVerifiedCode] = useState<string | null>(null);
  const [dietPreview, setDietPreview] = useState<PreviewDiet | null>(null);
  const [dietConnectBusy, setDietConnectBusy] = useState(false);

  const userName = useMemo(
    () => `${profile.firstName} ${profile.lastName}`.trim() || "Danisan",
    [profile.firstName, profile.lastName]
  );

  const loadProfile = useCallback(async () => {
    try {
      const { data } = await api.get("/api/client/profile");
      const normalizedTheme = data.themePreference === "dark" ? "dark" : "light";
      setProfile({
        firstName: data.firstName || "",
        lastName: data.lastName || "",
        weight: data.weight || 0,
        height: data.height || 0,
        goalText: data.goalText || "Formu Koru",
        dietitianName: data.dietitianName || "Atanmadi",
        programStartDate: data.programStartDate || new Date().toISOString(),
        themePreference: normalizedTheme,
      });

      localStorage.setItem("theme", normalizedTheme);
      document.documentElement.classList.toggle("dark", normalizedTheme === "dark");
    } catch (error) {
      console.error("Profil verisi alinamadi", error);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  const hasNoDietitian = !profile.dietitianName || profile.dietitianName === "Atanmadi";

  const handleVerifyDietCode = async () => {
    const code = dietCode.replace(/\s/g, "").toUpperCase();
    if (code.length !== 6) {
      alert("Lütfen 6 haneli bir kod girin (büyük harf ve rakam).");
      return;
    }
    setDietConnectBusy(true);
    setDietPreview(null);
    setVerifiedCode(null);
    try {
      const { data } = await api.post<PreviewDiet>("/api/Client/preview-dietitian-by-code", {
        connectionCode: code,
      });
      const displayName =
        (data as PreviewDiet).displayName ||
        `Dr. ${(data as PreviewDiet).firstName} ${(data as PreviewDiet).lastName}`.trim();
      setDietPreview({ displayName, firstName: data.firstName, lastName: data.lastName });
      setVerifiedCode(code);
    } catch (error: any) {
      const msg = error?.response?.data?.message || "Kod gecerli degil veya diyetisyen onayli degil.";
      alert(msg);
    } finally {
      setDietConnectBusy(false);
    }
  };

  const handleCancelDietConnect = () => {
    setDietPreview(null);
    setVerifiedCode(null);
  };

  const handleConfirmDietConnect = async () => {
    if (!verifiedCode) return;
    setDietConnectBusy(true);
    try {
      const { data } = await api.post("/api/Client/connect-to-dietitian", {
        connectionCode: verifiedCode,
      });
      alert(data?.message || "Eşleştirme tamamlandı.");
      setDietCode("");
      setDietPreview(null);
      setVerifiedCode(null);
      await loadProfile();
    } catch (error: any) {
      const msg = error?.response?.data?.message || "Eşleştirilemedi.";
      alert(msg);
    } finally {
      setDietConnectBusy(false);
    }
  };

  const toggleTheme = async () => {
    const next = profile.themePreference === "dark" ? "light" : "dark";
    try {
      await api.post("/api/client/theme", { themePreference: next });
      setProfile((prev) => ({ ...prev, themePreference: next }));
      localStorage.setItem("theme", next);
      document.documentElement.classList.toggle("dark", next === "dark");
    } catch (error) {
      alert("Tema kaydedilemedi: " + ((error as any)?.response?.data?.message || "Hata"));
    }
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-screen bg-[#F4F6F8] dark:bg-slate-950 px-4 py-6 sm:px-6 lg:px-8 pb-24 md:pb-8">
        <div className="mx-auto max-w-5xl space-y-5">
          <div>
            <h1 className="text-3xl sm:text-5xl font-bold text-slate-900 dark:text-slate-100">Profil & Ayarlar</h1>
            <p className="text-slate-500 dark:text-slate-400">Bilgilerinizi yonetin</p>
          </div>

          <section className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 sm:p-8">
            <div className="flex flex-col items-center gap-5">
              <div className="w-24 h-24 rounded-full bg-emerald-500 text-white flex items-center justify-center text-4xl font-bold">
                {(profile.firstName?.charAt(0) || "D").toUpperCase()}
                {(profile.lastName?.charAt(0) || "").toUpperCase()}
              </div>
              <div className="text-center">
                <h2 className="text-3xl font-bold text-slate-900 dark:text-slate-100">{userName}</h2>
                <p className="text-emerald-600 dark:text-emerald-400 mt-1">Diyetisyen: {profile.dietitianName}</p>
              </div>
            </div>

            <div className="mt-8 grid grid-cols-1 sm:grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.height || "-"} cm</p>
                <p className="text-slate-500 dark:text-slate-400">Boyum</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.weight || "-"} kg</p>
                <p className="text-slate-500 dark:text-slate-400">Kilom</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.goalText}</p>
                <p className="text-slate-500 dark:text-slate-400">Hedef</p>
              </div>
            </div>

            <div className="mt-6 border-t border-slate-200 dark:border-slate-800 pt-4 text-sm text-slate-500 dark:text-slate-400">
              Program Baslangici:{" "}
              <span className="font-semibold text-slate-700 dark:text-slate-200">
                {new Date(profile.programStartDate).toLocaleDateString("tr-TR", {
                  day: "numeric",
                  month: "long",
                  year: "numeric",
                })}
              </span>
            </div>
          </section>

          {hasNoDietitian && (
            <section className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-6 sm:p-8 space-y-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                  <Stethoscope className="h-6 w-6 text-emerald-500" />
                  Diyetisyenime baglan
                </h2>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                  Diyetisyeninizin 6 haneli takip kodunu girin. Once kodu dogrulayin, ismi gorun, sonra
                  eşleşmeyi onaylayin.
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-2 sm:items-end">
                <div className="flex-1">
                  <label className="text-xs text-slate-500 dark:text-slate-400 block mb-1">Takip kodu</label>
                  <input
                    type="text"
                    value={dietCode}
                    onChange={(e) => setDietCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6))}
                    maxLength={6}
                    placeholder="ornek: A1B2C3"
                    className="w-full rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/80 px-4 py-3 text-slate-900 dark:text-slate-100 font-mono tracking-widest"
                    disabled={dietConnectBusy}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => void handleVerifyDietCode()}
                  disabled={dietConnectBusy || dietCode.replace(/\s/g, "").length !== 6}
                  className="rounded-xl bg-slate-200 dark:bg-slate-700 px-5 py-3 font-semibold text-slate-800 dark:text-slate-100 disabled:opacity-50"
                >
                  {dietConnectBusy && !dietPreview ? "Kontrol ediliyor…" : "Kodu dogrula"}
                </button>
              </div>
              {dietPreview && (
                <div className="rounded-2xl border border-emerald-200 dark:border-emerald-800 bg-emerald-50/80 dark:bg-emerald-900/20 p-4 space-y-3">
                  <p className="text-slate-800 dark:text-slate-100">
                    <span className="font-semibold">Bulundu: </span>
                    {dietPreview.displayName}
                  </p>
                  <p className="text-sm text-slate-600 dark:text-slate-300">
                    Bu isimle eslesmek istediginizden emin misiniz? Onayladiginizda baglanti veritabanina
                    kaydedilir.
                  </p>
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => void handleConfirmDietConnect()}
                      disabled={dietConnectBusy}
                      className="rounded-xl bg-emerald-600 px-5 py-2.5 font-semibold text-white disabled:opacity-50"
                    >
                      {dietConnectBusy ? "Kaydediliyor…" : "Evet, eşleştir"}
                    </button>
                    <button
                      type="button"
                      onClick={handleCancelDietConnect}
                      disabled={dietConnectBusy}
                      className="rounded-xl border border-slate-300 dark:border-slate-600 px-5 py-2.5 font-semibold text-slate-700 dark:text-slate-200"
                    >
                      Vazgectim
                    </button>
                  </div>
                </div>
              )}
            </section>
          )}

          <section className="space-y-3">
            <button className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 flex items-center justify-between">
              <span className="flex items-center gap-3 text-lg font-semibold"><UserRound className="text-emerald-500" />Kisisel Bilgilerimi Duzenle</span>
              <ChevronRight className="text-slate-400" />
            </button>

            <button
              onClick={toggleTheme}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 flex items-center justify-between"
            >
              <span className="flex items-center gap-3 text-lg font-semibold"><Moon className="text-emerald-500" />Tema Ayarlari</span>
              <span className="text-base font-medium text-slate-600 dark:text-slate-300">
                {profile.themePreference === "dark" ? "Dark Mode" : "Light Mode"}
              </span>
            </button>

            <button
              onClick={() => setInfoText("Gizlilik Politikasi: Verileriniz sadece saglik takibi amaciyla guvenli sekilde saklanir.")}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 flex items-center justify-between"
            >
              <span className="flex items-center gap-3 text-lg font-semibold"><Lock className="text-emerald-500" />Gizlilik Politikasi</span>
              <ChevronRight className="text-slate-400" />
            </button>

            <button
              onClick={() => setInfoText("Hakkinda: NutriBridge, danisan ve diyetisyen surecini kolaylastirmak icin gelistirilmistir.")}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-5 flex items-center justify-between"
            >
              <span className="flex items-center gap-3 text-lg font-semibold"><Info className="text-emerald-500" />Hakkinda</span>
              <ChevronRight className="text-slate-400" />
            </button>
          </section>

          {infoText && (
            <div className="rounded-2xl border border-emerald-200 dark:border-emerald-900 bg-emerald-50 dark:bg-emerald-900/20 p-4 text-emerald-800 dark:text-emerald-200">
              {infoText}
            </div>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
