import { AlertTriangle } from "lucide-react";

// Aviso de error de formulario
export function ErrorBanner({ children, className = "" }) {
  return (
    <div
      className={`bg-red-50 text-red-600 px-6 py-4 rounded-2xl flex items-center gap-3 font-medium shadow-sm border border-red-100 ${className}`}
    >
      <AlertTriangle size={20} /> {children}
    </div>
  );
}
