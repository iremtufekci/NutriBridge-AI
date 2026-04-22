import { useEffect, useMemo, useState } from "react";
import { ChevronRight, Info, Lock, Moon, UserRound } from "lucide-react";
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

export function ClientProfile() {
  const [profile, setProfile] = useState<ClientProfileData>(defaultProfile);
  const [infoText, setInfoText] = useState("");

  const userName = useMemo(
    () => `${profile.firstName} ${profile.lastName}`.trim() || "Danisan",
    [profile.firstName, profile.lastName]
  );

  useEffect(() => {
    const loadProfile = async () => {
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
    };
    loadProfile();
  }, []);

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
              onClick={() => setInfoText("Hakkinda: NutriBridge AI, danisan ve diyetisyen surecini kolaylastirmak icin gelistirilmistir.")}
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
