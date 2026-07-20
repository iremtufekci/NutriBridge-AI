import React from "react"; // React kütüphanesi
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"; // URL tabanlı sayfa geçişi

// --- Giriş ve kayıt ekranları ---
import { Login } from "./components/Auth/Login"; // Giriş formu
import { RegisterClient } from "./components/Auth/RegisterClient"; // Danışan kaydı
import { RegisterDietitian } from "./components/Auth/RegisterDietitian"; // Diyetisyen kaydı

// --- Admin sayfaları ---
import { AdminDashboard } from "./pages/admin/AdminDashboard"; // Admin özet paneli
import { AdminApprovals } from "./pages/admin/AdminApprovals"; // Diyetisyen onayları
import { AdminSettings } from "./pages/admin/AdminSettings"; // Admin ayarları
import { AdminSystemAnalytics } from "./pages/admin/AdminSystemAnalytics"; // Sistem istatistikleri
import { AdminUserManagement } from "./pages/admin/AdminUserManagement"; // Kullanıcı yönetimi

// --- Diyetisyen sayfaları ---
import { DietitianDashboard } from "./pages/dietitian/DietitianDashboard"; // Diyetisyen anasayfa
import { DietitianPrograms } from "./pages/dietitian/DietitianPrograms"; // Diyet programı yazma
import { DietitianAiReview } from "./pages/dietitian/DietitianAiReview"; // AI mutfak denetimi
import { DietitianMealAnalysisReview } from "./pages/dietitian/DietitianMealAnalysisReview"; // Yemek fotoğrafı inceleme
import { DietitianCriticalAlerts } from "./pages/dietitian/DietitianCriticalAlerts"; // Kritik uyarılar
import { DietitianTasks } from "./pages/dietitian/DietitianTasks"; // Günlük görevler
import { DietitianClients } from "./pages/dietitian/DietitianClients"; // Danışan listesi

// --- Danışan sayfaları ---
import { ClientHome } from "./pages/client/ClientHome"; // Danışan anasayfa
import { ClientProfile } from "./pages/client/ClientProfile"; // Profil düzenleme
import { ClientDietProgram } from "./pages/client/ClientDietProgram"; // Günlük diyet programı
import { ClientDietProgramHistory } from "./pages/client/ClientDietProgramHistory"; // Geçmiş programlar
import { ClientMealAnalysis } from "./pages/client/ClientMealAnalysis"; // Yemek fotoğrafı analizi
import { ClientPdfAnalysis } from "./pages/client/ClientPdfAnalysis"; // PDF tahlil analizi
import { ClientAiKitchenChef } from "./pages/client/ClientAiKitchenChef"; // AI mutfak şefi
import { ClientAiKitchenShares } from "./pages/client/ClientAiKitchenShares"; // Paylaşılan tarifler

// --- Ortak bileşenler ---
import { RoleAccountProfile } from "./pages/RoleAccountProfile"; // Diyetisyen profil sayfası
import { ThemeBootstrap } from "./components/ThemeBootstrap"; // Tema (açık/koyu) başlatıcı
import { AppFeedbackProvider } from "./components/feedback/AppFeedback"; // Toast/bildirim sağlayıcı

function NotFoundRedirect() { // Bilinmeyen URL'ler için
  return <Navigate to="/login" replace />; // Login'e yönlendir (geçmişi değiştir)
}

function App() { // Uygulamanın kök bileşeni
  return (
    <BrowserRouter> {/* Tarayıcı URL'sini dinler */}
      <AppFeedbackProvider> {/* Alt bileşenlere bildirim gösterme imkânı */}
        <ThemeBootstrap /> {/* Sayfa yüklenince temayı uygular */}
        <Routes> {/* Route listesi başlangıcı */}
        <Route path="/" element={<Login />} /> {/* Kök adres = giriş */}
        <Route path="/login" element={<Login />} /> {/* Giriş sayfası */}
        <Route path="/register-client" element={<RegisterClient />} /> {/* Danışan kayıt */}
        <Route path="/register-dietitian" element={<RegisterDietitian />} /> {/* Diyetisyen kayıt */}
        <Route path="/admin" element={<AdminDashboard />} /> {/* /admin kısayolu */}
        <Route path="/admin/dashboard" element={<AdminDashboard />} /> {/* Admin özet */}
        <Route path="/admin/users" element={<AdminUserManagement />} /> {/* Kullanıcılar */}
        <Route path="/admin/approvals" element={<AdminApprovals />} /> {/* Onay bekleyenler */}
        <Route path="/admin/analytics" element={<AdminSystemAnalytics />} /> {/* Analitik */}
        <Route path="/admin/profile" element={<Navigate to="/admin/settings" replace />} /> {/* Eski profil → ayarlar */}
        <Route path="/admin/settings" element={<AdminSettings />} /> {/* Admin ayarları */}
        <Route path="/dietitian" element={<DietitianDashboard />} /> {/* Diyetisyen kök */}
        <Route path="/dietitian/dashboard" element={<DietitianDashboard />} /> {/* Diyetisyen anasayfa */}
        <Route path="/dietitian/clients/:clientId" element={<DietitianClients />} /> {/* Tek danışan detayı */}
        <Route path="/dietitian/clients" element={<DietitianClients />} /> {/* Danışan listesi */}
        <Route path="/dietitian/programs" element={<DietitianPrograms />} /> {/* Program editörü */}
        <Route path="/dietitian/meal-analysis" element={<DietitianMealAnalysisReview />} /> {/* Öğün foto inceleme */}
        <Route path="/dietitian/ai-review" element={<DietitianAiReview />} /> {/* AI tarif denetimi */}
        <Route path="/dietitian/tasks" element={<DietitianTasks />} /> {/* Günlük görevler */}
        <Route path="/dietitian/alerts" element={<DietitianCriticalAlerts />} /> {/* Kritik uyarılar */}
        <Route path="/dietitian/profile" element={<RoleAccountProfile appRole="dietitian" />} /> {/* Diyetisyen profil */}
        <Route path="/client" element={<ClientHome />} /> {/* Danışan kök */}
        <Route path="/client/home" element={<ClientHome />} /> {/* Danışan anasayfa */}
        <Route path="/client/journal" element={<Navigate to="/client/diet-program" replace />} /> {/* Eski journal → program */}
        <Route path="/client/diet-program" element={<ClientDietProgram />} /> {/* Günlük program */}
        <Route path="/client/diet-program-history" element={<ClientDietProgramHistory />} /> {/* Geçmiş */}
        <Route path="/client/food-scan" element={<ClientMealAnalysis />} /> {/* Yemek tarama */}
        <Route path="/client/pdf-analysis" element={<ClientPdfAnalysis />} /> {/* PDF analizi */}
        <Route path="/client/ai-chef" element={<ClientAiKitchenChef />} /> {/* AI şef */}
        <Route path="/client/ai-chef-shares" element={<ClientAiKitchenShares />} /> {/* Paylaşımlar */}
        <Route path="/client/profile" element={<ClientProfile />} /> {/* Danışan profil */}
        <Route path="*" element={<NotFoundRedirect />} /> {/* Eşleşmeyen tüm URL'ler */}
        </Routes>
      </AppFeedbackProvider>
    </BrowserRouter>
  );
}

export default App; // Diğer dosyaların import edebilmesi için dışa aktar
