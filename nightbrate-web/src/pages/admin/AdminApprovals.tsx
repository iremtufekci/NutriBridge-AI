import { useEffect, useMemo, useState } from "react";
import { Check, Download, Eye, X } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

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
  const [pending, setPending] = useState<PendingDietitian[]>([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selected, setSelected] = useState<any | null>(null);

  const pendingCount = useMemo(() => pending.length, [pending]);

  const loadPending = async () => {
    setLoading(true);
    try {
      const { data } = await api.get("/api/admin/pending-dietitians");
      setPending(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Bekleyen diyetisyenler alinamadi", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPending();
  }, []);

  const approveDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${dietitianId}`);
      await loadPending();
    } catch (error) {
      alert("Onaylama basarisiz: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  const inspectDietitian = async (dietitianId?: string) => {
    if (!dietitianId) return;
    setDetailLoading(true);
    try {
      const { data } = await api.get(`/api/admin/dietitian/${dietitianId}`);
      setSelected(data);
    } catch (error) {
      alert("Detaylar alinamadi: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    } finally {
      setDetailLoading(false);
    }
  };

  const approveFromModal = async () => {
    const id = selected?.id;
    if (!id) return;
    try {
      await api.post(`/api/admin/approve-dietitian/${id}`);
      setSelected(null);
      await loadPending();
    } catch (error) {
      alert("Onaylama basarisiz: " + ((error as any)?.response?.data?.message || "Bilinmeyen hata"));
    }
  };

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 md:pb-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-3xl sm:text-5xl font-bold">Diyetisyen Onaylari</h1>
            <p className="text-slate-500 dark:text-[#9CA3AF] mt-1">Bekleyen kayitlari inceleyin ve onaylayin</p>
          </div>
          <div className="px-4 py-2 rounded-full bg-amber-100 text-amber-700 font-semibold w-fit">
            {pendingCount} Onay Bekliyor
          </div>
        </div>

        <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#1F2937] overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px]">
              <thead className="bg-slate-50 dark:bg-slate-800/60">
                <tr className="text-left text-sm text-slate-500 dark:text-slate-300">
                  <th className="p-4">Ad Soyad</th>
                  <th className="p-4">E-posta</th>
                  <th className="p-4">Diploma No</th>
                  <th className="p-4">Klinik</th>
                  <th className="p-4">Kayit Tarihi</th>
                  <th className="p-4">Durum</th>
                  <th className="p-4">Islemler</th>
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
                    <tr key={item.id} className="border-t border-slate-100 dark:border-slate-800">
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
                      <td className="p-4 text-slate-600 dark:text-slate-300 text-sm break-all">
                        {item.email || "—"}
                      </td>
                      <td className="p-4 text-slate-600 dark:text-slate-300">{item.diplomaNo || "-"}</td>
                      <td className="p-4 text-slate-600 dark:text-slate-300">{item.clinicName || "-"}</td>
                      <td className="p-4 text-slate-600 dark:text-slate-300">
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
                            className="p-2 rounded-full border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-[#2D3748]"
                            title="Incele"
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
                            title="Reddet (yakinda)"
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

        {(selected || detailLoading) && (
          <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
            <div className="w-full max-w-5xl rounded-3xl bg-white dark:bg-[#1F2937] border border-slate-200 dark:border-slate-700 shadow-2xl">
              <div className="flex items-center justify-between p-6 border-b border-slate-200 dark:border-slate-700">
                <div>
                  <h2 className="text-4xl font-bold">Diyetisyen Detaylari</h2>
                  <p className="text-slate-500 dark:text-[#9CA3AF] mt-1">Kayit bilgilerini inceleyin</p>
                </div>
                <button onClick={() => setSelected(null)} className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-[#2D3748]">
                  <X size={24} />
                </button>
              </div>

              {detailLoading ? (
                <div className="p-10 text-center text-slate-500">Yukleniyor...</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5 p-6">
                  <div className="rounded-2xl bg-slate-50 dark:bg-[#0D1117] border border-slate-200 dark:border-slate-700 p-6">
                    <div className="w-20 h-20 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-4xl font-bold mb-6">
                      {(selected?.firstName || "D").charAt(0)}
                    </div>
                    <DetailRow label="Ad Soyad" value={`Dr. ${selected?.firstName || ""} ${selected?.lastName || ""}`} />
                    <DetailRow label="Diploma No" value={selected?.diplomaNo || "-"} />
                    <DetailRow label="Klinik" value={selected?.clinicName || "-"} />
                    <DetailRow
                      label="Kayit Tarihi"
                      value={selected?.createdAt ? new Date(selected.createdAt).toLocaleDateString("tr-TR") : "-"}
                    />
                    <div className="mt-4">
                      <p className="text-slate-500 dark:text-[#9CA3AF] text-sm">Durum</p>
                      <span className="inline-flex rounded-full px-3 py-1 text-xs font-semibold bg-amber-100 text-amber-700 mt-2">
                        {selected?.isApproved ? "Onaylandi" : "Beklemede"}
                      </span>
                    </div>
                  </div>

                  <div className="rounded-2xl bg-slate-50 dark:bg-[#0D1117] border border-slate-200 dark:border-slate-700 p-6 flex flex-col">
                    <h3 className="text-3xl font-bold mb-4">Diploma/Sertifika</h3>
                    <div className="flex-1 rounded-2xl border border-dashed border-slate-300 dark:border-slate-600 flex flex-col items-center justify-center text-slate-500 dark:text-[#9CA3AF]">
                      <Download size={56} />
                      <p className="mt-4 text-xl">Diploma Onizleme</p>
                      <button
                        onClick={() => {
                          if (selected?.diplomaDocumentUrl) {
                            window.open(selected.diplomaDocumentUrl, "_blank");
                          } else {
                            alert("Bu kayit icin yuklenmis diploma dosyasi bulunmuyor.");
                          }
                        }}
                        className="mt-4 px-6 py-2 rounded-full bg-emerald-500 text-white font-semibold hover:bg-emerald-600"
                      >
                        Indir
                      </button>
                    </div>

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
      </div>
    </SidebarLayout>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="mb-4">
      <p className="text-slate-500 dark:text-[#9CA3AF] text-sm">{label}</p>
      <p className="font-semibold text-2xl text-slate-900 dark:text-slate-100">{value}</p>
    </div>
  );
}
