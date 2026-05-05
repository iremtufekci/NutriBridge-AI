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
        setErr("Profil yüklenemedi. Oturum açık mı kontrol edin.");
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

  const initials =
    name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() || "?";

  return (
    <>
      {err && (
        <p className="mb-4 rounded-xl border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-600">
          {err}
        </p>
      )}

      <div className="overflow-hidden rounded-3xl border border-slate-200/90 bg-white shadow-[0_8px_30px_rgba(15,23,42,0.06)]">
        <div className="bg-gradient-to-br from-emerald-50/80 via-white to-slate-50/50 px-5 pb-5 pt-6 sm:px-6 sm:pb-6 sm:pt-7">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-[#2ECC71] text-xl font-bold text-white shadow-md shadow-[#2ECC71]/25 ring-4 ring-white/60 sm:h-[72px] sm:w-[72px] sm:text-2xl">
              {initials}
            </div>
            <div className="min-w-0">
              <p className="truncate text-lg font-bold text-slate-900 sm:text-xl">
                {name || "—"}
              </p>
              <span className="mt-1.5 inline-flex rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
                {appRole === "dietitian" ? "Diyetisyen" : "Yönetici"}
              </span>
            </div>
          </div>
        </div>

        <div className="space-y-3 border-t border-slate-100 bg-white px-5 py-4 sm:px-6 sm:py-5">
        <div className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white shadow-sm">
            <Mail className="h-5 w-5 text-[#2ECC71]" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
              E-posta
            </p>
            <p className="mt-0.5 truncate text-sm font-medium text-slate-900 sm:text-base">
              {profile.email || "—"}
            </p>
          </div>
        </div>
        {appRole === "dietitian" && (
          <>
            <div className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white shadow-sm">
                <Building2 className="h-5 w-5 text-[#2ECC71]" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                  Klinik / çalışma alanı
                </p>
                <p className="mt-0.5 text-sm font-medium text-slate-900">{profile.clinicName || "—"}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white shadow-sm">
                <Award className="h-5 w-5 text-[#2ECC71]" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Diploma no</p>
                <p className="mt-0.5 text-sm font-medium text-slate-900">{profile.diplomaNo || "—"}</p>
              </div>
            </div>
            <div className="rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3.5">
              <div className="flex items-start gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white shadow-sm">
                  <KeyRound className="h-5 w-5 text-[#2ECC71]" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    Takip kodu (danışan eşleşmesi)
                  </p>
                  {profile.connectionCode && profile.connectionCode.length > 0 ? (
                    <>
                      <p className="mt-1 font-mono text-xl font-bold tracking-[0.2em] text-slate-900 sm:text-2xl">
                        {profile.connectionCode}
                      </p>
                      <p className="mt-2 text-xs leading-relaxed text-slate-500">
                        Danışanlar bu 6 haneli kodu uygulamada girerek size bağlanır.
                      </p>
                    </>
                  ) : (
                    <p className="mt-1 text-sm text-amber-700">
                      Kod henüz yok. Yönetici onayı sonrası otomatik atanır; onaylıysanız sayfayı yenileyin.
                    </p>
                  )}
                </div>
              </div>
            </div>
          </>
        )}
        {appRole === "admin" && (
          <div className="flex gap-3 rounded-2xl border border-slate-200/80 bg-slate-50/90 px-4 py-3.5">
            <UserRound className="mt-0.5 h-5 w-5 shrink-0 text-[#2ECC71]" />
            <p className="text-sm leading-relaxed text-slate-600">
              Ad ve soyad, veritabanındaki yönetici hesabı kaydınızdan okunur (sol menü de aynı bilgiyi kullanır).
            </p>
          </div>
        )}
        </div>
      </div>
    </>
  );
}

type Props = { appRole: "admin" | "dietitian" };

export function RoleAccountProfile({ appRole }: Props) {
  const userName = useAuthProfileDisplayName();

  return (
    <SidebarLayout userRole={appRole} userName={userName}>
      <div className="mx-auto max-w-lg px-4 py-8 pb-24 lg:pb-8">
        <h1 className="mb-2 text-2xl font-bold text-slate-900 sm:text-3xl">
          {appRole === "dietitian" ? "Profil & ayarlar" : "Profil"}
        </h1>
        <p className="mb-8 text-sm text-slate-500">Bilgiler veritabanındaki hesap kaydınızdan yüklenir.</p>
        <AccountProfilePanel appRole={appRole} />
      </div>
    </SidebarLayout>
  );
}
