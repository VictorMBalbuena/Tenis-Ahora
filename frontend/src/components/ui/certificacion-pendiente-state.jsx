import { Link } from "react-router-dom";
import { ShieldAlert, ArrowRight } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Estado vacío institucional cuando un entrenador aún tiene la certificación deportiva en validación.
 * Reemplaza vistas parciales o banners aislados evitando mostrar métricas vacías o sensaciones de error.
 */
export function CertificacionPendienteState({
  titulo = "Certificación deportiva en proceso de validación",
  descripcion = "Tu cuenta de entrenador fue creada y está a la espera de que la secretaría del club valide tu certificación deportiva habilitante. En cuanto sea aprobada, vas a ver acá tus entrenamientos asignados y podrás tomar asistencia.",
  className,
}) {
  return (
    <div
      role="status"
      className={cn(
        "rounded-2xl border border-amber-500/30 bg-amber-500/5 p-6 sm:p-8 text-center max-w-2xl mx-auto space-y-4 my-6 shadow-xs",
        className,
      )}
    >
      <div className="mx-auto flex size-12 items-center justify-center rounded-xl bg-amber-500/15 text-amber-600 dark:text-amber-400">
        <ShieldAlert className="size-6" />
      </div>

      <div className="space-y-2">
        <h3 className="text-base sm:text-lg font-bold text-foreground tracking-tight">
          {titulo}
        </h3>
        <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed max-w-lg mx-auto">
          {descripcion}
        </p>
      </div>

      <div className="pt-2">
        <Link
          to="/perfil"
          className={cn(
            buttonVariants({ variant: "outline", size: "sm" }),
            "gap-1.5 text-xs font-medium border-amber-500/30 text-amber-950 dark:text-amber-200 hover:bg-amber-500/10",
          )}
        >
          Consultar mi perfil <ArrowRight className="size-3.5" />
        </Link>
      </div>
    </div>
  );
}
