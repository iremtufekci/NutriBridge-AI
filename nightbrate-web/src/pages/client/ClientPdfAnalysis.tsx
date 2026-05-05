import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { FileText, Loader2, Upload } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { API_BASE_URL, api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

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

function resolvePdfHref(path: string): string {
  if (!path) return "#";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  const base = API_BASE_URL.replace(/\/$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  if (!base) return p;
  return `${base}${p}`;
}

export function ClientPdfAnalysis() {
  const userName = useAuthProfileDisplayName();
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<UploadResult | null>(null);
  const [history, setHistory] = useState<ListItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);

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

  const uploadFile = useCallback(
    async (file: File) => {
      setError(null);
      setResult(null);
      const fd = new FormData();
      const name = file.name?.toLowerCase().endsWith(".pdf") ? file.name : "belge.pdf";
      fd.append("pdf", file, name);
      setBusy(true);
      try {
        const { data } = await api.post<UploadResult>("/api/Client/pdf-analyses/upload", fd, {
          timeout: 300_000,
        });
        setResult(data);
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

  const displayItem = useMemo(() => result ?? null, [result]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="mx-auto max-w-lg px-4 py-6 pb-28 lg:pb-8">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-600">
            <FileText className="h-6 w-6" aria-hidden />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-slate-800">PDF analizi</h1>
            <p className="text-sm text-slate-500">
              Laboratuvar, rapor veya diyet belgesi yükleyin; yapay zekâ Türkçe özet üretir. Tıbbi tanı yerine geçmez.
            </p>
          </div>
        </div>

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

        {displayItem && (
          <section className="mt-6 space-y-4 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-xs font-semibold uppercase tracking-wide text-emerald-600">
                {displayItem.documentType}
              </p>
              <span className="text-xs text-slate-500">
                {displayItem.analysisSource === "gemini" ? "Yapay zeka analizi" : "Örnek (yapay zeka devre dışı)"}
              </span>
            </div>
            <a
              href={resolvePdfHref(displayItem.pdfUrl)}
              target="_blank"
              rel="noreferrer"
              className="block truncate text-sm font-medium text-emerald-600 underline"
            >
              {displayItem.originalFileName}
            </a>
            <p className="text-sm leading-relaxed text-slate-700">{displayItem.summary}</p>
            {displayItem.keyFindings?.length > 0 && (
              <div>
                <p className="mb-1 text-xs font-bold uppercase text-slate-500">Öne çıkanlar</p>
                <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
                  {displayItem.keyFindings.map((x, i) => (
                    <li key={i}>{x}</li>
                  ))}
                </ul>
              </div>
            )}
            {displayItem.cautions?.length > 0 && (
              <div className="rounded-2xl border border-amber-200/80 bg-amber-50/90 p-3">
                <p className="mb-1 text-xs font-bold uppercase text-amber-800">Dikkat</p>
                <ul className="list-disc space-y-1 pl-5 text-sm text-amber-900">
                  {displayItem.cautions.map((x, i) => (
                    <li key={i}>{x}</li>
                  ))}
                </ul>
              </div>
            )}
            {displayItem.suggestedForDietitian?.length > 0 && (
              <div>
                <p className="mb-1 text-xs font-bold uppercase text-slate-500">Diyetisyenle paylaşım</p>
                <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
                  {displayItem.suggestedForDietitian.map((x, i) => (
                    <li key={i}>{x}</li>
                  ))}
                </ul>
              </div>
            )}
          </section>
        )}

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
              {history.map((h) => (
                <li
                  key={h.id}
                  className="rounded-2xl border border-slate-200 bg-white p-4"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-800">{h.originalFileName}</p>
                      <p className="text-xs text-slate-500">{h.documentType}</p>
                    </div>
                    <a
                      href={resolvePdfHref(h.pdfUrl)}
                      target="_blank"
                      rel="noreferrer"
                      className="shrink-0 text-xs font-semibold text-emerald-600"
                    >
                      PDF
                    </a>
                  </div>
                  <p className="mt-2 line-clamp-2 text-xs text-slate-600">{h.summary}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </SidebarLayout>
  );
}
