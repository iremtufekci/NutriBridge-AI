// Paylaşılan UI bileşenleri — dashboard istatistik kartları vb.

// İstatistik kartı: başlık, değer, ikon ve isteğe bağlı trend gösterir
export function StatCard({ title, value, icon: Icon, iconColor, trend }: any) {
  return (
    <div className="bg-card p-6 rounded-xl border border-border shadow-sm">
      <div className="flex justify-between items-start">
        <div>
          {/* Kart başlığı */}
          <p className="text-sm text-muted-foreground">{title}</p>
          {/* Ana sayısal değer */}
          <h3 className="text-2xl font-bold mt-1">{value}</h3>
          {trend && (
            /* Geçen aya göre değişim metni */
            <p className={`text-xs mt-1 ${trend.isPositive ? 'text-primary' : 'text-destructive'}`}>
              {trend.value} <span className="text-muted-foreground">geçen aya göre</span>
            </p>
          )}
        </div>
        {/* Sağ üst ikon alanı */}
        <div className={`p-3 rounded-lg bg-muted ${iconColor}`}>
          <Icon size={24} />
        </div>
      </div>
    </div>
  );
}
