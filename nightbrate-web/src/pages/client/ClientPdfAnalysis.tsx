// React kancaları
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
// İkonlar
import { FileText, Loader2, Upload } from "lucide-react";
// Kenar çubuğu düzeni
import { SidebarLayout } from "../../components/SidebarLayout";
// API taban URL ve istemci
import { API_BASE_URL, api, getApiErrorMessage } from "../../api/http";
// Kullanıcı görünen adı
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import { isMockNetworkSource, isRealAiSource } from "../../lib/aiSource";

/** Backend'den dönen tek bir PDF analiz kaydı (yükleme + geçmiş listesi aynı şekil). */
type UploadResult = {
  id: string;
  pdfUrl: string;
  originalFileName: string;
  documentType: string;
  summary: string;
  keyFindings: string[];
  cautions: string[];
  suggestedForDietitian: string[];
  analysisSource: string;
  createdAtUtc: string;
};

type ListItem = UploadResult;

/** UTC ISO tarihini kullanıcıya Türkçe gösterir. */
function formatAnalysisDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("tr-TR", {
      day: "numeric",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

/** PDF yorumu: laboratuvar değerlerine göre kilo ve beslenme değerlendirmesi. */
function AnalysisDetailCard({ item }: { item: UploadResult }) {
  const comments = item.suggestedForDietitian?.filter((x) => x.trim()) ?? [];

  return (
    <section className="space-y-4 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-emerald-600">
          {item.documentType}
        </p>
        <span className="text-xs text-slate-500">
          {isRealAiSource(item.analysisSource)
            ? "Yapay zeka analizi (Groq)"
            : isMockNetworkSource(item.analysisSource)
              ? "Örnek (Groq'a bağlanılamadı)"
              : "Örnek (Groq yapılandırılmamış)"}
        </span>
      </div>
      {item.createdAtUtc && (
        <p className="text-xs text-slate-400">{formatAnalysisDate(item.createdAtUtc)}</p>
      )}
      <a
        href={resolvePdfHref(item.pdfUrl)}
        target="_blank"
        rel="noreferrer"
        className="block truncate text-sm font-medium text-emerald-600 underline"
      >
        {item.originalFileName}
      </a>
      {comments.length > 0 ? (
        <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-4">
          <p className="mb-3 text-xs font-bold uppercase tracking-wide text-slate-600">PDF yorumu</p>
          <ul className="space-y-3 text-sm leading-relaxed text-slate-800">
            {comments.map((x, i) => (
              <li key={i} className="flex gap-2">
                <span className="shrink-0 text-slate-400">•</span>
                <span>{x}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <p className="text-sm text-slate-500">Bu belge için yorum üretilemedi.</p>
      )}
    </section>
  );
}

// Geçmiş listesinde kısa önizleme metni
function historyPreviewText(item: UploadResult): string {
  const first = item.suggestedForDietitian?.find((x) => x.trim());
  if (first) return first;
  return item.summary || "";
}

/** Göreli yol (/uploads/...) ise API taban adresiyle tam PDF linki üretir. */
function resolvePdfHref(path: string): string {
  if (!path) return "#";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const base = API_BASE_URL.replace(/\/$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  if (!base) return p;
  return `${base}${p}`;
}

// Laboratuvar PDF yükleme ve analiz geçmişi sayfası
export function ClientPdfAnalysis() {
  const userName = useAuthProfileDisplayName();
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<UploadResult | null>(null); // Az önce yüklenen analiz
  const [selectedHistoryId, setSelectedHistoryId] = useState<string | null>(null); // Geçmişten seçilen kayıt id'si
  const [history, setHistory] = useState<ListItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);

  // Sayfa açılışında ve yeni yüklemeden sonra danışanın geçmiş PDF analizlerini çeker
  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const { data } = await api.get<ListItem[]>("/api/Client/pdf-analyses", { params: { take: 30 } });
      setHistory(Array.isArray(data) ? data : []);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  // Seçilen PDF'i multipart olarak backend'e gönderir; Gemini analizi orada yapılır
  const uploadFile = useCallback(
    async (file: File) => {
      setError(null);
      setResult(null);
      setSelectedHistoryId(null);
      const fd = new FormData();
      const name = file.name?.toLowerCase().endsWith(".pdf") ? file.name : "belge.pdf";
      fd.append("pdf", file, name);
      setBusy(true);
      try {
        const { data } = await api.post<UploadResult>("/api/Client/pdf-analyses/upload", fd, {
          timeout: 300_000,
        });
        setResult(data);
        setSelectedHistoryId(data.id);
        await loadHistory();
      } catch (e) {
        setError(getApiErrorMessage(e));
      } finally {
        setBusy(false);
      }
    },
    [loadHistory]
  );

  const onPick = useCallback(() => inputRef.current?.click(), []);

  // Üstte gösterilecek detay: önce yeni sonuç, yoksa geçmişten seçilen kayıt
  const displayItem = useMemo(() => {
    if (result) return result;
    if (!selectedHistoryId) return null;
    return history.find((h) => h.id === selectedHistoryId) ?? null;
  }, [result, selectedHistoryId, history]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="mx-auto max-w-lg px-4 py-6 pb-28 lg:pb-8">
        {/* Sayfa başlığı ve açıklama */}
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-600">
            <FileText className="h-6 w-6" aria-hidden />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-slate-800">PDF analizi</h1>
            <p className="text-sm text-slate-500">
              Laboratuvar sonuçlarınızı yükleyin; değerlere göre kilo alımı/verememe ile ilişkili diyetisyen
              yorumları üretilir. Tıbbi tanı yerine geçmez.
            </p>
          </div>
        </div>

        {/* Gizli PDF dosya girişi */}
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            e.target.value = "";
            if (f) void uploadFile(f);
          }}
        />

        {/* Yükleme düğmesi */}
        <button
          type="button"
          disabled={busy}
          onClick={onPick}
          className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#2ECC71] py-4 text-base font-semibold text-white shadow-sm transition hover:bg-emerald-600 disabled:opacity-60"
        >
          {busy ? <Loader2 className="h-5 w-5 animate-spin" /> : <Upload className="h-5 w-5" />}
          {busy ? "Analiz ediliyor…" : "PDF seç ve yükle"}
        </button>

        {error && (
          <div className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {/* Seçili analiz detay kartı */}
        {displayItem && (
          <div className="mt-6">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">
              {result ? "Son analiz" : "Analiz detayı"}
            </h2>
            <AnalysisDetailCard item={displayItem} />
          </div>
        )}

        {/* Geçmiş yüklemeler listesi */}
        <section className="mt-10">
          <h2 className="mb-3 text-base font-semibold text-slate-800">Geçmiş yüklemeler</h2>
          {historyLoading ? (
            <div className="flex justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-emerald-500" />
            </div>
          ) : history.length === 0 ? (
            <p className="text-sm text-slate-500">Henüz kayıtlı PDF yok.</p>
          ) : (
            <ul className="space-y-3">
              {history.map((h) => {
                const selected = selectedHistoryId === h.id;
                return (
                  <li key={h.id}>
                    <button
                      type="button"
                      onClick={() => {
                        setResult(null);
                        setSelectedHistoryId(h.id);
                      }}
                      className={`w-full rounded-2xl border p-4 text-left transition ${
                        selected
                          ? "border-emerald-400 bg-emerald-50/60 ring-1 ring-emerald-200"
                          : "border-slate-200 bg-white hover:border-emerald-200 hover:bg-slate-50"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-semibold text-slate-800">{h.originalFileName}</p>
                          <p className="text-xs text-slate-500">{h.documentType}</p>
                          {h.createdAtUtc && (
                            <p className="mt-1 text-xs text-slate-400">{formatAnalysisDate(h.createdAtUtc)}</p>
                          )}
                        </div>
                        <a
                          href={resolvePdfHref(h.pdfUrl)}
                          target="_blank"
                          rel="noreferrer"
                          onClick={(e) => e.stopPropagation()}
                          className="shrink-0 text-xs font-semibold text-emerald-600"
                        >
                          PDF
                        </a>
                      </div>
                      <p className="mt-2 line-clamp-2 text-xs text-slate-600">{historyPreviewText(h)}</p>
                      <p className="mt-2 text-xs font-semibold text-emerald-600">
                        {selected ? "Detay açık ↑" : "Tam analizi gör →"}
                      </p>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
    </SidebarLayout>
  );
}
