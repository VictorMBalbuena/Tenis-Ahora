import { useNavigate } from "react-router-dom";
import { UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CambiarPasswordCard } from "../components/CambiarPasswordCard";
import { TemaCard } from "../components/TemaCard";
import { NotificacionesCard } from "../components/NotificacionesCard";
import { SesionesCard } from "../components/SesionesCard";

/**
 * Configuración de la cuenta propia.
 *
 * Son cuatro cosas distintas, cada una con su propio guardado: contraseña,
 * apariencia, notificaciones y sesiones. No hay un botón único de guardar
 * todo a propósito, porque cambiar la contraseña y marcar una notificación no
 * se deshacen igual.
 */
export function ConfiguracionPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Configuración</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Tu contraseña, cómo se ve el sistema y qué avisos querés recibir.
          </p>
        </div>
        <Button variant="outline" onClick={() => navigate("/perfil")}>
          <UserRound />
          Mi perfil
        </Button>
      </header>

      <div className="space-y-6">
        <CambiarPasswordCard />
        <TemaCard />
        <NotificacionesCard />
        <SesionesCard />
      </div>
    </div>
  );
}
