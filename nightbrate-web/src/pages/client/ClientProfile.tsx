import { useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronRight, Info, Lock, Moon, Stethoscope, UserRound, X } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import {
  CLIENT_ABOUT_TEXT,
  CLIENT_PRIVACY_POLICY_TEXT,
  resolveGoalLabelFromCalories,
} from "./clientSettingsCopy";

type ClientProfileData = {
  firstName: string;
  lastName: string;
  weight: number;
  height: number;
  targetCalories: number;
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
  targetCalories: 2000,
  goalText: "Formu Koru",
  dietitianName: "Atanmadi",
  programStartDate: new Date().toISOString(),
  themePreference: "light",
};

type PreviewDiet = { displayName: string; firstName?: string; lastName?: string };

type Panel = null | "edit" | "privacy" | "about";

export function ClientProfile() {
  const [profile, setProfile] = useState<ClientProfileData>(defaultProfile);
  const [panel, setPanel] = useState<Panel>(null);
  const [editDraft, setEditDraft] = useState({
    firstName: "",
    lastName: "",
    height: "",
    weight: "",
    targetCalories: "",
  });
  const [saveBusy, setSaveBusy] = useState(false);

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
      const tc = typeof data.targetCalories === "number" ? data.targetCalories : 2000;
      setProfile({
        firstName: data.firstName || "",
        lastName: data.lastName || "",
        weight: data.weight || 0,
        height: data.height || 0,
        targetCalories: tc,
        goalText: data.goalText || resolveGoalLabelFromCalories(tc),
        dietitianName: data.dietitianName || "Atanmadi",
        programStartDate: data.programStartDate || new Date().toISOString(),
        themePreference: normalizedTheme,
      });

      localStorage.setItem("theme", normalizedTheme);
      document.documentElement.classList.toggle("dark", normalizedTheme === "dark");
      window.dispatchEvent(
        new CustomEvent("nightbrate-theme", { detail: { isDark: normalizedTheme === "dark" } })
      );
    } catch (error) {
      console.error("Profil verisi alinamadi", error);
    }
  }, []);

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

  const openEditPanel = () => {
    setEditDraft({
      firstName: profile.firstName,
      lastName: profile.lastName,
      height: profile.height > 0 ? String(profile.height) : "",
      weight: profile.weight > 0 ? String(profile.weight) : "",
      targetCalories: String(profile.targetCalories || 2000),
    });
    setPanel("edit");
  };

  const saveProfile = async () => {
    const fn = editDraft.firstName.trim();
    const ln = editDraft.lastName.trim();
    if (!fn || !ln) {
      alert("Ad ve soyad zorunludur.");
      return;
    }
    const h = parseFloat(editDraft.height.replace(",", "."));
    const w = parseFloat(editDraft.weight.replace(",", "."));
    const tc = parseInt(editDraft.targetCalories, 10);
    if (Number.isNaN(h) || h < 50 || h > 250) {
      alert("Boy 50–250 cm arasında olmalıdır.");
      return;
    }
    if (Number.isNaN(w) || w < 20 || w > 400) {
      alert("Kilo 20–400 kg arasında olmalıdır.");
      return;
    }
    if (Number.isNaN(tc) || tc < 800 || tc > 6000) {
      alert("Hedef kalori 800–6000 arasında olmalıdır.");
      return;
    }

    const hSame = Math.abs(h - (profile.height || 0)) < 0.01;
    const wSame = Math.abs(w - (profile.weight || 0)) < 0.01;
    const sameAsLoaded =
      fn === (profile.firstName || "").trim() &&
      ln === (profile.lastName || "").trim() &&
      hSame &&
      wSame &&
      tc === (profile.targetCalories || 2000);
    if (sameAsLoaded) {
      alert("Kaydedilecek değişiklik yok.");
      return;
    }
    if (
      !window.confirm(
        "Kişisel bilgilerinizi bu şekilde kaydetmek istediğinize emin misiniz?"
      )
    ) {
      return;
    }

    setSaveBusy(true);
    try {
      await api.post("/api/Client/profile", {
        firstName: fn,
        lastName: ln,
        height: h,
        weight: w,
        targetCalories: tc,
      });
      setPanel(null);
      await loadProfile();
    } catch (error: unknown) {
      alert(getApiErrorMessage(error));
    } finally {
      setSaveBusy(false);
    }
  };

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
      await api.post("/api/Auth/theme", { themePreference: next });
      setProfile((prev) => ({ ...prev, themePreference: next }));
      localStorage.setItem("theme", next);
      document.documentElement.classList.toggle("dark", next === "dark");
      window.dispatchEvent(
        new CustomEvent("nightbrate-theme", { detail: { isDark: next === "dark" } })
      );
    } catch (error) {
      alert("Tema kaydedilemedi: " + ((error as any)?.response?.data?.message || "Hata"));
    }
  };

  const applyPresetCalories = (n: number) => {
    setEditDraft((d) => ({ ...d, targetCalories: String(n) }));
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-screen bg-[#F4F6F8] dark:bg-slate-950 px-4 py-6 sm:px-6 lg:px-8 pb-24 md:pb-8">
        <div className="mx-auto max-w-5xl space-y-5">
          <div>
            <h1 className="text-3xl sm:text-5xl font-bold text-slate-900 dark:text-slate-100">Profil & Ayarlar</h1>
            <p className="text-slate-500 dark:text-[#9CA3AF]">Bilgilerinizi yonetin</p>
          </div>

          <section className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-6 sm:p-8">
            <div className="flex flex-col items-center gap-5">
              <div className="w-24 h-24 rounded-full bg-[#2ECC71] text-white flex items-center justify-center text-4xl font-bold">
                {(profile.firstName?.charAt(0) || "D").toUpperCase()}
                {(profile.lastName?.charAt(0) || "").toUpperCase()}
              </div>
              <div className="text-center">
                <h2 className="text-3xl font-bold text-slate-900 dark:text-slate-100">{userName}</h2>
                <p className="text-emerald-600 dark:text-[#2ECC71] mt-1">Diyetisyen: {profile.dietitianName}</p>
              </div>
            </div>

            <div className="mt-8 grid grid-cols-1 sm:grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.height || "-"} cm</p>
                <p className="text-slate-500 dark:text-[#9CA3AF]">Boyum</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.weight || "-"} kg</p>
                <p className="text-slate-500 dark:text-[#9CA3AF]">Kilom</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900 dark:text-slate-100">{profile.goalText}</p>
                <p className="text-slate-500 dark:text-[#9CA3AF]">Hedef</p>
              </div>
            </div>

            <div className="mt-6 border-t border-slate-200 dark:border-slate-800 pt-4 text-sm text-slate-500 dark:text-[#9CA3AF]">
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
            <section className="rounded-3xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-6 sm:p-8 space-y-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                  <Stethoscope className="h-6 w-6 text-emerald-600 dark:text-[#2ECC71]" />
                  Diyetisyenime baglan
                </h2>
                <p className="text-sm text-slate-500 dark:text-[#9CA3AF] mt-1">
                  Diyetisyeninizin 6 haneli takip kodunu girin. Once kodu dogrulayin, ismi gorun, sonra
                  eşleşmeyi onaylayin.
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-2 sm:items-end">
                <div className="flex-1">
                  <label className="text-xs text-slate-500 dark:text-[#9CA3AF] block mb-1">Takip kodu</label>
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
            <button
              type="button"
              onClick={openEditPanel}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
                <UserRound className="text-emerald-600 dark:text-[#2ECC71]" />
                Kisisel Bilgilerimi Duzenle
              </span>
              <ChevronRight className="text-slate-400 shrink-0" />
            </button>

            <button
              type="button"
              onClick={toggleTheme}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-5 flex items-center justify-between"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
                <Moon className="text-emerald-600 dark:text-[#2ECC71]" />
                Tema Ayarlari
              </span>
              <span className="text-base font-medium text-slate-600 dark:text-slate-300">
                {profile.themePreference === "dark" ? "Dark Mode" : "Light Mode"}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setPanel("privacy")}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
                <Lock className="text-emerald-600 dark:text-[#2ECC71]" />
                Gizlilik Politikasi
              </span>
              <ChevronRight className="text-slate-400 shrink-0" />
            </button>

            <button
              type="button"
              onClick={() => setPanel("about")}
              className="w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#1F2937] p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900 dark:text-slate-100">
                <Info className="text-emerald-600 dark:text-[#2ECC71]" />
                Hakkinda
              </span>
              <ChevronRight className="text-slate-400 shrink-0" />
            </button>
          </section>
        </div>

        {typeof document !== "undefined" &&
          panel === "edit" &&
          createPortal(
            <div
              className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/50 pointer-events-auto"
              role="dialog"
              aria-modal="true"
              onClick={() => setPanel(null)}
            >
              <div
                className="bg-white dark:bg-[#1F2937] rounded-2xl max-w-md w-full max-h-[90vh] overflow-hidden shadow-xl border border-slate-200 dark:border-slate-600 pointer-events-auto"
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
              >
                <div className="p-4 border-b border-slate-200 dark:border-slate-600 flex items-center justify-between">
                  <h2 className="font-bold text-lg text-slate-900 dark:text-slate-100">Kisisel bilgiler</h2>
                  <button
                    type="button"
                    className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
                    onClick={() => setPanel(null)}
                    aria-label="Kapat"
                  >
                    <X className="w-5 h-5 text-slate-600" />
                  </button>
                </div>
                <div className="p-4 space-y-3 overflow-y-auto max-h-[75vh]">
                  <div>
                    <label className="text-xs text-slate-500 dark:text-slate-400" htmlFor="client-edit-fname">
                      Ad
                    </label>
                    <input
                      id="client-edit-fname"
                      name="firstName"
                      autoComplete="given-name"
                      className="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/80 px-3 py-2.5 text-slate-900 dark:text-slate-100"
                      value={editDraft.firstName}
                      onChange={(e) => setEditDraft((d) => ({ ...d, firstName: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500 dark:text-slate-400" htmlFor="client-edit-lname">
                      Soyad
                    </label>
                    <input
                      id="client-edit-lname"
                      name="lastName"
                      autoComplete="family-name"
                      className="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/80 px-3 py-2.5 text-slate-900 dark:text-slate-100"
                      value={editDraft.lastName}
                      onChange={(e) => setEditDraft((d) => ({ ...d, lastName: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500 dark:text-slate-400" htmlFor="client-edit-height">
                      Boy (cm)
                    </label>
                    <input
                      id="client-edit-height"
                      type="text"
                      name="height"
                      inputMode="decimal"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/80 px-3 py-2.5 text-slate-900 dark:text-slate-100"
                      value={editDraft.height}
                      onChange={(e) => setEditDraft((d) => ({ ...d, height: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500 dark:text-slate-400" htmlFor="client-edit-weight">
                      Kilo (kg)
                    </label>
                    <input
                      id="client-edit-weight"
                      type="text"
                      name="weight"
                      inputMode="decimal"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/80 px-3 py-2.5 text-slate-900 dark:text-slate-100"
                      value={editDraft.weight}
                      onChange={(e) => setEditDraft((d) => ({ ...d, weight: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500 dark:text-slate-400" htmlFor="client-edit-cal">
                      Gunluk hedef kalori
                    </label>
                    <input
                      id="client-edit-cal"
                      type="text"
                      name="targetCalories"
                      inputMode="numeric"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 dark:border-slate-600 bg-slate-50 dark:bg-slate-900/80 px-3 py-2.5 text-slate-900 dark:text-slate-100"
                      value={editDraft.targetCalories}
                      onChange={(e) => setEditDraft((d) => ({ ...d, targetCalories: e.target.value }))}
                    />
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      Oneri: Hedef, girilen kaloriye gore gosterilir:{" "}
                      {resolveGoalLabelFromCalories(
                        parseInt(editDraft.targetCalories, 10) || profile.targetCalories
                      )}
                    </p>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {[
                        { n: 1600, t: "Kilo ver" },
                        { n: 2000, t: "Formu koru" },
                        { n: 2500, t: "Kilo al" },
                      ].map((x) => (
                        <button
                          key={x.n}
                          type="button"
                          onClick={() => applyPresetCalories(x.n)}
                          className="text-xs px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200"
                        >
                          {x.t} ({x.n})
                        </button>
                      ))}
                    </div>
                  </div>
                  <div className="flex gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setPanel(null)}
                      className="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 font-semibold text-slate-800 dark:text-slate-200"
                      disabled={saveBusy}
                    >
                      Iptal
                    </button>
                    <button
                      type="button"
                      onClick={() => void saveProfile()}
                      disabled={saveBusy}
                      className="flex-1 py-2.5 rounded-xl bg-[#2ECC71] text-white font-semibold disabled:opacity-50"
                    >
                      {saveBusy ? "Kaydediliyor…" : "Kaydet"}
                    </button>
                  </div>
                </div>
              </div>
            </div>,
            document.body
          )}

        {typeof document !== "undefined" &&
          (panel === "privacy" || panel === "about") &&
          createPortal(
            <div
              className="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-black/50"
              role="dialog"
              aria-modal="true"
              onClick={() => setPanel(null)}
            >
              <div
                className="bg-white dark:bg-[#1F2937] rounded-2xl max-w-2xl w-full max-h-[88vh] overflow-hidden shadow-xl border border-slate-200 dark:border-slate-600"
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
              >
                <div className="p-4 border-b border-slate-200 dark:border-slate-600 flex items-center justify-between">
                  <h2 className="font-bold text-lg text-slate-900 dark:text-slate-100">
                    {panel === "privacy" ? "Gizlilik politikasi" : "Hakkinda"}
                  </h2>
                  <button
                    type="button"
                    className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
                    onClick={() => setPanel(null)}
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="p-4 overflow-y-auto max-h-[72vh] text-sm text-slate-700 dark:text-slate-300 whitespace-pre-line leading-relaxed">
                  {panel === "privacy" ? CLIENT_PRIVACY_POLICY_TEXT : CLIENT_ABOUT_TEXT}
                </div>
              </div>
            </div>,
            document.body
          )}
      </div>
    </SidebarLayout>
  );
}
