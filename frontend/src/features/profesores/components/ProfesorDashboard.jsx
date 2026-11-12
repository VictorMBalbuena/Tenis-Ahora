import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  GraduationCap,
  ClipboardCheck,
  Eye,
  User,
  ArrowRight,
} from "lucide-react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { CertificacionPendienteState } from "@/components/ui/certificacion-pendiente-state";
import { listarMisClases } from "@/features/clases/api/clasesApi";
import {
  diasTexto,
  horario,
  nombreCancha,
} from "@/features/clases/model/clase";

const DIAS_EN = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];

const DIAS_NOMBRES = {
  SUNDAY: "domingo",
  MONDAY: "lunes",
  TUESDAY: "martes",
  WEDNESDAY: "miércoles",
  THURSDAY: "jueves",
  FRIDAY: "viernes",
  SATURDAY: "sábado",
};

/**
 * Inicio mínimo indispensable para el Profesor en su rutina diaria de clases.
 * Muestra directamente las clases del día, botón para tomar asistencia y accesos directos.
 */
export function ProfesorDashboard({ user }) {
  const [clases, setClases] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [certificacionPendiente, setCertificacionPendiente] = useState(false);

  useEffect(() => {
    let cancelado = false;

    async function cargarDatos() {
      setCargando(true);
      try {
        const res = await listarMisClases({ size: 50 });
        if (!cancelado) {
          const lista = res?.contenido || res || [];
          setClases(Array.isArray(lista) ? lista : []);
        }
      } catch (err) {
        if (cancelado) return;
        if (
          err?.response?.status === 422 ||
          err?.response?.data?.codigo === "PROFESOR_NO_VALIDADO"
        ) {
          setCertificacionPendiente(true);
        }
      } finally {
        if (!cancelado) setCargando(false);
      }
    }

    cargarDatos();
    return () => {
      cancelado = true;
    };
  }, []);

  const nombreUsuario = useMemo(() => {
    if (user?.nombre) return user.nombre;
    if (!user?.email) return "Profesor";
    const parteLocal = user.email.split("@")[0];
    return parteLocal.charAt(0).toUpperCase() + parteLocal.slice(1);
  }, [user]);

  const fechaActual = useMemo(() => {
    const hoy = new Date();
    const texto = hoy.toLocaleDateString("es-AR", {
      weekday: "long",
      day: "numeric",
      month: "long",
    });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }, []);

  const hoyDiaEn = useMemo(() => DIAS_EN[new Date().getDay()], []);
  const hoyDiaNombre = useMemo(
    () => DIAS_NOMBRES[hoyDiaEn] || "hoy",
    [hoyDiaEn],
  );

  const sesionesHoy = useMemo(() => {
    return clases.filter(
      (c) => Array.isArray(c.dias) && c.dias.includes(hoyDiaEn),
    );
  }, [clases, hoyDiaEn]);

  const alumnosHoy = useMemo(() => {
    return sesionesHoy.reduce((sum, s) => sum + (s.cupoOcupado ?? 0), 0);
  }, [sesionesHoy]);

  return (
    <div className="space-y-6 text-left max-w-5xl">
      <h1 className="sr-only">Dashboard de {nombreUsuario}</h1>

      {/* Encabezado limpio y directo */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-2 border-b border-border">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
              Hola, {nombreUsuario}
            </h2>
          </div>
          <p className="text-xs text-muted-foreground mt-0.5">{fechaActual}</p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/clases"
            className={cn(
              buttonVariants({ variant: "outline", size: "sm" }),
              "text-xs gap-1.5 flex-1 sm:flex-initial justify-center",
            )}
          >
            <GraduationCap className="size-3.5" />
            Mis clases
          </Link>
          <Link
            to="/perfil"
            className={cn(
              buttonVariants({ variant: "ghost", size: "sm" }),
              "text-xs gap-1.5 flex-1 sm:flex-initial justify-center",
            )}
          >
            <User className="size-3.5" />
            Perfil
          </Link>
        </div>
      </div>

      {/* Alerta si la certificación está pendiente o contenido normal */}
      {certificacionPendiente ? (
        <CertificacionPendienteState
          titulo="Certificación docente en proceso de validación"
          descripcion="Tu cuenta de profesor fue creada y está a la espera de que la secretaría del club valide tu certificación docente habilitante. En cuanto sea aprobada, vas a ver acá tus clases asignadas y podrás tomar asistencia."
        />
      ) : (
        <>
          {/* 3 Métricas esenciales del día (Mobile-first: fila horizontal en mobile, 3 columnas en desktop) */}
          <Card className="grid grid-cols-1 divide-y divide-border sm:grid-cols-3 sm:divide-y-0 sm:divide-x border-border bg-card shadow-2xs overflow-hidden">
            <div className="flex items-center justify-between p-3.5 sm:block sm:p-4 text-left">
              <p className="text-xs font-medium text-muted-foreground">
                Clases hoy
              </p>
              <p className="text-2xl font-bold font-mono tabular-nums text-foreground sm:mt-1">
                {sesionesHoy.length}
              </p>
            </div>

            <div className="flex items-center justify-between p-3.5 sm:block sm:p-4 text-left">
              <p className="text-xs font-medium text-muted-foreground">
                Alumnos hoy
              </p>
              <p className="text-2xl font-bold font-mono tabular-nums text-foreground sm:mt-1">
                {alumnosHoy}
              </p>
            </div>

            <div className="flex items-center justify-between p-3.5 sm:block sm:p-4 text-left">
              <p className="text-xs font-medium text-muted-foreground">
                Mis clases
              </p>
              <p className="text-2xl font-bold font-mono tabular-nums text-foreground sm:mt-1">
                {clases.length}
              </p>
            </div>
          </Card>

          {/* Agenda del día: foco central de la rutina diaria */}
          <section aria-label="Agenda de hoy" className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Clases de hoy ({hoyDiaNombre})
              </h3>
              {sesionesHoy.length > 0 && (
                <span className="text-xs text-muted-foreground">
                  {sesionesHoy.length}{" "}
                  {sesionesHoy.length === 1 ? "sesión" : "sesiones"}
                </span>
              )}
            </div>

            {cargando ? (
              <p className="rounded-xl border border-dashed border-border py-8 text-center text-xs text-muted-foreground">
                Cargando clases de hoy...
              </p>
            ) : sesionesHoy.length === 0 ? (
              <div className="rounded-xl border border-dashed border-border bg-card/40 p-6 text-center space-y-2">
                <p className="text-sm font-medium text-foreground">
                  Hoy no tenés clases programadas.
                </p>
                <p className="text-xs text-muted-foreground">
                  Consultá tus clases en otros días de la semana desde tu listado
                  completo.
                </p>
                <Link
                  to="/clases"
                  className={cn(
                    buttonVariants({ variant: "outline", size: "sm" }),
                    "gap-1 mt-1",
                  )}
                >
                  Ver todas mis clases <ArrowRight className="size-3" />
                </Link>
              </div>
            ) : (
              <div className="space-y-2.5">
                {sesionesHoy.map((sesion) => (
                  <div
                    key={sesion.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between gap-3.5 rounded-xl border border-border bg-card p-4 shadow-2xs hover:border-foreground/30 transition-colors"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="flex h-10 w-14 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary font-mono text-xs font-bold">
                        {String(sesion.horaInicio || "—").slice(0, 5)}
                      </div>
                      <div className="min-w-0">
                        <h4 className="text-sm font-semibold text-foreground truncate">
                          {sesion.nombre}
                        </h4>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          {nombreCancha(sesion)} - {horario(sesion)} -{" "}
                          {sesion.cupoOcupado ?? 0} alumnos
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 w-full sm:w-auto pt-2 sm:pt-0 border-t border-border/40 sm:border-t-0 justify-end sm:justify-start">
                      <Link
                        to={`/clases/${sesion.id}`}
                        state={{ from: "/dashboard" }}
                        className={cn(
                          buttonVariants({ variant: "ghost", size: "default" }),
                          "h-10 text-xs sm:text-sm gap-1.5 flex-1 sm:flex-initial justify-center touch-manipulation",
                        )}
                      >
                        <Eye className="size-4" />
                        Detalle
                      </Link>
                      <Link
                        to={`/clases/${sesion.id}/asistencia`}
                        state={{ from: "/dashboard" }}
                        className={cn(
                          buttonVariants({ size: "default" }),
                          "h-10 text-xs sm:text-sm gap-2 font-medium flex-1 sm:flex-initial justify-center shadow-xs touch-manipulation",
                        )}
                      >
                        <ClipboardCheck className="size-4" />
                        Tomar asistencia
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Otras clases asignadas en la semana */}
          {clases.length > 0 && (
            <section
              aria-label="Todas mis clases"
              className="space-y-3 pt-2"
            >
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Mis Clases semanales
                </h3>
                <Link
                  to="/clases"
                  className="hidden sm:inline-flex text-xs text-muted-foreground hover:text-foreground transition-colors font-medium"
                >
                  Ver todas ({clases.length}) →
                </Link>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                {clases.slice(0, 4).map((c) => (
                  <div
                    key={c.id}
                    className="group flex flex-col justify-between rounded-xl border border-border bg-card p-3.5 sm:p-4 shadow-2xs transition-colors hover:border-foreground/30 gap-3"
                  >
                    <div className="space-y-1.5">
                      <div className="flex items-start justify-between gap-2 min-w-0">
                        <Link
                          to={`/clases/${c.id}`}
                          state={{ from: "/dashboard" }}
                          className="text-sm font-semibold text-foreground hover:underline truncate block flex-1 min-w-0"
                        >
                          {c.nombre}
                        </Link>
                        <Badge
                          variant="outline"
                          className="text-[11px] font-mono tabular-nums shrink-0 font-normal self-start"
                        >
                          {c.cupoOcupado ?? 0}/{c.cupoMaximo} alumnos
                        </Badge>
                      </div>
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
                        <span>{diasTexto(c)}</span>
                        <span>-</span>
                        <span>{horario(c)}</span>
                        <span>-</span>
                        <span>{nombreCancha(c)}</span>
                      </div>
                    </div>

                    <div>
                      <Link
                        to={`/clases/${c.id}/asistencia`}
                        state={{ from: "/dashboard" }}
                        className={cn(
                          buttonVariants({ variant: "outline", size: "default" }),
                          "h-10 w-full justify-center gap-2 text-xs sm:text-sm font-medium hover:bg-primary hover:text-primary-foreground transition-colors touch-manipulation",
                        )}
                      >
                        <ClipboardCheck className="size-4" />
                        Tomar asistencia
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}
