import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";

// Auth Bileşenleri (Klasör yapına göre yollar)
import { Login } from "./components/Auth/Login";
import { RegisterClient } from "./components/Auth/RegisterClient";
import { RegisterDietitian } from "./components/Auth/RegisterDietitian";

// Sayfalar
import { AdminDashboard } from "./pages/admin/AdminDashboard";
import { DietitianDashboard } from "./pages/dietitian/DietitianDashboard";
import { ClientHome } from "./pages/client/ClientHome";

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
        <Route path="/dietitian" element={<DietitianDashboard />} />
        <Route path="/client" element={<ClientHome />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;