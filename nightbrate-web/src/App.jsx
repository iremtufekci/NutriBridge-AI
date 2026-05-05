import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

// Auth Bileşenleri (Klasör yapına göre yollar)
import { Login } from "./components/Auth/Login";
import { RegisterClient } from "./components/Auth/RegisterClient";
import { RegisterDietitian } from "./components/Auth/RegisterDietitian";

// Sayfalar
import { AdminDashboard } from "./pages/admin/AdminDashboard";
import { AdminApprovals } from "./pages/admin/AdminApprovals";
import { DietitianDashboard } from "./pages/dietitian/DietitianDashboard";
import { DietitianPrograms } from "./pages/dietitian/DietitianPrograms";
import { DietitianAiReview } from "./pages/dietitian/DietitianAiReview";
import { DietitianCriticalAlerts } from "./pages/dietitian/DietitianCriticalAlerts";
import { DietitianTasks } from "./pages/dietitian/DietitianTasks";
import { ClientHome } from "./pages/client/ClientHome";
import { ClientProfile } from "./pages/client/ClientProfile";
import { ClientDietProgram } from "./pages/client/ClientDietProgram";
import { ClientDietProgramHistory } from "./pages/client/ClientDietProgramHistory";
import { DietitianClients } from "./pages/dietitian/DietitianClients";
import { ClientMealAnalysis } from "./pages/client/ClientMealAnalysis";
import { ClientPdfAnalysis } from "./pages/client/ClientPdfAnalysis";
import { ClientAiKitchenChef } from "./pages/client/ClientAiKitchenChef";
import { ClientAiKitchenShares } from "./pages/client/ClientAiKitchenShares";
import { RoleAccountProfile } from "./pages/RoleAccountProfile";
import { AdminSettings } from "./pages/admin/AdminSettings";
import { AdminSystemAnalytics } from "./pages/admin/AdminSystemAnalytics";
import { AdminUserManagement } from "./pages/admin/AdminUserManagement";
import { ThemeBootstrap } from "./components/ThemeBootstrap";

function NotFoundRedirect() {
  return <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <ThemeBootstrap />
      <Routes>
        {/* Uygulama açıldığında ilk durak Login olsun */}
        <Route path="/" element={<Login />} />
        <Route path="/login" element={<Login />} />
        
        {/* Kayıt Sayfaları */}
        <Route path="/register-client" element={<RegisterClient />} />
        <Route path="/register-dietitian" element={<RegisterDietitian />} />

        {/* Dashboard Sayfaları */}
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/users" element={<AdminUserManagement />} />
        <Route path="/admin/approvals" element={<AdminApprovals />} />
        <Route path="/admin/analytics" element={<AdminSystemAnalytics />} />
        <Route path="/admin/profile" element={<Navigate to="/admin/settings" replace />} />
        <Route path="/admin/settings" element={<AdminSettings />} />
        <Route path="/dietitian" element={<DietitianDashboard />} />
        <Route path="/dietitian/dashboard" element={<DietitianDashboard />} />
        <Route path="/dietitian/clients/:clientId" element={<DietitianClients />} />
        <Route path="/dietitian/clients" element={<DietitianClients />} />
        <Route path="/dietitian/programs" element={<DietitianPrograms />} />
        <Route path="/dietitian/ai-review" element={<DietitianAiReview />} />
        <Route path="/dietitian/tasks" element={<DietitianTasks />} />
        <Route path="/dietitian/alerts" element={<DietitianCriticalAlerts />} />
        <Route path="/dietitian/profile" element={<RoleAccountProfile appRole="dietitian" />} />
        <Route path="/client" element={<ClientHome />} />
        <Route path="/client/home" element={<ClientHome />} />
        <Route path="/client/journal" element={<Navigate to="/client/diet-program" replace />} />
        <Route path="/client/diet-program" element={<ClientDietProgram />} />
        <Route path="/client/diet-program-history" element={<ClientDietProgramHistory />} />
        <Route path="/client/food-scan" element={<ClientMealAnalysis />} />
        <Route path="/client/pdf-analysis" element={<ClientPdfAnalysis />} />
        <Route path="/client/ai-chef" element={<ClientAiKitchenChef />} />
        <Route path="/client/ai-chef-shares" element={<ClientAiKitchenShares />} />
        <Route path="/client/profile" element={<ClientProfile />} />
        <Route path="*" element={<NotFoundRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;