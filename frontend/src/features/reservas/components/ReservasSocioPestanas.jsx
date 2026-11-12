import { useState } from "react";
import { Link } from "react-router-dom";
import { Ban, CalendarDays, CheckCircle2, ChevronRight, Hourglass } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  ESTADOS_RESERVA,
  diaYMes,
  formatearFecha,
  formatearHorarioTurno,
  resultadoDeCancelacion,
} from "../model/reserva";

// Todas las reservas del socio en una sola tarjeta: cada pestaña trae su cantidad y la lista se desplaza por dentro.
const PESTANAS = [
  { id: "proximas", titulo: "Próximas", Icono: CalendarDays, vacio: "No tenés reservas próximas." },
  { id: "retenidas", titulo: "Retenidas", Icono: Hourglass, vacio: "No tenés turnos esperando el pago de la seña." },
  { id: "jugadas", titulo: "Jugadas", Icono: CheckCircle2, vacio: "Todavía no jugaste ninguna reserva." },
  { id: "canceladas", titulo: "Canceladas", Icono: Ban, vacio: "No tenés reservas canceladas." },
];

function FilaDeReserva({ reserva }) {
  const estado = ESTADOS_RESERVA[reserva.estado] ?? { label: reserva.estado, clase: "bg-muted text-muted-foreground" };
  const fecha = diaYMes(reserva.fecha);
  const retenida = reserva.estado === "PENDIENTE_PAGO";
  const cancelada = reserva.estado === "CANCELADA";

  return (
    <li className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg px-2 py-2.5 transition-colors hover:bg-muted/50">
      <span className="flex size-12 shrink-0 flex-col items-center justify-center rounded-xl bg-muted leading-none">
        <span className="text-base font-bold text-foreground">{fecha.dia}</span>
        <span className="mt-0.5 text-[10px] font-semibold uppercase text-muted-foreground">{fecha.mes}</span>
      </span>

      <div className="min-w-0 flex-1 basis-40">
        <p className="truncate text-sm font-semibold text-foreground">
          {formatearHorarioTurno(reserva.horaInicio, reserva.horaFin)}
        </p>
        <p className="truncate text-xs text-muted-foreground">
          {reserva.cancha} · {formatearFecha(reserva.fecha)}
        </p>
      </div>

      <div className="ml-auto flex shrink-0 items-center gap-2">
        {cancelada ? (
          <span className="text-xs font-medium text-muted-foreground">{resultadoDeCancelacion(reserva)}</span>
        ) : (
          <span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${estado.clase}`}>{estado.label}</span>
        )}
        {retenida && (
          <Link
            to={`/reservas/${reserva.id}/cobro`}
            className="inline-flex h-8 items-center rounded-full bg-primary px-3.5 text-xs font-semibold text-primary-foreground transition-colors hover:brightness-95"
          >
            Pagar seña
          </Link>
        )}
        <Link
          to={`/reservas/${reserva.id}`}
          aria-label={`Ver detalle de la reserva del ${formatearFecha(reserva.fecha)}`}
          className="inline-flex size-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <ChevronRight className="size-4" />
        </Link>
      </div>
    </li>
  );
}

export function ReservasSocioPestanas({ listas, totales, cargando }) {
  const [activa, setActiva] = useState("proximas");
  const pestana = PESTANAS.find((p) => p.id === activa);
  const reservas = listas[activa];

  return (
    <section className="flex h-full min-h-[320px] min-w-0 flex-col rounded-xl border border-border bg-card p-4 shadow-sm sm:p-5">
      <div className="flex items-center justify-between gap-3 border-b border-border pb-3">
        <div role="tablist" aria-label="Mis reservas" className="flex min-w-0 gap-1 overflow-x-auto">
          {PESTANAS.map(({ id, titulo, Icono }) => (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={activa === id}
              onClick={() => setActiva(id)}
              className={cn(
                "inline-flex h-9 shrink-0 items-center gap-1.5 rounded-full px-3 text-sm font-semibold transition-colors",
                activa === id ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <Icono className="size-4" />
              {titulo}
              <span className={cn("rounded-full px-1.5 text-xs tabular-nums", activa === id ? "bg-primary-foreground/20" : "bg-muted")}>
                {totales[id]}
              </span>
            </button>
          ))}
        </div>
        <Link to="/reservas" className="hidden shrink-0 text-sm font-semibold text-primary hover:underline xl:inline">
          Ver todas
        </Link>
      </div>

      <div role="tabpanel" aria-label={pestana.titulo} className="mt-2 min-h-0 flex-1 overflow-y-auto">
        {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando reservas...</p>}
        {!cargando && reservas.length === 0 && (
          <p className="py-10 text-center text-sm text-muted-foreground">{pestana.vacio}</p>
        )}
        {!cargando && reservas.length > 0 && (
          <ul className="space-y-0.5">
            {reservas.map((reserva) => (
              <FilaDeReserva key={reserva.id} reserva={reserva} />
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
