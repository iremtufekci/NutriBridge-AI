import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Activity,
  Ban,
  Calendar,
  Check,
  ChevronDown,
  Filter,
  Loader2,
  Mail,
  Phone,
  Search,
  ShieldAlert,
  Unlock,
} from "lucide-react";
import { SidebarLayout } from "../../components/SidebarLayout";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";
import axios from "axios";

function formatTimeAgoTr(iso: string | null | undefined) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  const diff = Date.now() - d.getTime();
  if (diff < 0) return "Az önce";
  const s = Math.floor(diff / 1000);
  if (s < 60) return "Az önce";
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} dk önce`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h} saat önce`;
  const days = Math.floor(h / 24);
  if (days < 7) return `${days} gün önce`;
  return d.toLocaleDateString("tr-TR");
}

function formatApiError(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const d = e.response?.data;
    const msg =
      d && typeof d === "object" && "message" in d
        ? String((d as { message?: string }).message || "").trim()
        : "";
    if (msg) return msg;
    return e.message || "Bilinmeyen hata";
  }
  if (e instanceof Error) return e.message;
  return "Hata";
}

type Stats = {
  totalUsers: number;
  admins: number;
  dietitians: number;
  clients: number;
  active: number;
  pending: number;
};

type UserRow = {
  id: string;
  displayName: string;
  initial: string;
  email: string;
  phone: string;
  role: string;
  roleKey: string;
  statusKey: string;
  statusLabel: string;
  createdAt: string;
  lastActivityAt?: string | null;
  isSuspended: boolean;
};

type ActivityItem = {
  id: string;
  initial?: string;
  actorDisplayName: string;
  description: string;
  createdAt: string;
};

const ROLE_FILTER_OPTIONS = [
  { value: "all", label: "Tüm Roller" },
  { value: "admin", label: "Yönetici" },
  { value: "dietitian", label: "Diyetisyen" },
  { value: "client", label: "Danışan" },
];

const STATUS_FILTER_OPTIONS = [
  { value: "all", label: "Tüm Durumlar" },
  { value: "active", label: "Aktif" },
  { value: "pending", label: "Beklemede" },
  { value: "suspended", label: "Askıda" },
];

export function AdminUserManagement() {
  const adminName = useAuthProfileDisplayName();
  const [stats, setStats] = useState<Stats | null>(null);
  const [rows, setRows] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [searchDebounced, setSearchDebounced] = useState("");
  const [roleFilter, setRoleFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [showFilters, setShowFilters] = useState(false);

  const [logUser, setLogUser] = useState<UserRow | null>(null);
  const [logItems, setLogItems] = useState<ActivityItem[]>([]);
  const [logLoading, setLogLoading] = useState(false);

  const [suspendUser, setSuspendUser] = useState<UserRow | null>(null);
  const [suspendMsg, setSuspendMsg] = useState("");
  const [suspendLoading, setSuspendLoading] = useState(false);

  useEffect(() => {
    const t = setTimeout(() => setSearchDebounced(search.trim()), 350);
    return () => clearTimeout(t);
  }, [search]);

  const loadStats = useCallback(async () => {
    try {
      const { data } = await api.get<Stats>("/api/Admin/user-management/stats");
      setStats(data);
    } catch (e) {
      console.error(e);
    }
  }, []);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<UserRow[]>("/api/Admin/user-management/users", {
        params: {
          q: searchDebounced || undefined,
          role: roleFilter === "all" ? undefined : roleFilter,
          status: statusFilter === "all" ? undefined : statusFilter,
        },
      });
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [searchDebounced, roleFilter, statusFilter]);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const openLogs = async (u: UserRow) => {
    setLogUser(u);
    setLogItems([]);
    setLogLoading(true);
    try {
      const { data } = await api.get<ActivityItem[]>(
        `/api/Admin/user-management/${encodeURIComponent(u.id)}/activity-logs`,
        { params: { take: 40 } }
      );
      setLogItems(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
    } finally {
      setLogLoading(false);
    }
  };

  const submitSuspend = async () => {
    if (!suspendUser) return;
    setSuspendLoading(true);
    try {
      await api.post(`/api/Admin/user-management/${encodeURIComponent(suspendUser.id)}/suspend`, {
        message: suspendMsg.trim(),
      });
      setSuspendUser(null);
      setSuspendMsg("");
      await loadStats();
      await loadUsers();
    } catch (e) {
      window.alert(formatApiError(e));
    } finally {
      setSuspendLoading(false);
    }
  };

  const doUnsuspend = async (u: UserRow) => {
    if (!window.confirm("Bu kullanıcının askısını kaldırmak istiyor musunuz?")) return;
    try {
      await api.post(`/api/Admin/user-management/${encodeURIComponent(u.id)}/unsuspend`);
      await loadStats();
      await loadUsers();
    } catch (e) {
      window.alert(formatApiError(e));
    }
  };

  const countLabel = useMemo(() => rows.length, [rows.length]);

  const cardBase =
    "rounded-2xl border p-4 shadow-sm transition-colors bg-white border-slate-200/80 dark:bg-[#161B22] dark:border-slate-700/80";

  return (
    <SidebarLayout userRole="admin" userName={adminName}>
      <div className="p-4 sm:p-6 lg:p-8 space-y-6 bg-[#F4F6F8] dark:bg-[#0D1117] min-h-screen text-slate-900 dark:text-white transition-colors pb-24 md:pb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">Kullanıcı Yönetimi</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 text-sm sm:text-base">
            Sistemdeki tüm kullanıcıları görüntüleyin ve yönetin
          </p>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
          {(
            [
              ["Toplam Kullanıcı", stats?.totalUsers ?? "—", "text-slate-900 dark:text-white"],
              ["Admin", stats?.admins ?? "—", "text-amber-600 dark:text-amber-400"],
              ["Diyetisyen", stats?.dietitians ?? "—", "text-emerald-600 dark:text-emerald-400"],
              ["Danışan", stats?.clients ?? "—", "text-emerald-600 dark:text-emerald-400"],
              ["Aktif", stats?.active ?? "—", "text-emerald-600 dark:text-emerald-400"],
              ["Bekleyen", stats?.pending ?? "—", "text-amber-600 dark:text-amber-400"],
            ] as const
          ).map(([t, v, c]) => (
            <div key={t} className={cardBase}>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 font-medium">{t}</p>
              <p className={`text-2xl sm:text-3xl font-bold mt-1 ${c}`}>{v}</p>
            </div>
          ))}
        </div>

        <div
          className={`${cardBase} p-4 sm:p-5 space-y-4`}
        >
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                type="search"
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-[#0D1117] text-slate-900 dark:text-white placeholder:text-slate-400 focus:ring-2 focus:ring-[#2ECC71]/40 focus:border-[#2ECC71] outline-none"
                placeholder="İsim, e-posta veya telefon ile ara..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <button
              type="button"
              onClick={() => setShowFilters((f) => !f)}
              className={`inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-semibold text-white transition-colors shrink-0 ${
                showFilters
                  ? "bg-[#2ECC71] hover:bg-[#27ae60]"
                  : "bg-slate-800 dark:bg-slate-200 dark:text-slate-900 hover:opacity-90"
              }`}
            >
              <Filter className="w-4 h-4" />
              Filtreler
            </button>
          </div>

          {showFilters && (
            <>
              <div className="border-t border-slate-200 dark:border-slate-700 pt-4" />
              <div className="grid sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-bold text-slate-800 dark:text-slate-200 mb-1.5">
                    Kullanıcı Rolü
                  </label>
                  <div className="relative">
                    <select
                      className="w-full appearance-none rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-[#0D1117] py-2.5 pl-3 pr-10 text-sm focus:ring-2 focus:ring-[#2ECC71]/30 outline-none"
                      value={roleFilter}
                      onChange={(e) => setRoleFilter(e.target.value)}
                    >
                      {ROLE_FILTER_OPTIONS.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </select>
                    <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-bold text-slate-800 dark:text-slate-200 mb-1.5">
                    Hesap Durumu
                  </label>
                  <div className="relative">
                    <select
                      className="w-full appearance-none rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-[#0D1117] py-2.5 pl-3 pr-10 text-sm focus:ring-2 focus:ring-[#2ECC71]/30 outline-none"
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                    >
                      {STATUS_FILTER_OPTIONS.map((o) => (
                        <option key={o.value} value={o.value}>
                          {o.label}
                        </option>
                      ))}
                    </select>
                    <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
                  </div>
                </div>
              </div>
            </>
          )}

          <p className="text-sm text-slate-500 dark:text-slate-400">
            {loading ? "Yükleniyor…" : `${countLabel} kullanıcı bulundu`}
          </p>
        </div>

        <div className={`${cardBase} overflow-hidden`}>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50/80 dark:bg-slate-900/50">
                  <th className="p-3 font-bold">Kullanıcı</th>
                  <th className="p-3 font-bold">İletişim</th>
                  <th className="p-3 font-bold">Rol</th>
                  <th className="p-3 font-bold">Durum</th>
                  <th className="p-3 font-bold">Kayıt Tarihi</th>
                  <th className="p-3 font-bold">Son Aktivite</th>
                  <th className="p-3 font-bold text-right">İşlemler</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="p-12 text-center text-slate-500">
                      <Loader2 className="w-8 h-8 animate-spin inline text-[#2ECC71]" />
                    </td>
                  </tr>
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="p-8 text-center text-slate-500">
                      Kullanıcı yok veya eşleşen sonuç yok.
                    </td>
                  </tr>
                ) : (
                  rows.map((u) => (
                    <tr
                      key={u.id}
                      className="border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50/50 dark:hover:bg-slate-900/30"
                    >
                      <td className="p-3">
                        <div className="flex items-center gap-2">
                          <div className="w-9 h-9 rounded-full bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-200 flex items-center justify-center text-sm font-bold shrink-0">
                            {u.initial}
                          </div>
                          <span className="font-medium line-clamp-1">{u.displayName}</span>
                        </div>
                      </td>
                      <td className="p-3 text-slate-600 dark:text-slate-300">
                        <div className="flex items-center gap-1.5 text-xs sm:text-sm">
                          <Mail className="w-3.5 h-3.5 shrink-0" />
                          <span className="break-all">{u.email}</span>
                        </div>
                        {u.phone && u.phone !== "—" && (
                          <div className="flex items-center gap-1.5 mt-1 text-xs sm:text-sm">
                            <Phone className="w-3.5 h-3.5 shrink-0" />
                            {u.phone}
                          </div>
                        )}
                      </td>
                      <td className="p-3">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                            u.roleKey === "admin"
                              ? "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200"
                              : "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-200"
                          }`}
                        >
                          {u.role}
                        </span>
                      </td>
                      <td className="p-3">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                            u.statusKey === "suspended"
                              ? "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200"
                              : u.statusKey === "pending"
                                ? "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-200"
                                : "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-200"
                          }`}
                        >
                          {u.statusKey === "active" && <Check className="w-3 h-3" />}
                          {u.statusLabel}
                        </span>
                      </td>
                      <td className="p-3 text-slate-600 dark:text-slate-300 whitespace-nowrap">
                        <div className="flex items-center gap-1.5 text-xs sm:text-sm">
                          <Calendar className="w-3.5 h-3.5" />
                          {u.createdAt
                            ? new Date(u.createdAt).toLocaleDateString("tr-TR")
                            : "—"}
                        </div>
                      </td>
                      <td className="p-3 text-slate-600 dark:text-slate-300 text-xs sm:text-sm whitespace-nowrap">
                        {formatTimeAgoTr(u.lastActivityAt)}
                      </td>
                      <td className="p-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            type="button"
                            title="Aktivite logları"
                            onClick={() => void openLogs(u)}
                            className="w-9 h-9 rounded-full border border-slate-200 dark:border-slate-600 bg-white dark:bg-[#0D1117] flex items-center justify-center text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800"
                          >
                            <Activity className="w-4 h-4" />
                          </button>
                          {u.isSuspended ? (
                            <button
                              type="button"
                              title="Askıyı kaldır"
                              onClick={() => void doUnsuspend(u)}
                              className="w-9 h-9 rounded-full bg-emerald-600 text-white flex items-center justify-center hover:bg-emerald-700"
                            >
                              <Unlock className="w-4 h-4" />
                            </button>
                          ) : (
                            <button
                              type="button"
                              title="Askıya al / engelle"
                              onClick={() => {
                                setSuspendUser(u);
                                setSuspendMsg("");
                              }}
                              className="w-9 h-9 rounded-full bg-red-600 text-white flex items-center justify-center hover:bg-red-700"
                            >
                              <Ban className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {logUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog">
          <div className="bg-white dark:bg-[#161B22] rounded-2xl max-w-lg w-full max-h-[80vh] overflow-hidden shadow-xl border border-slate-200 dark:border-slate-700">
            <div className="p-4 border-b border-slate-200 dark:border-slate-700 flex items-center justify-between">
              <h2 className="font-bold text-lg">Aktivite — {logUser.displayName}</h2>
              <button
                type="button"
                className="p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800"
                onClick={() => setLogUser(null)}
              >
                Kapat
              </button>
            </div>
            <div className="p-4 overflow-y-auto max-h-[60vh] space-y-3">
              {logLoading ? (
                <div className="text-center py-8 text-slate-500">Yükleniyor…</div>
              ) : logItems.length === 0 ? (
                <p className="text-slate-500 text-sm">Bu kullanıcı için kayıt yok.</p>
              ) : (
                logItems.map((a) => (
                  <div
                    key={a.id}
                    className="flex gap-3 p-3 rounded-xl bg-slate-50 dark:bg-slate-900/50 border border-slate-100 dark:border-slate-800"
                  >
                    <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-xs font-bold shrink-0">
                      {a.initial}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs text-slate-500">
                        {a.actorDisplayName} · {new Date(a.createdAt).toLocaleString("tr-TR")}
                      </p>
                      <p className="text-sm mt-0.5">{a.description}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {suspendUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog">
          <div className="bg-white dark:bg-[#161B22] rounded-2xl max-w-md w-full shadow-xl border border-slate-200 dark:border-slate-700 p-6 space-y-4">
            <div className="flex items-start gap-2">
              <ShieldAlert className="w-6 h-6 text-amber-500 shrink-0 mt-0.5" />
              <div>
                <h2 className="font-bold text-lg">Kullanıcıyı askıya al</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
                  <strong className="text-slate-800 dark:text-slate-200">{suspendUser.email}</strong> giriş
                  yapamayacak. Giriş ekranında gösterilecek metni yazın.
                </p>
              </div>
            </div>
            <textarea
              className="w-full min-h-[120px] rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-[#0D1117] p-3 text-sm focus:ring-2 focus:ring-[#2ECC71]/30 outline-none"
              placeholder="Askıya alma nedeni (kullanıcıya gösterilir)…"
              value={suspendMsg}
              onChange={(e) => setSuspendMsg(e.target.value)}
            />
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                className="px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800"
                onClick={() => setSuspendUser(null)}
                disabled={suspendLoading}
              >
                Vazgeç
              </button>
              <button
                type="button"
                className="px-4 py-2 rounded-xl bg-red-600 text-white font-semibold hover:bg-red-700 disabled:opacity-50"
                disabled={suspendLoading || !suspendMsg.trim()}
                onClick={() => void submitSuspend()}
              >
                {suspendLoading ? "İşleniyor…" : "Askıya al"}
              </button>
            </div>
          </div>
        </div>
      )}
    </SidebarLayout>
  );
}
