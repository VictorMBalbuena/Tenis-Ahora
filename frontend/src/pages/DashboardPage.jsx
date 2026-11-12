import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";
import {
  Calendar,
  LayoutGrid,
  Users,
  ArrowUpRight,
} from "lucide-react";
import { useAuthStore } from "@/store/authStore";
import axiosClient from "@/config/axiosClient";
import { DashboardSocio } from "./DashboardSocio";
import { KpiCard } from "@/components/ui/kpi-card";
import { EntrenadorDashboard } from "@/features/entrenadores/components/EntrenadorDashboard";
import { ProfesorDashboard } from "@/features/profesores/components/ProfesorDashboard";

// Las canchas salen siempre del backend. No hay lista de respaldo escrita a
// mano: mostrar canchas que no existen es peor que no mostrar ninguna.

const ACCESOS_PERSONAL = [
  { to: "/reservas", etiqueta: "Ir a Gestión de Reservas", titulo: "Reservas", detalle: "Agendar y consultar turnos", Icono: Calendar },
  { to: "/canchas", etiqueta: "Ir a Control de Canchas", titulo: "Canchas", detalle: "Superficies e iluminación", Icono: LayoutGrid },
  { to: "/usuarios", etiqueta: "Ir a Directorio de Usuarios", titulo: "Usuarios", detalle: "Socios y personal", Icono: Users },
];

function DashboardPersonal() {
  const user = useAuthStore((state) => state.user);

  // Flujo específico del Entrenador (CU84): Dashboard con sus entrenamientos, agenda y alumnos
  if (user?.rol === "ENTRENADOR") {
    return <EntrenadorDashboard user={user} />;
  }

  // Flujo específico del Profesor (CU81/CU82): Dashboard con sus clases, agenda y asistencia
  if (user?.rol === "PROFESOR") {
    return <ProfesorDashboard user={user} />;
  }

  const esAdmin = user?.rol === "ADMINISTRADOR";
  const esPersonal = esAdmin || user?.rol === "EMPLEADO";

  const [canchas, setCanchas] = useState([]);
  const [canchasSinCargar, setCanchasSinCargar] = useState(false);
  const [turnosHoy, setTurnosHoy] = useState(0);
  const [sociosActivos, setSociosActivos] = useState(0);

  // Carga de datos dinámicos desde API
  useEffect(() => {
    let cancelado = false;

    async function cargarMetricas() {
      // Carga dinámica de canchas reales para todos los roles
      try {
        const resCanchas = await axiosClient.get("/canchas?size=50");
        const contenido = resCanchas.data?.contenido || resCanchas.data;
        if (!cancelado && Array.isArray(contenido)) {
          setCanchas(
            contenido.map((c) => ({
              id: c.id,
              numero: c.identificador || String(c.id),
              nombre: c.nombre || `Cancha ${c.identificador || c.id}`,
              superficie: c.superficie || c.tipoSuperficie || "Polvo de ladrillo",
              activa: c.activa !== false,
            }))
          );
        }
      } catch {
        // El socio no tiene permiso para listar canchas: se avisa en vez de
        // inventar uno.
        if (!cancelado) setCanchasSinCargar(true);
      }

      if (esPersonal) {
        try {
          const resSocios = await axiosClient.get("/socios?size=1");
          if (!cancelado && typeof resSocios.data?.totalElementos === "number") {
            setSociosActivos(resSocios.data.totalElementos);
          }
        } catch {
          // métrica opcional
        }

        try {
          const resReservas = await axiosClient.get("/reservas?size=50");
          if (!cancelado && typeof resReservas.data?.totalElementos === "number") {
            setTurnosHoy(resReservas.data.totalElementos);
          }
        } catch {
          // métrica opcional
        }
      }
    }

    cargarMetricas();
    return () => {
      cancelado = true;
    };
  }, [esPersonal]);

  // Nombre amigable para el rol accesible
  const nombreUsuario = useMemo(() => {
    if (!user?.email) return "Usuario";
    const parteLocal = user.email.split("@")[0];
    return parteLocal.charAt(0).toUpperCase() + parteLocal.slice(1);
  }, [user]);

  // Fecha actual formateada en español
  const fechaActual = useMemo(() => {
    const hoy = new Date();
    const texto = hoy.toLocaleDateString("es-AR", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }, []);

  const totalCanchas = canchas.length;
  const canchasDisponibles = canchas.filter((c) => c.activa !== false).length;
  const canchasOcupadas = totalCanchas - canchasDisponibles;

  return (
    <div className="space-y-6 sm:space-y-8 text-left max-w-5xl">
      {/* Cero títulos visuales redundantes: sólo encabezado accesible para a11y */}
      <h1 className="sr-only">
        Dashboard de {nombreUsuario}
      </h1>

      {/* Barra de estado operativa y discreta */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 pb-1 text-xs text-muted-foreground">
        <div className="inline-flex items-center gap-2">
          <span className="size-2 rounded-full bg-emerald-500 animate-pulse" />
          <span className="font-medium text-foreground">
            Club Abierto • {totalCanchas} canchas operativas
          </span>
          <span className="hidden sm:inline text-muted-foreground/40">•</span>
          <span className="hidden sm:inline text-muted-foreground">
            Tenis Ahora Club & Sistema Deportivo
          </span>
        </div>
        <span className="text-[11px] sm:text-xs text-muted-foreground">{fechaActual}</span>
      </div>

      {/* 2. Tarjeta Horizontal de Métricas (Design horizontal unificado) */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <KpiCard
          etiqueta="Canchas Disponibles"
          valor={canchasDisponibles}
          detalle={`de ${totalCanchas} pistas`}
          icono={LayoutGrid}
        />

        <KpiCard
          etiqueta="Turnos del Día"
          valor={turnosHoy}
          detalle="reservas agendadas"
          icono={Calendar}
        />

        <KpiCard
          etiqueta="Socios Activos"
          valor={sociosActivos}
          detalle="al día"
          icono={Users}
        />
      </div>

      {/* 3. Accesos Directos Discretos */}
      <section aria-label="Accesos directos">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {ACCESOS_PERSONAL.map(({ to, etiqueta, titulo, detalle, Icono }) => (
            <motion.div
              key={to}
              whileHover={{ y: -1 }}
              whileTap={{ scale: 0.99 }}
              transition={{ duration: 0.15 }}
            >
              <Link
                to={to}
                aria-label={etiqueta}
                className="group flex items-center justify-between rounded-xl border border-border bg-card px-5 py-3.5 transition-colors hover:border-foreground/30 hover:bg-muted/30 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring shadow-2xs"
              >
                <div className="flex items-center gap-3.5">
                  <div className="flex size-8.5 items-center justify-center rounded-lg bg-muted/60 text-muted-foreground transition-colors group-hover:text-foreground">
                    <Icono className="size-4" />
                  </div>
                  <div>
                    <span className="text-xs font-medium text-foreground block">
                      {titulo}
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      {detalle}
                    </span>
                  </div>
                </div>
                <ArrowUpRight className="size-3.5 text-muted-foreground/50 transition-colors group-hover:text-foreground" />
              </Link>
            </motion.div>
          ))}
        </div>
      </section>

      {/* 4. Estado de Canchas Silencioso */}
      <section aria-label="Estado de las canchas" className="space-y-4 pt-2">
        <div className="flex items-center justify-between pb-1">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Estado de Canchas
          </h2>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="size-1.5 rounded-full bg-emerald-500" />
              {canchasDisponibles} Libres
            </span>
            <span className="flex items-center gap-1.5">
              <span className="size-1.5 rounded-full bg-amber-500" />
              {canchasOcupadas} Ocupadas
            </span>
          </div>
        </div>

        {canchas.length === 0 && (
          <p className="rounded-xl border border-dashed border-border py-10 text-center text-sm text-muted-foreground">
            {canchasSinCargar
              ? "No se pudo cargar el estado de las canchas."
              : "Todavía no hay canchas cargadas."}
          </p>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {canchas.map((cancha) => {
            const estaDisponible = cancha.activa !== false;

            return (
              <div
                key={cancha.id}
                className="flex flex-col justify-between rounded-xl border border-border bg-card p-5 sm:p-5.5 transition-colors hover:border-foreground/30 shadow-2xs"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-xs font-semibold text-foreground">
                      {cancha.nombre}
                    </h3>
                    <span className="text-[11px] text-muted-foreground">
                      {cancha.superficie}
                    </span>
                  </div>

                  <span
                    className={`inline-flex items-center gap-1.5 text-[11px] font-medium ${
                      estaDisponible
                        ? "text-emerald-700 dark:text-emerald-400"
                        : "text-amber-700 dark:text-amber-400"
                    }`}
                  >
                    <span
                      className={`size-1.5 rounded-full ${
                        estaDisponible ? "bg-emerald-500" : "bg-amber-500"
                      }`}
                    />
                    {estaDisponible ? "Disponible" : "Mantenimiento"}
                  </span>
                </div>

                <div className="mt-4 border-t border-border pt-2.5 text-[11px] text-muted-foreground flex items-center justify-between">
                  <span className="truncate">
                    {estaDisponible ? "Libre para reservar" : "No disponible"}
                  </span>
                  <span className="font-mono text-[10px] text-muted-foreground/60 shrink-0 ml-2">
                    Pista {cancha.numero}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}

export default function DashboardPage() {
  const rol = useAuthStore((state) => state.user?.rol);
  return rol === "SOCIO" ? <DashboardSocio /> : <DashboardPersonal />;
}
