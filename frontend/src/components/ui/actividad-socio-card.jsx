import { Link } from "react-router-dom";
import { Clock, Loader2 } from "lucide-react";
import { CuentaRegresiva } from "@/components/ui/cuenta-regresiva";
import { cn } from "@/lib/utils";

// Tarjeta de una clase o un entrenamiento para el socio: los días se ven de un vistazo y el resto está en su pantalla.
const DIAS = [
  ["MONDAY", "L"],
  ["TUESDAY", "M"],
  ["WEDNESDAY", "X"],
  ["THURSDAY", "J"],
  ["FRIDAY", "V"],
  ["SATURDAY", "S"],
  ["SUNDAY", "D"],
];

export const moneda = (monto) => `$ ${Number(monto || 0).toLocaleString("es-AR")}`;
export const conValor = (actividad) => Number(actividad.valorMensual) > 0;

export function DiasDeLaSemana({ dias = [] }) {
  return (
    <div className="flex gap-1" aria-label="Días de dictado">
      {DIAS.map(([valor, letra]) => (
        <span
          key={valor}
          className={cn(
            "flex size-7 items-center justify-center rounded-full text-xs font-semibold",
            dias.includes(valor) ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground/50"
          )}
        >
          {letra}
        </span>
      ))}
    </div>
  );
}

export function EstadoDeInscripcion({ inscripcion }) {
  if (inscripcion.estado === "PENDIENTE_PAGO") {
    const clase = "rounded-full bg-[var(--estado-aviso-bg)] px-2.5 py-1 text-xs font-semibold text-[var(--estado-aviso-fg)]";
    return inscripcion.venceEn
      ? <CuentaRegresiva venceEn={inscripcion.venceEn} prefijo="Retenido" className={clase} />
      : <span className={clase}>Lugar retenido</span>;
  }
  return <span className="rounded-full bg-[var(--estado-ok-bg)] px-2.5 py-1 text-xs font-semibold text-[var(--estado-ok-fg)]">Inscripto</span>;
}

function AccionPrincipal({ actividad, inscripcion, procesando, onInscribirme }) {
  const clase = "inline-flex h-9 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-primary-foreground transition-colors hover:brightness-95 disabled:opacity-50";
  if (inscripcion?.estado === "PENDIENTE_PAGO") {
    return <Link to={`/inscripciones/${inscripcion.id}/cobro`} className={clase}>Pagar seña</Link>;
  }
  if (Number(inscripcion?.saldoPendiente) > 0) {
    return <Link to={`/inscripciones/${inscripcion.id}/cobro`} className={clase}>Pagar saldo</Link>;
  }
  if (inscripcion) return null;
  const sinLugar = (actividad.cupoDisponible ?? 0) <= 0;
  return (
    <button type="button" disabled={sinLugar || procesando} onClick={() => onInscribirme(actividad)} className={clase}>
      {procesando && <Loader2 className="size-4 animate-spin" />}
      {sinLugar ? "Sin lugares" : "Inscribirme"}
    </button>
  );
}

export function ActividadSocioCard({ actividad, inscripcion, datos, Icono, ruta, procesando, onInscribirme }) {
  return (
    <article className="flex min-w-0 flex-col gap-4 rounded-xl border border-border bg-card p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <Icono className="size-5" />
        </span>
        {inscripcion ? (
          <EstadoDeInscripcion inscripcion={inscripcion} />
        ) : (
          <span className="font-mono text-sm font-semibold tabular-nums text-foreground">
            {conValor(actividad) ? moneda(actividad.valorMensual) : "Sin cargo"}
          </span>
        )}
      </div>

      <div className="min-w-0">
        <h3 className="truncate text-base font-bold text-foreground">{actividad.nombre}</h3>
        <p className="truncate text-sm text-muted-foreground">{datos.etiqueta}</p>
      </div>

      <div className="space-y-2.5">
        <DiasDeLaSemana dias={actividad.dias} />
        <p className="flex items-center gap-1.5 text-sm text-foreground">
          <Clock className="size-4 text-muted-foreground" />
          {datos.horario}
        </p>
      </div>

      <div className="mt-auto flex items-center justify-between gap-3 border-t border-border pt-3">
        <Link to={`${ruta}/${actividad.id}`} className="text-sm font-semibold text-primary hover:underline">
          Ver detalles
        </Link>
        <AccionPrincipal actividad={actividad} inscripcion={inscripcion} procesando={procesando} onInscribirme={onInscribirme} />
      </div>
    </article>
  );
}
