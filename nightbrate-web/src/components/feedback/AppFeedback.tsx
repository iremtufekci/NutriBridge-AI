// Uygulama genelinde toast bildirimleri ve onay/uyarı diyalogları
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { AlertCircle, AlertTriangle, CheckCircle2, Info, X } from "lucide-react";

// Toast renk ve ikon varyantları
type ToastVariant = "success" | "error" | "info";

type ToastItem = {
  id: string;
  message: string;
  variant: ToastVariant;
};

type DialogVariant = "default" | "danger" | "success";

type AlertOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  variant?: DialogVariant;
};

type ConfirmOptions = {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: DialogVariant;
};

type AppFeedbackContextValue = {
  notify: {
    success: (message: string) => void;
    error: (message: string) => void;
    info: (message: string) => void;
  };
  alert: (options: AlertOptions | string) => Promise<void>;
  confirm: (options: ConfirmOptions | string) => Promise<boolean>;
};

const AppFeedbackContext = createContext<AppFeedbackContextValue | null>(null);

// Metin veya nesne olarak gelen uyarı seçeneklerini standartlaştırır
function normalizeAlert(opts: AlertOptions | string): AlertOptions {
  return typeof opts === "string" ? { message: opts } : opts;
}

// Metin veya nesne olarak gelen onay seçeneklerini standartlaştırır
function normalizeConfirm(opts: ConfirmOptions | string): ConfirmOptions {
  return typeof opts === "string" ? { message: opts } : opts;
}

// Toast kartları için Tailwind sınıf eşlemesi
const toastStyles: Record<ToastVariant, string> = {
  success: "border-emerald-200/90 bg-emerald-50 text-emerald-950 shadow-emerald-100/80",
  error: "border-red-200/90 bg-red-50 text-red-950 shadow-red-100/80",
  info: "border-slate-200/90 bg-white text-slate-800 shadow-slate-200/80",
};

function ToastIcon({ variant }: { variant: ToastVariant }) {
  if (variant === "success") return <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600" />;
  if (variant === "error") return <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />;
  return <Info className="h-5 w-5 shrink-0 text-slate-500" />;
}

function DialogIcon({ variant }: { variant: DialogVariant }) {
  if (variant === "danger") return <AlertTriangle className="h-6 w-6 text-red-600" />;
  if (variant === "success") return <CheckCircle2 className="h-6 w-6 text-emerald-600" />;
  return <Info className="h-6 w-6 text-[#2ECC71]" />;
}

function DialogButtons({
  variant,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
  showCancel,
}: {
  variant: DialogVariant;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
  showCancel: boolean;
}) {
  const confirmClass =
    variant === "danger"
      ? "bg-red-600 hover:bg-red-700 focus:ring-red-400"
      : "bg-[#2ECC71] hover:bg-[#27AE60] focus:ring-[#2ECC71]/40";

  return (
    <div className={`flex flex-col-reverse gap-2 sm:flex-row sm:gap-3 ${showCancel ? "sm:justify-end" : ""}`}>
      {showCancel ? (
        <button
          type="button"
          onClick={onCancel}
          className="rounded-xl border border-slate-200 bg-white px-5 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
        >
          {cancelLabel}
        </button>
      ) : null}
      <button
        type="button"
        onClick={onConfirm}
        className={`rounded-xl px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 ${confirmClass} ${showCancel ? "" : "w-full"}`}
      >
        {confirmLabel}
      </button>
    </div>
  );
}

// Tüm uygulamayı saran geri bildirim sağlayıcısı
export function AppFeedbackProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const [dialog, setDialog] = useState<{
    kind: "alert" | "confirm";
    options: AlertOptions | ConfirmOptions;
    resolve: (value: boolean) => void;
  } | null>(null);

  const toastTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  // Yeni toast ekler; en fazla 5 adet, 4.5 sn sonra otomatik kapanır
  const pushToast = useCallback((message: string, variant: ToastVariant) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    setToasts((prev) => [...prev, { id, message, variant }].slice(-5));
    const t = setTimeout(() => {
      setToasts((prev) => prev.filter((x) => x.id !== id));
      toastTimers.current.delete(id);
    }, 4500);
    toastTimers.current.set(id, t);
  }, []);

  useEffect(() => {
    const timers = toastTimers.current;
    return () => {
      timers.forEach((t) => clearTimeout(t));
      timers.clear();
    };
  }, []);

  const dismissToast = (id: string) => {
    const t = toastTimers.current.get(id);
    if (t) clearTimeout(t);
    toastTimers.current.delete(id);
    setToasts((prev) => prev.filter((x) => x.id !== id));
  };

  // Tek düğmeli bilgi diyaloğu; Promise ile kapanış beklenir
  const alert = useCallback((options: AlertOptions | string) => {
    const opts = normalizeAlert(options);
    return new Promise<void>((resolve) => {
      setDialog({
        kind: "alert",
        options: opts,
        resolve: () => {
          setDialog(null);
          resolve();
        },
      });
    });
  }, []);

  // Onay/vazgeç diyaloğu; true/false döner
  const confirm = useCallback((options: ConfirmOptions | string) => {
    const opts = normalizeConfirm(options);
    return new Promise<boolean>((resolve) => {
      setDialog({
        kind: "confirm",
        options: opts,
        resolve: (value: boolean) => {
          setDialog(null);
          resolve(value);
        },
      });
    });
  }, []);

  useEffect(() => {
    if (!dialog) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (dialog.kind === "confirm") dialog.resolve(false);
        else dialog.resolve(true);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [dialog]);

  const value = useMemo<AppFeedbackContextValue>(
    () => ({
      notify: {
        success: (m) => pushToast(m, "success"),
        error: (m) => pushToast(m, "error"),
        info: (m) => pushToast(m, "info"),
      },
      alert,
      confirm,
    }),
    [alert, confirm, pushToast]
  );

  const dialogOpts = dialog ? (dialog.options as ConfirmOptions & AlertOptions) : null;
  const isConfirm = dialog?.kind === "confirm";
  const variant = (dialogOpts?.variant ?? "default") as DialogVariant;

  // Modal katman: arka plan tıklanınca veya Escape ile kapanır
  const overlay =
    dialog && dialogOpts ? (
      <div
        className="fixed inset-0 z-[200] flex items-end justify-center bg-slate-900/40 p-4 backdrop-blur-[2px] sm:items-center"
        role="presentation"
        onClick={() => dialog.resolve(isConfirm ? false : true)}
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="app-feedback-title"
          className="w-full max-w-md animate-[slideUp_0.22s_ease-out] rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl sm:animate-none"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-50">
              <DialogIcon variant={variant} />
            </div>
            <div className="min-w-0 flex-1">
              <h2 id="app-feedback-title" className="text-lg font-bold text-slate-900">
                {dialogOpts.title ?? (isConfirm ? "Emin misiniz?" : "Bilgi")}
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-slate-600">{dialogOpts.message}</p>
            </div>
          </div>
          <div className="mt-6">
            <DialogButtons
              variant={variant}
              confirmLabel={dialogOpts.confirmLabel ?? (isConfirm ? "Onayla" : "Tamam")}
              cancelLabel={dialogOpts.cancelLabel ?? "Vazgeç"}
              showCancel={isConfirm}
              onConfirm={() => dialog.resolve(true)}
              onCancel={() => dialog.resolve(false)}
            />
          </div>
        </div>
      </div>
    ) : null;

  // Üstte kayan toast listesi (portal ile body'ye)
  const toastLayer = (
    <div
      className="pointer-events-none fixed left-0 right-0 top-0 z-[190] flex flex-col items-center gap-2 px-4 pt-[max(1rem,env(safe-area-inset-top,0px))]"
      aria-live="polite"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`pointer-events-auto flex w-full max-w-md items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg animate-[slideDown_0.25s_ease-out] ${toastStyles[t.variant]}`}
        >
          <ToastIcon variant={t.variant} />
          <p className="min-w-0 flex-1 leading-snug">{t.message}</p>
          <button
            type="button"
            onClick={() => dismissToast(t.id)}
            className="shrink-0 rounded-md p-0.5 opacity-60 transition-opacity hover:opacity-100"
            aria-label="Kapat"
          >
            <X size={16} />
          </button>
        </div>
      ))}
    </div>
  );

  return (
    <AppFeedbackContext.Provider value={value}>
      {children}
      {createPortal(toastLayer, document.body)}
      {overlay ? createPortal(overlay, document.body) : null}
    </AppFeedbackContext.Provider>
  );
}

// Alt bileşenlerde notify / alert / confirm erişimi
export function useAppFeedback(): AppFeedbackContextValue {
  const ctx = useContext(AppFeedbackContext);
  if (!ctx) throw new Error("useAppFeedback AppFeedbackProvider içinde kullanılmalıdır.");
  return ctx;
}
