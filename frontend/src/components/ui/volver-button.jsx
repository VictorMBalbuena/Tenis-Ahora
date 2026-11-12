import { ArrowLeft } from "lucide-react";
import { useNavigate } from "react-router-dom";

// Botón "Volver" del encabezado de una pantalla
export function VolverButton({ to }) {
  const navigate = useNavigate();
  return (
    <button
      onClick={() => navigate(to)}
      className="flex items-center gap-2 text-sm font-bold text-foreground hover:text-primary transition-colors"
    >
      <ArrowLeft size={18} /> Volver
    </button>
  );
}
