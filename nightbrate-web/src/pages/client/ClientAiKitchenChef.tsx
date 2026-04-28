import { useCallback, useEffect, useMemo, useState } from "react";
import { ChefHat, Lightbulb, Loader2, Share2 } from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api, getApiErrorMessage } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

const PREFERENCES: { code: string; label: string }[] = [
  { code: "practical", label: "Pratik" },
  { code: "low_calorie", label: "Düşük Kalori" },
  { code: "vegetarian", label: "Vejetaryen" },
  { code: "high_protein", label: "Protein" },
  { code: "vegan", label: "Vegan" },
  { code: "gluten_free", label: "Gluten Free" },
];

type Recipe = {
  title: string;
  description?: string | null;
  estimatedCalories: number;
  prepTimeMinutes?: number | null;
  ingredients: string[];
  steps: string[];
};

type KitchenChefResponse = {
  recipes: Recipe[];
  source?: string;
};

export function ClientAiKitchenChef() {
  const userName = useAuthProfileDisplayName();
  const [ingredients, setIngredients] = useState("");
  const [preference, setPreference] = useState<string | null>(null);
  const [targetCalories, setTargetCalories] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<KitchenChefResponse | null>(null);
  /** Diyetisyene yalnızca bir tarif gider — liste (radyo) ile tek seçim. */
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
  const [saveBusy, setSaveBusy] = useState(false);
  const [saveInfo, setSaveInfo] = useState<string | null>(null);

  useEffect(() => {
    setSelectedIndex(null);
    setSaveInfo(null);
  }, [result]);

  const valid = useMemo(() => {
    const k = parseInt(targetCalories.replace(/\D/g, ""), 10);
    return (
      ingredients.trim().length > 0 &&
      preference !== null &&
      !Number.isNaN(k) &&
      k >= 200 &&
      k <= 5000
    );
  }, [ingredients, preference, targetCalories]);

  const submit = useCallback(async () => {
    setError(null);
    if (!valid) {
      setError("Malzeme, bir tercih ve 200–5000 arası hedef kalori zorunludur.");
      return;
    }
    const kcal = parseInt(targetCalories.replace(/\D/g, ""), 10);
    setBusy(true);
    setResult(null);
    setSaveInfo(null);
    try {
      const { data } = await api.post<KitchenChefResponse>(
        "/api/KitchenChef/generate",
        {
          ingredients: ingredients.trim(),
          preference,
          targetCalories: kcal,
        },
        { timeout: 200_000 }
      );
      setResult(data);
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }, [ingredients, preference, targetCalories, valid]);

  const shareWithDietitian = useCallback(async () => {
    if (!result?.recipes?.length) return;
    setSaveInfo(null);
    setError(null);
    const kcal = parseInt(targetCalories.replace(/\D/g, ""), 10);
    if (!preference || !ingredients.trim()) {
      setError("Önce tarif üretin, sonra paylaşın.");
      return;
    }
    if (selectedIndex === null || !result.recipes[selectedIndex]) {
      setError("Paylaşmak için listelerden yalnızca bir tarif seçin (radio).");
      return;
    }
    const picked = [result.recipes[selectedIndex]];
    setSaveBusy(true);
    try {
      await api.post("/api/KitchenChef/save", {
        ingredients: ingredients.trim(),
        preference,
        targetCalories: kcal,
        source: result.source ?? "mock",
        selectedRecipes: picked,
      });
      setSaveInfo("Seçtiğiniz tarif kaydedildi. Diyetisyeniniz “AI Denetimi” ekranından görebilir.");
    } catch (e) {
      setError(getApiErrorMessage(e));
    } finally {
      setSaveBusy(false);
    }
  }, [ingredients, preference, result, selectedIndex, targetCalories]);

  return (
    <SidebarLayout userRole="client" userName={userName}>
      <div className="mx-auto max-w-2xl px-4 py-6 pb-28 lg:pb-8">
        <div className="mb-6 rounded-2xl border border-emerald-200/80 bg-gradient-to-br from-emerald-50/90 to-white p-5 dark:from-emerald-950/40 dark:to-[#1F2937] dark:border-emerald-900/50">
          <div className="flex items-center gap-3">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-emerald-200/60 dark:bg-[#1F2937] dark:ring-emerald-800/50">
              <ChefHat className="h-8 w-8 text-emerald-600 dark:text-emerald-400" aria-hidden />
            </div>
            <div>
              <h1 className="text-lg font-bold text-slate-800 dark:text-slate-100">AI Mutfak Şefi</h1>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Yapay zeka ile hedef kalorinize uygun tarifler oluşturun
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div>
            <label htmlFor="chef-ingredients" className="mb-2 block text-sm font-semibold text-slate-800 dark:text-slate-200">
              Elinizdeki Malzemeler <span className="text-red-500">*</span>
            </label>
            <textarea
              id="chef-ingredients"
              value={ingredients}
              onChange={(e) => setIngredients(e.target.value)}
              rows={4}
              placeholder="Tavuk, krema, mantar, pirinç..."
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 shadow-sm placeholder:text-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/30 dark:border-slate-600 dark:bg-[#1F2937] dark:text-slate-100 dark:placeholder:text-slate-500"
            />
            <p className="mt-2 flex items-start gap-2 text-xs text-amber-700 dark:text-amber-400/90">
              <Lightbulb className="h-4 w-4 shrink-0 mt-0.5" aria-hidden />
              İpucu: Birden fazla malzeme yazabilirsiniz
            </p>
          </div>

          <div>
            <p className="mb-2 block text-sm font-semibold text-slate-800 dark:text-slate-200">
              Tercih <span className="text-red-500">*</span> <span className="font-normal text-slate-500">(birini seçin)</span>
            </p>
            <div className="flex flex-wrap gap-2">
              {PREFERENCES.map((p) => {
                const on = preference === p.code;
                return (
                  <button
                    key={p.code}
                    type="button"
                    onClick={() => setPreference(p.code)}
                    className={[
                      "rounded-full border px-4 py-2 text-sm font-medium transition-colors",
                      on
                        ? "border-emerald-500 bg-emerald-500 text-white shadow-sm"
                        : "border-slate-200 bg-white text-slate-700 hover:border-emerald-300 dark:border-slate-600 dark:bg-[#1F2937] dark:text-slate-200",
                    ].join(" ")}
                  >
                    {p.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <label htmlFor="chef-kcal" className="mb-2 block text-sm font-semibold text-slate-800 dark:text-slate-200">
              Hedef kalori (kcal) <span className="text-red-500">*</span>
            </label>
            <input
              id="chef-kcal"
              type="number"
              inputMode="numeric"
              min={200}
              max={5000}
              value={targetCalories}
              onChange={(e) => setTargetCalories(e.target.value)}
              placeholder="Örn. 500"
              className="w-full max-w-xs rounded-2xl border border-slate-200 bg-white px-4 py-3 text-slate-800 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/30 dark:border-slate-600 dark:bg-[#1F2937] dark:text-slate-100"
            />
            <p className="mt-1 text-xs text-slate-500">200 – 5000 kcal arası (öğüne veya porsiyona göre hedefiniz)</p>
          </div>

          <button
            type="button"
            onClick={() => void submit()}
            disabled={busy || !valid}
            className="inline-flex w-full min-h-[52px] items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 px-4 text-base font-semibold text-white shadow-md active:opacity-90 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto sm:min-w-[220px]"
          >
            {busy ? <Loader2 className="h-5 w-5 animate-spin" /> : <ChefHat className="h-5 w-5" />}
            Tarif Oluştur
          </button>
        </div>

        {busy && (
          <div className="mt-6 flex items-center gap-2 rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-600 dark:bg-[#1F2937]">
            <Loader2 className="h-5 w-5 animate-spin text-emerald-600" />
            <span className="text-sm text-slate-600 dark:text-slate-300">Tarifler hazırlanıyor… Lütfen bu sayfadan ayrılmayın.</span>
          </div>
        )}

        {error && (
          <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-200">
            {error}
          </div>
        )}

        {result && !busy && result.recipes?.length > 0 && (
          <div className="mt-8 space-y-6">
            {result.source && (
              <p
                className={`text-xs font-semibold ${
                  result.source === "gemini" ? "text-blue-700 dark:text-blue-300" : "text-amber-800 dark:text-amber-300"
                }`}
              >
                {result.source === "gemini"
                  ? "Kaynak: Google Gemini"
                  : "Kaynak: Yerel önizleme (API’de Gemini anahtarı yoksa)"}
              </p>
            )}
            <p className="text-sm text-slate-600 dark:text-slate-400">
              Aşağıda <strong>birden fazla</strong> tarif göreceksiniz. Diyetisyeninizle paylaşmak istediğiniz{" "}
              <strong>yalnızca birini</strong> işaretleyip gönderin.
            </p>
            {result.recipes.map((r, i) => (
              <article
                key={r.title + (r.estimatedCalories ?? "") + i}
                className="overflow-hidden rounded-2xl border border-emerald-200/80 bg-white shadow-sm dark:border-emerald-900/40 dark:bg-[#1F2937]"
              >
                <div className="border-b border-emerald-100 bg-emerald-50/50 px-5 py-4 dark:border-emerald-900/30 dark:bg-emerald-950/20">
                  <label className="flex cursor-pointer items-start gap-3">
                    <input
                      type="radio"
                      name="share-one-recipe"
                      checked={selectedIndex === i}
                      onChange={() => setSelectedIndex(i)}
                      className="mt-1.5 h-4 w-4 border-slate-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    <h2 className="text-lg font-bold text-slate-800 dark:text-slate-100">{r.title}</h2>
                  </label>
                  {r.description && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{r.description}</p>}
                  <div className="mt-2 flex flex-wrap gap-3 text-sm">
                    <span className="font-semibold text-emerald-700 dark:text-emerald-400">{r.estimatedCalories} kcal</span>
                    {r.prepTimeMinutes != null && r.prepTimeMinutes > 0 && (
                      <span className="text-slate-600 dark:text-slate-400">~{r.prepTimeMinutes} dk</span>
                    )}
                  </div>
                </div>
                <div className="space-y-4 px-5 py-4 text-sm text-slate-700 dark:text-slate-300">
                  <div>
                    <p className="mb-2 font-semibold text-slate-800 dark:text-slate-200">Malzemeler</p>
                    <ul className="list-inside list-disc space-y-1">
                      {(r.ingredients ?? []).map((x) => (
                        <li key={x}>{x}</li>
                      ))}
                    </ul>
                  </div>
                  <div>
                    <p className="mb-2 font-semibold text-slate-800 dark:text-slate-200">Hazırlanış</p>
                    <ol className="list-inside list-decimal space-y-2">
                      {(r.steps ?? []).map((s, i) => (
                        <li key={i} className="pl-1">
                          {s}
                        </li>
                      ))}
                    </ol>
                  </div>
                </div>
              </article>
            ))}
            <button
              type="button"
              onClick={() => void shareWithDietitian()}
              disabled={saveBusy}
              className="inline-flex w-full min-h-[48px] items-center justify-center gap-2 rounded-2xl border-2 border-emerald-500 bg-white px-4 text-sm font-semibold text-emerald-700 shadow-sm hover:bg-emerald-50 dark:border-emerald-600 dark:bg-[#1F2937] dark:text-emerald-300 dark:hover:bg-emerald-950/30 sm:w-auto"
            >
              {saveBusy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Share2 className="h-4 w-4" />}
              Seçili tarifi diyetisyenle paylaş
            </button>
            {saveInfo && <p className="text-sm text-emerald-700 dark:text-emerald-400">{saveInfo}</p>}
          </div>
        )}

        {!result && !busy && !error && (
          <div className="mt-12 flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/50 py-12 text-center dark:border-slate-600 dark:bg-slate-800/30">
            <div className="mb-3 flex h-16 w-16 items-center justify-center rounded-full bg-slate-200/80 dark:bg-slate-700/80">
              <ChefHat className="h-8 w-8 text-slate-500" />
            </div>
            <p className="text-sm font-medium text-slate-600 dark:text-slate-400">Malzemelerinizi ve hedef kalorinizi girin</p>
            <p className="text-xs text-slate-500 dark:text-slate-500">Birden fazla tarif arasından birini seçip paylaşabilirsiniz</p>
          </div>
        )}
      </div>
    </SidebarLayout>
  );
}
