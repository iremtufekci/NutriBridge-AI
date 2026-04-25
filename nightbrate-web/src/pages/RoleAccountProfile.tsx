import { useEffect, useState } from "react";
import { UserRound, Mail, Building2, Award, KeyRound } from "lucide-react";
import { SidebarLayout } from "../components/SidebarLayout";
import { useAuthProfileDisplayName } from "../hooks/useAuthProfileDisplayName";
import { api } from "../api/http";

type AuthProfile = {
  email: string;
  role: string;
  firstName: string;
  lastName: string;
  displayName: string;
  clinicName?: string;
  diplomaNo?: string;
  connectionCode?: string;
};

const empty: AuthProfile = {
  email: "",
  role: "",
  firstName: "",
  lastName: "",
  displayName: "",
  clinicName: undefined,
  diplomaNo: undefined,
};

export type AccountProfileData = {
  profile: AuthProfile;
  err: string;
  name: string;
};

export function useAccountProfile(): AccountProfileData {
  const [profile, setProfile] = useState<AuthProfile>(empty);
  const [err, setErr] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const { data: raw } = await api.get<AuthProfile>("/api/Auth/profile");
        const data = { ...raw } as AuthProfile & { ConnectionCode?: string };
        if (!data.connectionCode && (raw as { ConnectionCode?: string }).ConnectionCode) {
          data.connectionCode = (raw as { ConnectionCode?: string }).ConnectionCode;
        }
        setProfile(data);
        if (data.displayName) localStorage.setItem("userName", data.displayName);
      } catch (e) {
        setErr("Profil yuklenemedi. Oturum acik mi kontrol edin.");
        console.error(e);
      }
    })();
  }, []);

  const name =
    [profile.firstName, profile.lastName].filter((x) => (x || "").trim()).length > 0
      ? `${profile.firstName} ${profile.lastName}`.trim()
      : profile.displayName;

  return { profile, err, name };
}

type PanelProps = { appRole: "admin" | "dietitian" };

export function AccountProfilePanel({ appRole }: PanelProps) {
  const { profile, err, name } = useAccountProfile();

  return (
    <>
      {err && (
        <p className="mb-4 rounded-lg border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-600 dark:text-red-400">
          {err}
        </p>
      )}

      <div className="space-y-4 rounded-2xl border border-slate-200 bg-white/80 p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800/50">
        <div className="flex items-center gap-3 text-slate-800 dark:text-slate-200">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/20 text-2xl font-bold text-emerald-700 dark:text-emerald-300">
            {name
              .split(" ")
              .map((n) => n[0])
              .join("")
              .slice(0, 2)
              .toUpperCase() || "?"}
          </div>
          <div>
            <p className="text-lg font-semibold">{name || "—"}</p>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              {appRole === "dietitian" ? "Diyetisyen" : "Yönetici"}
            </p>
          </div>
        </div>
        <div className="flex items-start gap-2 border-t border-slate-200 pt-4 dark:border-slate-600">
          <Mail className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
          <div>
            <p className="text-xs text-slate-500 dark:text-slate-400">E-posta</p>
            <p className="font-medium text-slate-900 dark:text-slate-100">{profile.email || "—"}</p>
          </div>
        </div>
        {appRole === "dietitian" && (
          <>
            <div className="flex items-start gap-2">
              <Building2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Klinik / çalışma alanı</p>
                <p className="font-medium text-slate-900 dark:text-slate-100">{profile.clinicName || "—"}</p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <Award className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              <div>
                <p className="text-xs text-slate-500 dark:text-slate-400">Diploma no</p>
                <p className="font-medium text-slate-900 dark:text-slate-100">{profile.diplomaNo || "—"}</p>
              </div>
            </div>
            <div className="flex items-start gap-2 border-t border-slate-200 pt-4 dark:border-slate-600">
              <KeyRound className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
              <div className="min-w-0 flex-1">
                <p className="text-xs text-slate-500 dark:text-slate-400">Diyetisyen takip kodu (danışan eşleşmesi)</p>
                {profile.connectionCode && profile.connectionCode.length > 0 ? (
                  <>
                    <p className="mt-1 font-mono text-2xl font-bold tracking-[0.2em] text-slate-900 dark:text-slate-100">
                      {profile.connectionCode}
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
                      Danışanlar uygulamada bu 6 haneli kodu girerek size bağlanır. Kod veritabanındaki
                      onaylı hesabınızdan yüklenir.
                    </p>
                  </>
                ) : (
                  <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
                    Kod henüz yok. Yönetici onayı sonrası otomatik atanır; onaylıysanız kısa süre sonra
                    yenileyin.
                  </p>
                )}
              </div>
            </div>
          </>
        )}
        {appRole === "admin" && (
          <p className="border-t border-slate-200 pt-4 text-sm text-slate-600 dark:border-slate-600 dark:text-slate-300">
            <UserRound className="mr-1 inline h-4 w-4" />
            Yönetici hesaplari icin tanim, ad / soyad alani opsiyoneldir; tanimi e-posta ile goruntulenir.
          </p>
        )}
      </div>
    </>
  );
}

type Props = { appRole: "admin" | "dietitian" };

export function RoleAccountProfile({ appRole }: Props) {
  const userName = useAuthProfileDisplayName();

  return (
    <SidebarLayout userRole={appRole} userName={userName}>
      <div className="mx-auto max-w-lg px-4 py-8">
        <h1 className="mb-2 text-2xl font-bold text-slate-900 dark:text-slate-100 sm:text-3xl">
          {appRole === "dietitian" ? "Profil & ayarlar" : "Profil"}
        </h1>
        <p className="mb-8 text-sm text-slate-500 dark:text-slate-400">Bilgiler veritabanındaki hesap kaydınızdan yüklenir.</p>
        <AccountProfilePanel appRole={appRole} />
      </div>
    </SidebarLayout>
  );
}
