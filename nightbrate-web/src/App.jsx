import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { Login } from "./components/Auth/Login";
import { RegisterClient } from "./components/Auth/RegisterClient";
import { RegisterDietitian } from "./components/Auth/RegisterDietitian"; // BURASI ÖNEMLİ

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register-client" element={<RegisterClient />} />
        <Route path="/register-dietitian" element={<RegisterDietitian />} />
      </Routes>
    </Router>
  );
}

export default App;