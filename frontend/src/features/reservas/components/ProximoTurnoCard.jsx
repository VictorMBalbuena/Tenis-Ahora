import { Link } from "react-router-dom";
import { CalendarClock, Clock, CreditCard, MapPin } from "lucide-react";
import { CuentaRegresiva } from "@/components/ui/cuenta-regresiva";
import { fechaLarga, faltaParaLaFecha, formatearHorarioTurno } from "../model/reserva";

// Celda principal del inicio del socio: el turno que sigue, o el que hay que pagar para no perderlo.
export function ProximoTurnoCard({ reserva, cargando }) {
  const retenida = reserva?.estado === "PENDIENTE_PAGO";

  return (
    <section className="relative flex h-full min-h-[200px] flex-col justify-between gap-5 overflow-hidden rounded-xl bg-primary p-6 text-primary-foreground shadow-sm">
      <CalendarClock aria-hidden="true" className="pointer-events-none absolute -right-6 -top-6 size-44 opacity-10" />

      {!reserva ? (
        <div>
          <p className="text-sm font-medium opacity-80">Tu próximo turno</p>
          <p className="mt-2 text-2xl font-extrabold tracking-[-.02em]">
            {cargando ? "Cargando..." : "No tenés turnos próximos"}
          </p>
        </div>
      ) : (
        <>
          <div className="relative min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-medium opacity-80">{retenida ? "Turno retenido" : "Tu próximo turno"}</p>
              <span className="rounded-full bg-primary-foreground/15 px-2.5 py-0.5 text-xs font-semibold">
                {faltaParaLaFecha(reserva.fecha)}
              </span>
            </div>
            <p className="mt-2 text-2xl font-extrabold leading-tight tracking-[-.02em]">{fechaLarga(reserva.fecha)}</p>
            <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm opacity-90">
              <span className="inline-flex items-center gap-1.5">
                <Clock className="size-4" />
                {formatearHorarioTurno(reserva.horaInicio, reserva.horaFin)}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <MapPin className="size-4" />
                {reserva.cancha}
              </span>
            </div>
          </div>

          <div className="relative flex flex-wrap items-center gap-3">
            <Link
              to={retenida ? `/reservas/${reserva.id}/cobro` : `/reservas/${reserva.id}`}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-full bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:bg-muted"
            >
              {retenida && <CreditCard className="size-4" />}
              {retenida ? "Pagar seña" : "Ver detalle"}
            </Link>
            {retenida && reserva.venceEn && (
              <CuentaRegresiva venceEn={reserva.venceEn} prefijo="Para pagar la seña:" className="text-sm font-semibold" />
            )}
          </div>
        </>
      )}
    </section>
  );
}
