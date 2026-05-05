import { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { AlertTriangle, CheckCircle2, Loader2, User, X } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type CritAlert = {
  id: string;
  clientId: string;
  clientName: string;
  alertType: string;
  severity: string;
  message: string;
  date: string;
  referenceDate: string;
};

type ClientBrief = {
  clientId: string;
  firstName: string;
  lastName: string;
  email: string;
  targetCalories: number;
  weight: number;
  height: number;
  phone?: string;
};

function alertTypeLabel(t: string) {
  switch (t) {
    case "MissedMeals":
      return "Öğün tamamlama";
    case "HighCalories":
      return "Yüksek kalori";
    default:
      return t;
  }
}

export function DietitianCriticalAlerts() {
  const name = useAuthProfileDisplayName();
  const [alerts, setAlerts] = useState<CritAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [profileClientId, setProfileClientId] = useState<string | null>(null);
  const [brief, setBrief] = useState<ClientBrief | null>(null);
  const [briefLoading, setBriefLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<CritAlert[]>("/api/dietitian/critical-alerts");
      setAlerts(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(getApiErrorMessage(e));
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const acknowledge = async (a: CritAlert) => {
    setBusyId(a.id);
    setError(null);
    try {
      await api.post("/api/dietitian/acknowledge-critical-alert", {
        clientId: a.clientId,
        alertType: a.alertType,
        referenceDate: a.referenceDate,
      });
      setAlerts((prev) => prev.filter((x) => x.id !== a.id));
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  };

  const openProfile = async (clientId: string) => {
    setProfileClientId(clientId);
    setBrief(null);
    setBriefLoading(true);
    try {
      const { data } = await api.get<ClientBrief>("/api/dietitian/client-brief", { params: { clientId } });
      setBrief(data);
    } catch {
      setBrief(null);
    } finally {
      setBriefLoading(false);
    }
  };

  const highCount = alerts.filter((x) => x.severity === "High").length;

  return (
    <SidebarLayout userRole="dietitian" userName={name}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-slate-50 min-h-screen text-slate-900 transition-colors pb-28 lg:pb-8">
        <div>
          <h1 className="text-3xl sm:text-4xl font-bold flex items-center gap-2">
            <AlertTriangle className="w-9 h-9 text-rose-500 shrink-0" />
            Kritik Uyarılar
          </h1>
          <p className="text-slate-500 mt-1 max-w-2xl">
            Son 3 günün verilerine göre öğün uyumu ve günlük kalori kuralları değerlendirilir. İncelediğiniz
            kayıt listeden düşer; veri tekrar eşikleri aştığında yeni uyarı oluşabilir.
          </p>
        </div>

        {loading && (
          <p className="text-sm text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" /> Uyarılar yükleniyor…
          </p>
        )}

        {error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </div>
        )}

        {!loading && alerts.length > 0 && (
          <div className="rounded-2xl border border-rose-200/90 bg-rose-50/90 px-4 py-3 flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
            <p className="text-sm text-rose-900">
              <span className="font-semibold">Kritik durumda {alerts.length} kayıt</span>
              {highCount > 0 && (
                <span>
                  {" "}
                  ({highCount} yüksek öncelik). Bu danışanlar kısa sürede değerlendirme gerektirebilir; kaydı onaylayarak
                  arşivleyebilirsiniz.
                </span>
              )}
              {highCount === 0 && " Orta öncelikli uyarıları inceleyip onaylayabilirsiniz."}
            </p>
          </div>
        )}

        {!loading && alerts.length === 0 && !error && (
          <div className="rounded-2xl border border-emerald-200/80 bg-emerald-50/60 px-4 py-6 text-center">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
            <p className="font-semibold text-emerald-800">Şu an listelenecek kritik uyarı yok</p>
            <p className="text-sm text-slate-600 mt-1">Yeni kayıtlar veri geldikçe burada belirir.</p>
          </div>
        )}

        <ul className="space-y-4 max-w-3xl">
          {alerts.map((a) => {
            const isHigh = a.severity === "High";
            return (
              <li
                key={a.id}
                className={[
                  "rounded-2xl border p-4 sm:p-5 shadow-sm bg-white",
                  isHigh
                    ? "border-rose-200 border-l-4 border-l-rose-500"
                    : "border-amber-200/90 border-l-4 border-l-amber-500",
                ].join(" ")}
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={[
                        "w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold shrink-0",
                        isHigh
                          ? "bg-rose-100 text-rose-600"
                          : "bg-amber-100 text-amber-800",
                      ].join(" ")}
                    >
                      {(a.clientName || "?").charAt(0).toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <p className="text-lg font-bold text-slate-900 truncate">{a.clientName}</p>
                      <p className="text-xs text-slate-500">
                        Tarih: {new Date(a.date).toLocaleDateString("tr-TR", { day: "numeric", month: "long", year: "numeric" })}
                      </p>
                    </div>
                  </div>
                  <span
                    className={[
                      "text-xs font-semibold px-2.5 py-1 rounded-full shrink-0",
                      isHigh
                        ? "bg-rose-100 text-rose-700"
                        : "bg-amber-100 text-amber-800",
                    ].join(" ")}
                  >
                    {isHigh ? "Yüksek öncelik" : "Orta öncelik"}
                  </span>
                </div>

                <div className="mt-3 rounded-xl bg-slate-50 px-3 py-2 flex items-center gap-2 text-sm text-slate-800">
                  <span aria-hidden>⚠️</span>
                  <span className="font-medium">{alertTypeLabel(a.alertType)}</span>
                </div>
                <p className="mt-2 text-sm text-slate-600 leading-relaxed">{a.message}</p>

                <div className="mt-4 flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => void acknowledge(a)}
                    disabled={busyId === a.id}
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-semibold px-4 py-2.5 disabled:opacity-50"
                  >
                    {busyId === a.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    İncelendi (diyetisyen onayı)
                  </button>
                  <button
                    type="button"
                    onClick={() => void openProfile(a.clientId)}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border-2 border-slate-200 bg-white text-slate-800 text-sm font-semibold px-4 py-2.5 hover:bg-slate-50"
                  >
                    <User className="w-4 h-4" />
                    Profili gör
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      </div>

      {profileClientId &&
        createPortal(
          <div
            className="fixed inset-0 z-[200] flex items-end sm:items-center justify-center p-3 sm:p-4 bg-black/50 backdrop-blur-sm"
            role="dialog"
            aria-modal
            onClick={() => {
              setProfileClientId(null);
              setBrief(null);
            }}
          >
            <div
              className="w-full max-w-md rounded-2xl border border-slate-200 bg-white shadow-xl p-5 max-h-[90vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-start justify-between gap-2">
                <h2 className="text-lg font-bold text-slate-900">Danışan özeti</h2>
                <button
                  type="button"
                  onClick={() => {
                    setProfileClientId(null);
                    setBrief(null);
                  }}
                  className="p-1 rounded-lg hover:bg-slate-100"
                  aria-label="Kapat"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              {briefLoading && (
                <p className="mt-4 text-sm text-slate-500 flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" /> Yükleniyor…
                </p>
              )}
              {!briefLoading && brief && (
                <dl className="mt-4 space-y-2 text-sm">
                  <div>
                    <dt className="text-slate-500">Ad</dt>
                    <dd className="font-medium text-slate-900">
                      {brief.firstName} {brief.lastName}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">E-posta</dt>
                    <dd className="text-slate-800 break-all">{brief.email || "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Telefon</dt>
                    <dd className="text-slate-800">{brief.phone?.trim() ? brief.phone : "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Hedef kalori</dt>
                    <dd>{brief.targetCalories} kkal</dd>
                  </div>
                  <div className="flex gap-6">
                    <div>
                      <dt className="text-slate-500">Kilo</dt>
                      <dd>{brief.weight} kg</dd>
                    </div>
                    <div>
                      <dt className="text-slate-500">Boy</dt>
                      <dd>{brief.height} cm</dd>
                    </div>
                  </div>
                </dl>
              )}
              {!briefLoading && !brief && (
                <p className="mt-4 text-sm text-red-600">Profil yüklenemedi veya erişim yok.</p>
              )}
            </div>
          </div>,
          document.body
        )}
    </SidebarLayout>
  );
}
