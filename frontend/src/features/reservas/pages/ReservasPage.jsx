import { useAuthStore } from "@/store/authStore";
import { MisReservasView } from "../components/MisReservasView";
import { GestionReservasView } from "../components/GestionReservasView";

/**
 * Página principal de reservas contextual por rol (CU17 / CU80).
 * - SOCIO: MisReservasView
 * - EMPLEADO / ADMINISTRADOR: GestionReservasView
 */
export function ReservasPage() {
  const auth = useAuthStore();
  const rol = auth?.user?.rol || auth?.rol;

  if (rol === "SOCIO") {
    return <MisReservasView />;
  }

  if (rol === "EMPLEADO" || rol === "ADMINISTRADOR") {
    return <GestionReservasView />;
  }

  return (
    <div className="max-w-4xl mx-auto p-6 text-center text-muted-foreground">
      No tenés los permisos requeridos para acceder al módulo de reservas.
    </div>
  );
}

export default ReservasPage;
