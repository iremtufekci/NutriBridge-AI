// Diyetisyen başvurularını tablo ve detay modalı ile inceleme/onaylama sayfası
import { useEffect, useMemo, useState } from "react";
import { Check, Download, Eye, X } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { AdminPageShell, adminCardClass } from "../../components/admin/AdminPageShell";
import { api, API_BASE_URL } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { useAppFeedback } from "../../components/feedback/AppFeedback";

// Diploma URL'sini tam adres olarak çözümler (göreli veya mutlak)
function resolveDocumentHref(path?: string | null): string {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const base = API_BASE_URL.replace(/\/$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  if (!base) return p;
  return `${base}${p}`;
}

// Belge PDF mi; önizleme iframe veya img seçimi için
function isPdfUrl(url: string): boolean {
  return /\.pdf($|\?|#)/i.test(url);
}

type PendingDietitian = {
  id?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  diplomaNo?: string;
  clinicName?: string;
  createdAt?: string;
  isApproved?: boolean;
};

export function AdminApprovals() {
  const adminName = useAuthProfileDisplayName();
  const { notify, confirm } = useAppFeedback();
  const [pending, setPending] = useState<PendingDietitian[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selected, setSelected] = useState<any | null>(null);

  const pendingCount = useMemo(() => pending.length, [pending]);

  // Onay bekleyen diyetisyen listesini yeniler
  const loadPending = async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/api/admin/pending-dietitians");
      setPending(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Bekleyen diyetisyenler alınamadı", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPending();
  }, []);

  // Tablodan tek tıkla onaylama
  const approveDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    const ok = await confirm({
      title: "Diyetisyen onayı",
      message: "Bu diyetisyen başvurusunu onaylamak istiyor musunuz?",
      confirmLabel: "Onayla",
      variant: "success",
    });
    if (!ok) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${dietitianId}`);
      await loadPending();
      notify.success("Diyetisyen onaylandı.");
    } catch (error) {
      notify.error("Onaylama başarısız: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  // Detay modalı için tam başvuru bilgisini getirir
  const inspectDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    setDetailLoading(true);
    try {
      const { data } = await api.get(`/api/admin/dietitian/${dietitianId}`);
      setSelected(data);
    } catch (error) {
      notify.error("Detaylar alınamadı: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    } finally {
      setDetailLoading(false);
    }
  };

  // Modal içinden onaylama
  const approveFromModal = async () => {
    const id = selected?.id;
    if (!id) return;
    const ok = await confirm({
      title: "Diyetisyen onayı",
      message: "Bu diyetisyen başvurusunu onaylamak istiyor musunuz?",
      confirmLabel: "Onayla",
      variant: "success",
    });
    if (!ok) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${id}`);
      setSelected(null);
      await loadPending();
      notify.success("Diyetisyen onaylandı.");
    } catch (error) {
      notify.error("Onaylama başarısız: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <AdminPageShell
        title="Diyetisyen onayları"
        subtitle="Bekleyen kayıtları inceleyin ve onaylayın"
        badge={
          <span className="inline-flex rounded-full bg-amber-100 px-4 py-2 text-sm font-semibold text-amber-800">
            {pendingCount} onay bekliyor
          </span>
        }
      >
        {/* Bekleyen başvurular tablosu */}
        <div className={`${adminCardClass} overflow-hidden`}>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead className="bg-slate-50/90">
                <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3.5">Ad Soyad</th>
                  <th className="px-4 py-3.5">E-posta</th>
                  <th className="px-4 py-3.5">Diploma No</th>
                  <th className="px-4 py-3.5">Klinik</th>
                  <th className="px-4 py-3.5">Kayıt Tarihi</th>
                  <th className="px-4 py-3.5">Durum</th>
                  <th className="px-4 py-3.5">İşlemler</th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan={7} className="p-6 text-center text-slate-500">
                      Yukleniyor...
                    </td>
                  </tr>
                )}

                {!loading && pending.length === 0 && (
                  <tr>
                    <td colSpan={7} className="p-6 text-center text-slate-500">
                      Onay bekleyen diyetisyen bulunmuyor.
                    </td>
                  </tr>
                )}

                {!loading &&
                  pending.map((item) => (
                    <tr key={item.id} className="border-t border-slate-100 transition-colors hover:bg-slate-50/60">
                      <td className="p-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                            {(item.firstName || "D").charAt(0)}
                          </div>
                          <span className="font-semibold">
                            Dr. {item.firstName} {item.lastName}
                          </span>
                        </div>
                      </td>
                      <td className="p-4 text-slate-600 text-sm break-all">
                        {item.email || "—"}
                      </td>
                      <td className="p-4 text-slate-600">{item.diplomaNo || "-"}</td>
                      <td className="p-4 text-slate-600">{item.clinicName || "-"}</td>
                      <td className="p-4 text-slate-600">
                        {item.createdAt
                          ? new Date(item.createdAt).toLocaleDateString("tr-TR")
                          : "-"}
                      </td>
                      <td className="p-4">
                        <span className="inline-flex rounded-full px-3 py-1 text-xs font-semibold bg-amber-100 text-amber-700">
                          Beklemede
                        </span>
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => inspectDietitian(item.id)}
                            className="p-2 rounded-full border border-slate-200 hover:bg-slate-100"
                            title="İncele"
                          >
                            <Eye size={16} />
                          </button>
                          <button
                            onClick={() => approveDietitian(item.id)}
                            className="p-2 rounded-full bg-emerald-500 text-white hover:bg-emerald-600"
                            title="Onayla"
                          >
                            <Check size={16} />
                          </button>
                          <button
                            className="p-2 rounded-full bg-rose-500 text-white/80 cursor-not-allowed"
                            title="Reddet (yakında)"
                            disabled
                          >
                            <X size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Başvuru detayı ve diploma önizleme modalı */}
        {(selected || detailLoading) && (
          <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
            <div className="w-full max-w-5xl rounded-2xl border border-slate-200 bg-white shadow-2xl">
              <div className="flex items-center justify-between border-b border-slate-200 px-6 py-5">
                <div>
                  <h2 className="text-2xl font-bold text-slate-900">Diyetisyen detayları</h2>
                  <p className="mt-1 text-sm text-slate-500">Kayıt bilgilerini ve diploma belgesini inceleyin</p>
                </div>
                <button onClick={() => setSelected(null)} className="p-2 rounded-full hover:bg-slate-100">
                  <X size={24} />
                </button>
              </div>

              {detailLoading ? (
                <div className="p-10 text-center text-slate-500">Yukleniyor...</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5 p-6">
                  <div className="rounded-2xl bg-slate-50 border border-slate-200 p-6">
                    <div className="w-20 h-20 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-4xl font-bold mb-6">
                      {(selected?.firstName || "D").charAt(0)}
                    </div>
                    <DetailRow label="Ad Soyad" value={`Dr. ${selected?.firstName || ""} ${selected?.lastName || ""}`} />
                    <DetailRow label="E-posta" value={selected?.email || "-"} />
                    <DetailRow label="Diploma No" value={selected?.diplomaNo || "-"} />
                    <DetailRow label="Klinik" value={selected?.clinicName || "-"} />
                    <DetailRow
                      label="Kayit Tarihi"
                      value={selected?.createdAt ? new Date(selected.createdAt).toLocaleDateString("tr-TR") : "-"}
                    />
                    <div className="mt-4">
                      <p className="text-slate-500 text-sm">Durum</p>
                      <span className="inline-flex rounded-full px-3 py-1 text-xs font-semibold bg-amber-100 text-amber-700 mt-2">
                        {selected?.isApproved ? "Onaylandı" : "Beklemede"}
                      </span>
                    </div>
                  </div>

                  <div className="rounded-2xl bg-slate-50 border border-slate-200 p-6 flex flex-col min-h-[420px]">
                    <h3 className="text-2xl font-bold mb-4">Diploma / Sertifika</h3>
                    {(() => {
                      const docUrl = resolveDocumentHref(selected?.diplomaDocumentUrl);
                      if (!docUrl) {
                        return (
                          <div className="flex-1 rounded-2xl border border-dashed border-slate-300 flex flex-col items-center justify-center text-slate-500 p-6 text-center">
                            <Download size={48} className="opacity-40" />
                            <p className="mt-4">Bu başvuruda yüklenmiş belge yok.</p>
                            <p className="mt-2 text-sm">Eski kayıtlar belge yüklemeden oluşturulmuş olabilir.</p>
                          </div>
                        );
                      }
                      const pdf = isPdfUrl(docUrl);
                      return (
                        <div className="flex-1 flex flex-col gap-4">
                          <div className="flex-1 min-h-[280px] rounded-xl border border-slate-200 bg-white overflow-hidden">
                            {pdf ? (
                              <iframe
                                title="Diploma önizleme"
                                src={docUrl}
                                className="w-full h-full min-h-[280px]"
                              />
                            ) : (
                              <img
                                src={docUrl}
                                alt="Diploma belgesi"
                                className="w-full h-full max-h-[360px] object-contain bg-slate-100"
                              />
                            )}
                          </div>
                          <div className="flex flex-wrap gap-3">
                            <a
                              href={docUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-2 rounded-xl bg-emerald-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-emerald-600 no-underline"
                            >
                              <Eye size={16} />
                              Yeni sekmede aç
                            </a>
                            <a
                              href={docUrl}
                              download
                              className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 no-underline"
                            >
                              <Download size={16} />
                              İndir
                            </a>
                          </div>
                        </div>
                      );
                    })()}

                    <button
                      onClick={approveFromModal}
                      className="mt-5 w-full py-3 rounded-2xl bg-emerald-500 text-white font-semibold hover:bg-emerald-600"
                    >
                      Onayla
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </AdminPageShell>
    </SidebarLayout>
  );
}

// Modal içi etiket-değer satırı
function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="mb-4">
      <p className="text-slate-500 text-sm">{label}</p>
      <p className="text-lg font-semibold text-slate-900">{value}</p>
    </div>
  );
}
