import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.jsx";
import { iniciarTema } from "./lib/tema";
import { useAuthStore } from "./store/authStore";

// Exponer en desarrollo para pruebas de consola DevTools
if (import.meta.env.DEV) {
  window.authStore = useAuthStore;
}

// Antes del primer render, asi la pagina no aparece un instante en claro
// cuando la persona eligio el tema oscuro.
iniciarTema();

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
