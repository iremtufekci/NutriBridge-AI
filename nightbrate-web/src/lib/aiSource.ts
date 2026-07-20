/** Gerçek AI sağlayıcısı (Groq); mock değil. */
export function isRealAiSource(source?: string | null): boolean {
  return (source ?? "").toLowerCase() === "groq";
}

/** Groq'a ağ/DNS hatası nedeniyle ulaşılamadığında dönen yedek kaynak. */
export function isMockNetworkSource(source?: string | null): boolean {
  return (source ?? "").toLowerCase() === "mock_network";
}
