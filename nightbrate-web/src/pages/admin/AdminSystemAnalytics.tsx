import { useCallback, useEffect, useState, type ReactNode } from "react";
import {
  Activity,
  Check,
  Database,
  Globe,
  HardDrive,
  Loader2,
  Server,
  Shield,
  Sparkles,
  Terminal,
  XCircle,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { SidebarLayout } from "../../components/SidebarLayout";
import axios from "axios";
import { api } from "../../api/http";
import { useAuthProfileDisplayName } from "../../hooks/useAuthProfileDisplayName";

function formatApiError(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const s = e.response?.status;
    const d = e.response?.data;
    const msgFromBody =
      d && typeof d === "object" && "message" in d
        ? String((d as { message?: string }).message || "").trim()
        : typeof d === "string"
          ? d
          : "";
    if (s === 401) return "Oturum süresi doldu veya giriş yok. Yönetici olarak tekrar giriş yapın.";
    if (s === 403) return "Yetki yok: bu bölüm yalnızca yönetici rolü içindir. Çıkış yapıp yönetici hesabıyla deneyin.";
    if (s === 404) return "Uç nokta bulunamadı (404). Sunucuyu yeniden başlatıp güncel sürümü çalıştırdığınızdan emin olun.";
    if (msgFromBody) return msgFromBody;
    if (s) return `Durum kodu: ${s}`;
    if (e.message === "Network Error") {
      return "Ağ hatası: sunucuya ulaşılamıyor. Sunucunun çalıştığını (ör. 5231) ve geliştirme ortamında yönlendirmenin açık olduğunu kontrol edin.";
    }
    return e.message || "Bilinmeyen hata";
  }
  if (e instanceof Error) return e.message;
  return "Veri alınamadı";
}

type Kpis = {
  apiRequestsPerHour: number;
  apiRequestsPerHourDeltaPercent: number;
  avgQueryTimeMs: number;
  avgQueryTimeDeltaPercent: number;
  securityScore: number;
  securityOpenIssues: number;
  cacheHitRatioPercent: number;
  cacheStatusLabel: string;
};

type EndpointRow = {
  endpoint: string;
  calls: number;
  avgTimeMs: number;
  errors: number;
  successRatePercent: number;
};

type HRow = { hour: number; label: string; reads: number; writes: number; slowQueries: number };
type CRow = { hour: number; label: string; hits: number; misses: number };
type NRow = { hour: number; label: string; incomingMbps: number; outgoingMbps: number };
type Res = {
  cpuPercent: number;
  memoryPercent: number;
  memoryRefLabel: string;
  diskIoPercent: number;
  diskRefLabel: string;
  networkMbps: number;
  networkUp: number;
  networkDown: number;
  networkNote: string;
};

type ErrorLog = {
  statusCode: number;
  time: string;
  endpoint: string;
  message: string;
  count: number;
};

type SecEv = { severity: string; time: string; name: string; obfuscatedSource: string; countLabel: string; tone: string };

type Payload = {
  kpis: Kpis;
  endpointPerformance: EndpointRow[];
  databaseHourly: HRow[];
  cacheHourly: CRow[];
  networkHourly: NRow[];
  systemResources: Res;
  errorLogs: ErrorLog[];
  securityEvents: SecEv[];
  dataWindowHours: number;
  generatedAtUtc: string;
  dataNote: string;
};

function timeColor(ms: number) {
  if (ms < 200) return "text-emerald-600";
  if (ms < 1000) return "text-amber-600";
  return "text-rose-600";
}

function errToneClass(t: string) {
  if (t === "high") return "border-l-4 border-rose-500 bg-rose-50/50";
  if (t === "medium") return "border-l-4 border-amber-500 bg-amber-50/50";
  return "border-l-4 border-teal-500 bg-teal-50/30";
}

export function AdminSystemAnalytics() {
  const name = useAuthProfileDisplayName();
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [data, setData] = useState<Payload | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setErr(null);
      const { data: d } = await api.get<Payload>("/api/admin/system-analytics");
      setData(d);
    } catch (e) {
      setErr(formatApiError(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const t = setInterval(() => void load(), 20_000);
    return () => clearInterval(t);
  }, [load]);

  if (loading && !data) {
    return (
      <SidebarLayout userRole="admin" userName={name}>
        <div className="min-h-full flex items-center justify-center p-8 text-slate-500 gap-2">
          <Loader2 className="w-5 h-5 animate-spin" /> Sistem metrikleri yükleniyor…
        </div>
      </SidebarLayout>
    );
  }

  if (err && !data) {
    return (
      <SidebarLayout userRole="admin" userName={name}>
        <div className="p-6 max-w-lg space-y-3">
          <p className="text-rose-600 text-sm leading-relaxed">{err}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="text-sm font-semibold text-emerald-600 hover:underline"
          >
            Tekrar dene
          </button>
        </div>
      </SidebarLayout>
    );
  }

  const k = data!.kpis;
  const r = data!.systemResources;

  return (
    <SidebarLayout userRole="admin" userName={name}>
      <div className="min-h-full bg-slate-50 px-3 sm:px-6 py-4 sm:py-6 pb-24 lg:pb-8">
        <div className="mx-auto max-w-7xl space-y-5 sm:space-y-6">
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">Sistem Analitiği</h1>
            <p className="text-slate-500 text-sm mt-0.5">
              Teknik metrikler, sunucu performansı ve güvenlik (son {data?.dataWindowHours ?? 24} saat, canlı)
            </p>
            {data?.dataNote && <p className="text-xs text-slate-500 mt-1">{data.dataNote}</p>}
            {data?.generatedAtUtc && (
              <p className="text-xs text-slate-400 mt-0.5">Son üretim: {new Date(data.generatedAtUtc).toLocaleString("tr-TR")}</p>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
            <Kpi
              label="İstek sayısı / saat"
              value={k.apiRequestsPerHour.toLocaleString("tr-TR")}
              sub={`${k.apiRequestsPerHourDeltaPercent >= 0 ? "↗" : "↘"} ${k.apiRequestsPerHourDeltaPercent.toFixed(1)}%`}
              subClass="text-emerald-600"
              icon={Server}
              iconWrap="bg-emerald-100"
              iconColor="text-emerald-600"
            />
            <Kpi
              label="Ortalama yanıt (ms)"
              value={`${k.avgQueryTimeMs}ms`}
              sub={`${k.avgQueryTimeDeltaPercent >= 0 ? "↗" : "↘"} ${Math.abs(k.avgQueryTimeDeltaPercent).toFixed(0)}%`}
              subClass={k.avgQueryTimeDeltaPercent <= 0 ? "text-emerald-600" : "text-amber-600"}
              icon={Database}
              iconWrap="bg-teal-100"
              iconColor="text-teal-600"
            />
            <Kpi
              label="Güvenlik skoru"
              value={`${k.securityScore.toFixed(2)}%`}
              sub={k.securityOpenIssues > 0 ? `⚠ ${k.securityOpenIssues}` : "Sorun yok"}
              subClass={k.securityOpenIssues > 0 ? "text-rose-600" : "text-slate-500"}
              icon={Shield}
              iconWrap="bg-amber-100"
              iconColor="text-amber-600"
            />
            <Kpi
              label="Başarılı yanıt oranı (ön uç)"
              value={`${k.cacheHitRatioPercent.toFixed(1)}%`}
              sub={
                <span className="inline-flex items-center gap-1 text-emerald-600">
                  <Check className="w-3.5 h-3.5" /> {k.cacheStatusLabel}
                </span>
              }
              icon={HardDrive}
              iconWrap="bg-emerald-100"
              iconColor="text-emerald-600"
            />
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <div className="p-1.5 rounded-lg bg-emerald-100">
                <Terminal className="w-4 h-4 text-emerald-600" />
              </div>
              <div>
                <h2 className="font-bold text-slate-900">Uç nokta performansı</h2>
                <p className="text-xs text-slate-500">Son {data?.dataWindowHours} saat</p>
              </div>
            </div>
            <div className="overflow-x-auto -mx-1">
              <table className="w-full min-w-[640px] text-sm">
                <thead>
                  <tr className="text-left text-slate-500 border-b border-slate-200">
                    <th className="py-2 pr-2 font-medium">Uç nokta</th>
                    <th className="py-2 pr-2 font-medium">Çağrı</th>
                    <th className="py-2 pr-2 font-medium">Ortalama süre</th>
                    <th className="py-2 pr-2 font-medium">Hata</th>
                    <th className="py-2 font-medium">Başarılı oran (2xx)</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.endpointPerformance.map((row) => (
                    <tr key={row.endpoint} className="border-b border-slate-100">
                      <td className="py-2.5 pr-2">
                        <code className="text-xs bg-slate-100 px-2 py-0.5 rounded-md">{row.endpoint}</code>
                      </td>
                      <td className="py-2.5 pr-2 tabular-nums">{row.calls.toLocaleString("tr-TR")}</td>
                      <td className={`py-2.5 pr-2 font-medium ${timeColor(row.avgTimeMs)}`}>{row.avgTimeMs}ms</td>
                      <td className="py-2.5 pr-2 text-rose-600 tabular-nums">{row.errors}</td>
                      <td className="py-2.5 text-emerald-600 tabular-nums">{row.successRatePercent.toFixed(1)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <CardChart title="Veritabanı trafiği: okuma ve yazma" subtitle="Sorgu türü dağılımı" icon={Database}>
              <div className="h-64 w-full min-w-0">
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={data?.databaseHourly} margin={{ top: 6, right: 6, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-slate-200" />
                    <XAxis dataKey="label" tick={{ fontSize: 10 }} className="text-slate-500" />
                    <YAxis tick={{ fontSize: 10 }} className="text-slate-500" />
                    <Tooltip contentStyle={{ fontSize: 12 }} labelStyle={{ color: "#64748b" }} />
                    <Legend />
                    <Bar dataKey="reads" name="Okuma (GET)" fill="#14B8A6" radius={[3, 3, 0, 0]} />
                    <Bar dataKey="writes" name="Yazma / diğer" fill="#0D9488" radius={[3, 3, 0, 0]} />
                    <Line type="monotone" dataKey="slowQueries" name={"Yavaş (> 500 ms)"} stroke="#F97316" dot={false} />
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
            </CardChart>
            <CardChart title="Yanıt dağılımı" subtitle="Başarılı (2xx) ve hata" icon={Activity}>
              <div className="h-64 w-full min-w-0">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={data?.cacheHourly} margin={{ top: 6, right: 6, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-slate-200" />
                    <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} />
                    <Tooltip contentStyle={{ fontSize: 12 }} />
                    <Legend />
                    <Bar dataKey="hits" name="Başarılı (2xx)" fill="#22C55E" radius={[2, 2, 0, 0]} />
                    <Bar dataKey="misses" name="Hata / diğer" fill="#EF4444" radius={[2, 2, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardChart>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
            <div className="flex items-center justify-between gap-2 mb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-emerald-100">
                  <Globe className="w-4 h-4 text-emerald-600" />
                </div>
                <div>
                  <h2 className="font-bold text-slate-900">Tahmini ağ yükü</h2>
                  <p className="text-xs text-slate-500">İstek oranına göre (MB/s)</p>
                </div>
              </div>
            </div>
            <div className="h-64 w-full min-w-0">
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={data?.networkHourly} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-slate-200" />
                  <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                  <YAxis tick={{ fontSize: 10 }} />
                  <Tooltip contentStyle={{ fontSize: 12 }} />
                  <Legend />
                  <Line type="monotone" dataKey="incomingMbps" name="Gelen" stroke="#14B8A6" strokeWidth={2} dot />
                  <Line type="monotone" dataKey="outgoingMbps" name="Giden" stroke="#0EA5E9" strokeWidth={2} dot />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <div className="p-1.5 rounded-lg bg-emerald-100">
                <Sparkles className="w-4 h-4 text-emerald-500" />
              </div>
              <div>
                <h2 className="font-bold text-slate-900">Sistem kaynakları (anlık)</h2>
                <p className="text-xs text-slate-500">Sunucu işlemi (işlemci, bellek) ve disk</p>
              </div>
            </div>
            <p className="text-xs text-slate-500 mb-3">{r.networkNote}</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
              <ResourceTile label="İşlemci" value={`${r.cpuPercent}%`} refText="/ 100%" barClass="bg-emerald-500" pct={r.cpuPercent} showBar />
              <ResourceTile
                label="Bellek"
                value={`${r.memoryPercent}%`}
                refText={r.memoryRefLabel}
                barClass="bg-amber-500"
                pct={r.memoryPercent}
                showBar
              />
              <ResourceTile
                label="Disk doluluk"
                value={`${r.diskIoPercent}%`}
                refText={r.diskRefLabel}
                barClass="bg-teal-500"
                pct={r.diskIoPercent}
                showBar
              />
              <div className="rounded-xl border border-slate-200 p-3 bg-slate-50/80">
                <p className="text-xs text-slate-500">Tahmini ağ (5 dk)</p>
                <p className="text-xl font-extrabold text-emerald-600 mt-0.5">{r.networkMbps} MB/s</p>
                <p className="text-xs text-slate-500 mt-1">
                  ↑ {r.networkUp} ↓ {r.networkDown}
                </p>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <XCircle className="w-5 h-5 text-rose-500" />
                <div>
                  <h2 className="font-bold text-slate-900">Hata logları</h2>
                  <p className="text-xs text-slate-500">4xx/5xx (gruplanmış)</p>
                </div>
              </div>
              <ul className="space-y-2 max-h-80 overflow-y-auto pr-1">
                {data?.errorLogs.length ? (
                  data.errorLogs.map((e, i) => (
                    <li key={i} className="rounded-xl border border-slate-200 p-3 text-sm">
                      <div className="flex flex-wrap items-center gap-2 text-xs">
                        <span className="font-mono text-rose-600 bg-rose-100 px-1.5 py-0.5 rounded">
                          {e.statusCode}
                        </span>
                        <span className="text-slate-500">{e.time}</span>
                        {e.count > 1 && (
                          <span className="ml-auto text-rose-600 bg-rose-50 px-1.5 rounded">x{e.count}</span>
                        )}
                      </div>
                      <code className="block text-xs text-slate-600 mt-1 font-mono">{e.endpoint}</code>
                      <p className="text-slate-500 text-xs mt-0.5">{e.message}</p>
                    </li>
                  ))
                ) : (
                  <p className="text-sm text-slate-500">Son dönemde 4xx/5xx kaydı yok.</p>
                )}
              </ul>
            </div>
            <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <Shield className="w-5 h-5 text-amber-500" />
                <div>
                  <h2 className="font-bold text-slate-900">Güvenlik olayları</h2>
                  <p className="text-xs text-slate-500">401 / 403 / 429</p>
                </div>
              </div>
              <ul className="space-y-2 max-h-80 overflow-y-auto pr-1">
                {data?.securityEvents.length ? (
                  data.securityEvents.map((e, i) => (
                    <li key={i} className={`rounded-xl p-3 text-sm ${errToneClass(e.tone)}`}>
                      <div className="flex items-start justify-between gap-2">
                        <div>
                          <p className="text-xs text-slate-500">{e.severity}</p>
                          <p className="font-semibold text-slate-900 mt-0.5">{e.name}</p>
                        </div>
                        <span className="text-xs text-slate-500 shrink-0">{e.time}</span>
                      </div>
                      <p className="text-xs text-slate-600 font-mono mt-1">{e.obfuscatedSource}</p>
                      <p className="text-xs text-slate-500 text-right mt-1">{e.countLabel}</p>
                    </li>
                  ))
                ) : (
                  <p className="text-sm text-slate-500">Güvenlik sınıfı olay kaydı yok.</p>
                )}
              </ul>
            </div>
          </div>
        </div>
      </div>
    </SidebarLayout>
  );
}

function Kpi({
  label,
  value,
  sub,
  subClass,
  icon: Icon,
  iconWrap,
  iconColor,
}: {
  label: string;
  value: string;
  sub: ReactNode;
  subClass?: string;
  icon: typeof Server;
  iconWrap: string;
  iconColor: string;
}) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-4 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs text-slate-500 font-medium uppercase tracking-wide">{label}</p>
          <p className="text-2xl font-bold text-slate-900 mt-1 tabular-nums">{value}</p>
          <div className={`text-xs mt-0.5 ${subClass || "text-slate-500"}`}>{sub}</div>
        </div>
        <div className={`p-2 rounded-xl ${iconWrap}`}>
          <Icon className={`w-5 h-5 ${iconColor}`} />
        </div>
      </div>
    </div>
  );
}

function CardChart({
  title,
  subtitle,
  icon: Icon,
  children,
}: {
  title: string;
  subtitle: string;
  icon: typeof Database;
  children: ReactNode;
}) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-4 sm:p-5 shadow-sm">
      <div className="flex items-center justify-between gap-2 mb-2">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-emerald-100">
            <Icon className="w-4 h-4 text-emerald-600" />
          </div>
          <div>
            <h2 className="font-bold text-slate-900 text-sm sm:text-base">{title}</h2>
            <p className="text-xs text-slate-500">{subtitle}</p>
          </div>
        </div>
      </div>
      {children}
    </div>
  );
}

function ResourceTile({
  label,
  value,
  refText,
  barClass,
  pct,
  showBar,
}: {
  label: string;
  value: string;
  refText: string;
  barClass: string;
  pct: number;
  showBar: boolean;
}) {
  return (
    <div className="rounded-xl border border-slate-200 p-3 bg-slate-50/80">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="text-xl font-extrabold text-slate-900 mt-0.5">
        {value} <span className="text-sm font-normal text-slate-400">/ {refText}</span>
      </p>
      {showBar && (
        <div className="h-2 rounded-full bg-slate-200 overflow-hidden mt-2">
          <div className={`h-full ${barClass} rounded-full transition-all`} style={{ width: `${Math.min(100, pct)}%` }} />
        </div>
      )}
    </div>
  );
}
