import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, CircleAlert, CircleSlash, Dumbbell, GraduationCap, RotateCcw } from "lucide-react";
import { ErrorBanner } from "@/components/ui/error-banner";
import { listarMisInscripciones } from "@/features/inscripciones/api/inscripcionesApi";
import { obtenerMisReservas } from "@/features/reservas/api/reservasApi";
import { ProximoTurnoCard } from "@/features/reservas/components/ProximoTurnoCard";
import { ReservasSocioPestanas } from "@/features/reservas/components/ReservasSocioPestanas";
import { resumirCancelaciones } from "@/features/reservas/model/reserva";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";

// Inicio del socio: en una sola pantalla, su próximo turno, sus inscripciones, sus reservas y sus cancelaciones.
const TAMANIO_DE_PAGINA = 50;
const ESTADOS_TERMINADOS = ["CANCELADA", "FINALIZADA"];
const LISTAS_VACIAS = { proximas: [], retenidas: [], jugadas: [], canceladas: [] };
const TOTALES_EN_CERO = { proximas: 0, retenidas: 0, jugadas: 0, canceladas: 0 };

function TarjetaDeInscripcion({ to, titulo, inscripciones, Icono, sinInscripciones }) {
  const cantidad = inscripciones.length;
  const sinPagar = inscripciones.filter((i) => i.estado === "PENDIENTE_PAGO").length;
  const detalle = sinPagar > 0 ? `${sinPagar} con seña sin pagar` : cantidad > 0 ? "Inscripciones activas" : sinInscripciones;
  return (
    <Link
      to={to}
      className="group flex min-w-0 flex-col justify-between gap-4 rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-foreground/30 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <span className="flex items-start justify-between gap-2">
        <span className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <Icono className="size-5" />
        </span>
        <ArrowUpRight className="size-4 text-muted-foreground/50 transition-colors group-hover:text-foreground" />
      </span>
      <span className="min-w-0">
        <span className="block text-right font-mono text-3xl font-semibold leading-none tabular-nums text-foreground">{cantidad}</span>
        <span className="mt-2 block font-semibold text-foreground">{titulo}</span>
        <span className="block truncate text-xs text-muted-foreground">
          {detalle}
        </span>
      </span>
    </Link>
  );
}

function FilaDeCancelacion({ Icono, etiqueta, valor, tono }) {
  return (
    <div className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
      <span className={`flex size-9 shrink-0 items-center justify-center rounded-lg ${tono}`}>
        <Icono className="size-4" />
      </span>
      <span className="min-w-0 flex-1 truncate text-sm text-muted-foreground">{etiqueta}</span>
      <span className="shrink-0 font-mono text-sm font-semibold tabular-nums text-foreground">{valor}</span>
    </div>
  );
}

function PanelDeCancelaciones({ canceladas }) {
  const resumen = resumirCancelaciones(canceladas);
  return (
    <section className="flex h-full min-w-0 flex-col rounded-xl border border-border bg-card p-5 shadow-sm">
      <h2 className="mb-4 text-lg font-bold text-foreground">Cancelaciones</h2>
      <div className="divide-y divide-border">
        <FilaDeCancelacion Icono={CircleSlash} etiqueta="Sin cargo" valor={resumen.sinCargo} tono="bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]" />
        <FilaDeCancelacion Icono={RotateCcw} etiqueta="Con reembolso" valor={resumen.conReembolso} tono="bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]" />
        <FilaDeCancelacion Icono={CircleAlert} etiqueta="Con seña perdida" valor={resumen.conSeniaPerdida} tono="bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]" />
      </div>
    </section>
  );
}

export function DashboardSocio() {
  const nombre = useAuthStore((estado) => estado.user?.nombre);
  const [listas, setListas] = useState(LISTAS_VACIAS);
  const [totales, setTotales] = useState(TOTALES_EN_CERO);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [inscripciones, setInscripciones] = useState([]);

  useEffect(() => {
    let cancelado = false;

    Promise.all([
      obtenerMisReservas({ proximas: true, size: TAMANIO_DE_PAGINA }),
      obtenerMisReservas({ estado: "PENDIENTE_PAGO", size: TAMANIO_DE_PAGINA }),
      obtenerMisReservas({ estado: "FINALIZADA", size: TAMANIO_DE_PAGINA }),
      obtenerMisReservas({ estado: "CANCELADA", size: TAMANIO_DE_PAGINA }),
    ])
      .then(([proximas, retenidas, jugadas, canceladas]) => {
        if (cancelado) return;
        const vigentes = (proximas.contenido || []).filter((r) => !ESTADOS_TERMINADOS.includes(r.estado));
        setListas({
          proximas: vigentes,
          retenidas: retenidas.contenido || [],
          jugadas: jugadas.contenido || [],
          canceladas: canceladas.contenido || [],
        });
        setTotales({
          proximas: vigentes.length,
          retenidas: retenidas.totalElementos || 0,
          jugadas: jugadas.totalElementos || 0,
          canceladas: canceladas.totalElementos || 0,
        });
      })
      .catch((e) => {
        if (!cancelado) setError(mensajeDeError(e, "No se pudieron cargar tus reservas."));
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });

    listarMisInscripciones()
      .then((lista) => {
        if (!cancelado) setInscripciones(lista.filter((i) => i.activa));
      })
      .catch(() => {});

    return () => {
      cancelado = true;
    };
  }, []);

  const delTipo = (tipo) => inscripciones.filter((i) => i.tipo === tipo);
  const turnoDestacado = listas.proximas.find((r) => r.estado === "PENDIENTE_PAGO") ?? listas.proximas[0];

  return (
    <div className="flex w-full min-w-0 flex-col gap-5 lg:h-full">
      <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">
        {nombre ? `Hola, ${nombre}` : "Hola"}
      </h1>

      {error && <ErrorBanner>{error}</ErrorBanner>}

      <div className="grid min-w-0 grid-cols-1 gap-5 lg:min-h-[520px] lg:flex-1 lg:grid-cols-12 lg:grid-rows-[auto_minmax(0,1fr)]">
        <div className="min-w-0 lg:col-span-7">
          <ProximoTurnoCard reserva={turnoDestacado} cargando={cargando} />
        </div>
        <div className="grid min-w-0 grid-cols-2 gap-5 lg:col-span-5">
          <TarjetaDeInscripcion to="/clases" titulo="Clases" inscripciones={delTipo("CLASE")} Icono={GraduationCap} sinInscripciones="Mirá las disponibles" />
          <TarjetaDeInscripcion to="/entrenamientos" titulo="Entrenamientos" inscripciones={delTipo("ENTRENAMIENTO")} Icono={Dumbbell} sinInscripciones="Mirá los disponibles" />
        </div>
        <div className="min-h-0 min-w-0 lg:col-span-8">
          <ReservasSocioPestanas listas={listas} totales={totales} cargando={cargando} />
        </div>
        <div className="min-w-0 lg:col-span-4">
          <PanelDeCancelaciones canceladas={listas.canceladas} />
        </div>
      </div>
    </div>
  );
}
