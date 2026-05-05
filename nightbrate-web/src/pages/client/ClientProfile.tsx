import { useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { ChevronRight, Info, Lock, Stethoscope, UserRound, X } from "lucide-react";
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
};

const defaultProfile: ClientProfileData = {
  firstName: "Danışan",
  lastName: "",
  weight: 0,
  height: 0,
  targetCalories: 2000,
  goalText: "Formu Koru",
  dietitianName: "Atanmadı",
  programStartDate: new Date().toISOString(),
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
    () => `${profile.firstName} ${profile.lastName}`.trim() || "Danışan",
    [profile.firstName, profile.lastName]
  );

  const loadProfile = useCallback(async () => {
    try {
      const { data } = await api.get("/api/client/profile");
      const tc = typeof data.targetCalories === "number" ? data.targetCalories : 2000;
      setProfile({
        firstName: data.firstName || "",
        lastName: data.lastName || "",
        weight: data.weight || 0,
        height: data.height || 0,
        targetCalories: tc,
        goalText: data.goalText || resolveGoalLabelFromCalories(tc),
        dietitianName: data.dietitianName || "Atanmadı",
        programStartDate: data.programStartDate || new Date().toISOString(),
      });
    } catch (error) {
      console.error("Profil verisi alınamadı", error);
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

  const hasNoDietitian =
    !profile.dietitianName ||
    profile.dietitianName === "Atanmadi" ||
    profile.dietitianName === "Atanmadı";

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
      const msg = error?.response?.data?.message || "Kod geçerli değil veya diyetisyen onaylı değil.";
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

  const applyPresetCalories = (n: number) => {
    setEditDraft((d) => ({ ...d, targetCalories: String(n) }));
  };

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="min-h-screen bg-slate-50 px-4 py-6 sm:px-6 lg:px-8 pb-24 lg:pb-8">
        <div className="mx-auto max-w-5xl space-y-5">
          <div>
            <h1 className="text-3xl sm:text-5xl font-bold text-slate-900">Profil & Ayarlar</h1>
            <p className="text-slate-500">Bilgilerinizi yönetin</p>
          </div>

          <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8">
            <div className="flex flex-col items-center gap-5">
              <div className="w-24 h-24 rounded-full bg-[#2ECC71] text-white flex items-center justify-center text-4xl font-bold">
                {(profile.firstName?.charAt(0) || "D").toUpperCase()}
                {(profile.lastName?.charAt(0) || "").toUpperCase()}
              </div>
              <div className="text-center">
                <h2 className="text-3xl font-bold text-slate-900">{userName}</h2>
                <p className="text-emerald-600 mt-1">Diyetisyen: {profile.dietitianName}</p>
              </div>
            </div>

            <div className="mt-8 grid grid-cols-1 sm:grid-cols-3 gap-4 text-center">
              <div>
                <p className="text-4xl font-bold text-slate-900">{profile.height || "-"} cm</p>
                <p className="text-slate-500">Boyum</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900">{profile.weight || "-"} kg</p>
                <p className="text-slate-500">Kilom</p>
              </div>
              <div>
                <p className="text-4xl font-bold text-slate-900">{profile.goalText}</p>
                <p className="text-slate-500">Hedef</p>
              </div>
            </div>

            <div className="mt-6 border-t border-slate-200 pt-4 text-sm text-slate-500">
              Program başlangıcı:{" "}
              <span className="font-semibold text-slate-700">
                {new Date(profile.programStartDate).toLocaleDateString("tr-TR", {
                  day: "numeric",
                  month: "long",
                  year: "numeric",
                })}
              </span>
            </div>
          </section>

          {hasNoDietitian && (
            <section className="rounded-3xl border border-slate-200 bg-white p-6 sm:p-8 space-y-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                  <Stethoscope className="h-6 w-6 text-emerald-600" />
                  Diyetisyenime bağlan
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                  Diyetisyeninizin 6 haneli takip kodunu girin. Önce kodu doğrulayın, ismi görün, sonra
                  eşleşmeyi onaylayın.
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-2 sm:items-end">
                <div className="flex-1">
                  <label className="text-xs text-slate-500 block mb-1">Takip kodu</label>
                  <input
                    type="text"
                    value={dietCode}
                    onChange={(e) => setDietCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6))}
                    maxLength={6}
                    placeholder="örnek: A1B2C3"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-900 font-mono tracking-widest"
                    disabled={dietConnectBusy}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => void handleVerifyDietCode()}
                  disabled={dietConnectBusy || dietCode.replace(/\s/g, "").length !== 6}
                  className="rounded-xl bg-slate-200 px-5 py-3 font-semibold text-slate-800 disabled:opacity-50"
                >
                  {dietConnectBusy && !dietPreview ? "Kontrol ediliyor…" : "Kodu doğrula"}
                </button>
              </div>
              {dietPreview && (
                <div className="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-4 space-y-3">
                  <p className="text-slate-800">
                    <span className="font-semibold">Bulundu: </span>
                    {dietPreview.displayName}
                  </p>
                  <p className="text-sm text-slate-600">
                    Bu isimle eşleşmek istediğinizden emin misiniz? Onayladığınızda bağlantı veritabanına
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
                      className="rounded-xl border border-slate-300 px-5 py-2.5 font-semibold text-slate-700"
                    >
                      Vazgeçtim
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
              className="w-full rounded-2xl border border-slate-200 bg-white p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900">
                <UserRound className="text-emerald-600" />
                Kişisel bilgilerimi düzenle
              </span>
              <ChevronRight className="text-slate-400 shrink-0" />
            </button>

            <button
              type="button"
              onClick={() => setPanel("privacy")}
              className="w-full rounded-2xl border border-slate-200 bg-white p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900">
                <Lock className="text-emerald-600" />
                Gizlilik politikası
              </span>
              <ChevronRight className="text-slate-400 shrink-0" />
            </button>

            <button
              type="button"
              onClick={() => setPanel("about")}
              className="w-full rounded-2xl border border-slate-200 bg-white p-5 flex items-center justify-between text-left"
            >
              <span className="flex items-center gap-3 text-lg font-semibold text-slate-900">
                <Info className="text-emerald-600" />
                Hakkında
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
                className="bg-white rounded-2xl max-w-md w-full max-h-[90vh] overflow-hidden shadow-xl border border-slate-200 pointer-events-auto"
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
              >
                <div className="p-4 border-b border-slate-200 flex items-center justify-between">
                  <h2 className="font-bold text-lg text-slate-900">Kişisel bilgiler</h2>
                  <button
                    type="button"
                    className="p-2 rounded-lg hover:bg-slate-100"
                    onClick={() => setPanel(null)}
                    aria-label="Kapat"
                  >
                    <X className="w-5 h-5 text-slate-600" />
                  </button>
                </div>
                <div className="p-4 space-y-3 overflow-y-auto max-h-[75vh]">
                  <div>
                    <label className="text-xs text-slate-500" htmlFor="client-edit-fname">
                      Ad
                    </label>
                    <input
                      id="client-edit-fname"
                      name="firstName"
                      autoComplete="given-name"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-900"
                      value={editDraft.firstName}
                      onChange={(e) => setEditDraft((d) => ({ ...d, firstName: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500" htmlFor="client-edit-lname">
                      Soyad
                    </label>
                    <input
                      id="client-edit-lname"
                      name="lastName"
                      autoComplete="family-name"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-900"
                      value={editDraft.lastName}
                      onChange={(e) => setEditDraft((d) => ({ ...d, lastName: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500" htmlFor="client-edit-height">
                      Boy (cm)
                    </label>
                    <input
                      id="client-edit-height"
                      type="text"
                      name="height"
                      inputMode="decimal"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-900"
                      value={editDraft.height}
                      onChange={(e) => setEditDraft((d) => ({ ...d, height: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500" htmlFor="client-edit-weight">
                      Kilo (kg)
                    </label>
                    <input
                      id="client-edit-weight"
                      type="text"
                      name="weight"
                      inputMode="decimal"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-900"
                      value={editDraft.weight}
                      onChange={(e) => setEditDraft((d) => ({ ...d, weight: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs text-slate-500" htmlFor="client-edit-cal">
                      Günlük hedef kalori
                    </label>
                    <input
                      id="client-edit-cal"
                      type="text"
                      name="targetCalories"
                      inputMode="numeric"
                      autoComplete="off"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-900"
                      value={editDraft.targetCalories}
                      onChange={(e) => setEditDraft((d) => ({ ...d, targetCalories: e.target.value }))}
                    />
                    <p className="text-xs text-slate-500 mt-1">
                      Öneri: Hedef, girilen kaloriye göre gösterilir:{" "}
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
                          className="text-xs px-3 py-1.5 rounded-lg bg-slate-100 text-slate-800"
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
                      className="flex-1 py-2.5 rounded-xl border border-slate-200 font-semibold text-slate-800"
                      disabled={saveBusy}
                    >
                      İptal
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
                className="bg-white rounded-2xl max-w-2xl w-full max-h-[88vh] overflow-hidden shadow-xl border border-slate-200"
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
              >
                <div className="p-4 border-b border-slate-200 flex items-center justify-between">
                  <h2 className="font-bold text-lg text-slate-900">
                    {panel === "privacy" ? "Gizlilik politikası" : "Hakkında"}
                  </h2>
                  <button
                    type="button"
                    className="p-2 rounded-lg hover:bg-slate-100"
                    onClick={() => setPanel(null)}
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div className="p-4 overflow-y-auto max-h-[72vh] text-sm text-slate-700 whitespace-pre-line leading-relaxed">
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
