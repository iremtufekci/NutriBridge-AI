import { useCallback, useMemo, useRef, useState } from "react";
import { Camera, ImagePlus, Loader2, ScanSearch } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { API_BASE_URL, api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

type AnalysisResponse = {
  mealLogId?: string | null;
  photoUrl: string;
  estimatedCalories: number;
  detectedFoods: string[];
  timestampUtc: string;
  /** gemini | mock */
  analysisSource?: string;
};

function resolvePhotoSrc(photoUrl: string): string {
  if (!photoUrl) return "";
  if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) return photoUrl;
  const base = API_BASE_URL.replace(/\/$/, "");
  const path = photoUrl.startsWith("/") ? photoUrl : `/${photoUrl}`;
  if (!base) return path;
  return `${base}${path}`;
}

export function ClientMealAnalysis() {
  const userName = useAuthProfileDisplayName();
  const galleryRef = useRef<HTMLInputElement>(null);
  const cameraRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [previewObjectUrl, setPreviewObjectUrl] = useState<string | null>(null);

  const uploadFile = useCallback(async (file: File) => {
    setError(null);
    setResult(null);
    setPreviewObjectUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev);
      return URL.createObjectURL(file);
    });

    const fd = new FormData();
    const fileName =
      file.name && /\.(jpe?g|png)$/i.test(file.name)
        ? file.name
        : file.type === "image/png"
          ? "meal.png"
          : "meal.jpg";
    fd.append("photo", file, fileName);
    setBusy(true);
    try {
      if (import.meta.env.DEV) {
        // eslint-disable-next-line no-console -- Ağ sekmesi boşsa: isteğin gerçekten başlayıp başlamadığını ayırt etmek için
        console.debug("[MealAnalysis] İstek başlıyor", {
          ad: file.name,
          boyut: file.size,
          hedef: "/api/Meal/upload-meal-photo",
        });
      }
      const { data } = await api.post<AnalysisResponse>("/api/Meal/upload-meal-photo", fd, {
        timeout: 280_000,
      });
      if (import.meta.env.DEV) {
        // eslint-disable-next-line no-console
        console.debug("[MealAnalysis] Cevap alındı");
      }
      setResult(data);
    } catch (e) {
      if (import.meta.env.DEV) {
        // eslint-disable-next-line no-console
        console.debug("[MealAnalysis] Hata", e);
      }
      setError(getApiErrorMessage(e));
      setPreviewObjectUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev);
        return null;
      });
    } finally {
      setBusy(false);
    }
  }, []);

  const onPickGallery = useCallback(() => galleryRef.current?.click(), []);
  const onPickCamera = useCallback(() => cameraRef.current?.click(), []);

  const displayImg = useMemo(() => {
    if (result?.photoUrl) return resolvePhotoSrc(result.photoUrl);
    return previewObjectUrl || "";
  }, [result, previewObjectUrl]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="mx-auto max-w-lg px-4 py-6 pb-28 lg:pb-8">
        <div className="mb-6 flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-600">
            <ScanSearch className="h-6 w-6" aria-hidden />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-slate-800">Yemek analizi</h1>
            <p className="text-sm text-slate-500">
              Fotoğraf yükleyin; tahmini kalori ve besinler (şimdilik simülasyon) gösterilir.
            </p>
          </div>
        </div>

        <input
          ref={galleryRef}
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            e.target.value = "";
            if (f) void uploadFile(f);
          }}
        />
        <input
          ref={cameraRef}
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          capture="environment"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            e.target.value = "";
            if (f) void uploadFile(f);
          }}
        />

        <div className="flex flex-col gap-3 sm:flex-row">
          <button
            type="button"
            onClick={onPickGallery}
            disabled={busy}
            className="inline-flex min-h-[48px] flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-800 shadow-sm active:bg-slate-50 disabled:opacity-50"
          >
            <ImagePlus className="h-5 w-5" />
            Galeriden seç
          </button>
          <button
            type="button"
            onClick={onPickCamera}
            disabled={busy}
            className="inline-flex min-h-[48px] flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 text-sm font-semibold text-white shadow-sm active:bg-emerald-600 disabled:opacity-50"
          >
            <Camera className="h-5 w-5" />
            Kamerayı aç
          </button>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          Yalnızca JPG/PNG, en fazla 5 MB.
        </p>

        {busy && (
          <div className="mt-6 flex items-center gap-2 rounded-xl border border-slate-200 bg-white p-4">
            <Loader2 className="h-5 w-5 animate-spin text-emerald-600" />
            <span className="text-sm text-slate-600">
              Fotoğraf kaydediliyor ve analiz ediliyor (genelde 30 sn–2 dk, yoğunlukta biraz daha uzayabilir)…
            </span>
          </div>
        )}

        {error && (
          <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </div>
        )}

        {displayImg && !busy && (
          <div className="mt-6 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100">
            <img src={displayImg} alt="Yüklenen öğün" className="max-h-72 w-full object-cover" />
          </div>
        )}

        {result && !busy && (
          <div className="mt-6 space-y-4 rounded-2xl border border-emerald-200 bg-emerald-50/80 p-5">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-emerald-800">
                Tahmini enerji
              </p>
              <p className="text-3xl font-bold text-emerald-700">
                {result.estimatedCalories}{" "}
                <span className="text-lg font-semibold text-emerald-600/90">kkal</span>
              </p>
            </div>
            <div>
              <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-600">
                Tespit edilen besinler
              </p>
              <ul className="flex flex-wrap gap-2">
                {result.detectedFoods.map((f) => (
                  <li
                    key={f}
                    className="rounded-lg bg-white px-3 py-1 text-sm font-medium text-slate-800 shadow-sm"
                  >
                    {f}
                  </li>
                ))}
              </ul>
            </div>
            {result.mealLogId && (
              <p className="text-xs text-slate-500">Kayıt ID: {result.mealLogId}</p>
            )}
            {result.analysisSource && (
              <p
                className={`text-xs font-semibold ${
                  result.analysisSource === "gemini"
                    ? "text-blue-700"
                    : "text-amber-700"
                }`}
              >
                {result.analysisSource === "gemini"
                  ? "Analiz kaynağı: yapay zeka hizmeti"
                  : "Analiz kaynağı: yerel simülasyon (sunucuda yapay zeka anahtarı tanımlı değil)"}
              </p>
            )}
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
