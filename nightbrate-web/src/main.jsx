import { StrictMode } from 'react' // React'in geliştirme modu uyarıları için sarmalayıcı
import { createRoot } from 'react-dom/client' // React 18+ kök oluşturucu
import './index.css' // Global Tailwind ve tema stilleri
import App from './App.jsx' // Ana uygulama bileşeni (route'lar burada)

createRoot(document.getElementById('root')).render( // HTML'deki #root div'ine React'i bağla
  <StrictMode> {/* Çift render uyarıları — geliştirme ortamı */}
    <App /> {/* Tüm sayfa yönlendirmeleri App içinde */}
  </StrictMode>,
)
