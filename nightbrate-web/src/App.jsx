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
import { ClientHome } from "./pages/client/ClientHome";
import { ClientProfile } from "./pages/client/ClientProfile";
import { PlaceholderWithLayout } from "./pages/PlaceholderWithLayout";

function NotFoundRedirect() {
  return <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
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
        <Route path="/admin/users" element={<PlaceholderWithLayout />} />
        <Route path="/admin/approvals" element={<AdminApprovals />} />
        <Route path="/admin/analytics" element={<PlaceholderWithLayout />} />
        <Route path="/admin/settings" element={<PlaceholderWithLayout />} />
        <Route path="/dietitian" element={<DietitianDashboard />} />
        <Route path="/dietitian/dashboard" element={<DietitianDashboard />} />
        <Route path="/dietitian/clients" element={<PlaceholderWithLayout />} />
        <Route path="/dietitian/programs" element={<DietitianPrograms />} />
        <Route path="/dietitian/ai-review" element={<PlaceholderWithLayout />} />
        <Route path="/dietitian/alerts" element={<PlaceholderWithLayout />} />
        <Route path="/dietitian/profile" element={<PlaceholderWithLayout />} />
        <Route path="/client" element={<ClientHome />} />
        <Route path="/client/home" element={<ClientHome />} />
        <Route path="/client/journal" element={<PlaceholderWithLayout />} />
        <Route path="/client/food-scan" element={<PlaceholderWithLayout />} />
        <Route path="/client/ai-chef" element={<PlaceholderWithLayout />} />
        <Route path="/client/profile" element={<ClientProfile />} />
        <Route path="*" element={<NotFoundRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;