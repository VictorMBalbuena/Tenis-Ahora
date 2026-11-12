import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, Clock, Loader2, ArrowLeft } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { previsualizarCancelacion, confirmarCancelacion } from "../api/reservasApi";
import { formatearMoneda, extraerMensajeError } from "../model/reserva";
import { useReglasDeReserva } from "../hooks/useReglasDeReserva";

function CancelarReservaContent({ reserva, onCerrar, onCanceladaExitosamente }) {
  const reglas = useReglasDeReserva();
  const [cargandoPrevia, setCargandoPrevia] = useState(true);
  const [previa, setPrevia] = useState(null);
  const [errorPrevia, setErrorPrevia] = useState(null);

  const [confirmando, setConfirmando] = useState(false);
  const [errorConfirmar, setErrorConfirmar] = useState(null);

  useEffect(() => {
    let montado = true;

    previsualizarCancelacion(reserva.id)
      .then((data) => {
        if (montado) {
          setPrevia(data);
          setCargandoPrevia(false);
        }
      })
      .catch((err) => {
        if (montado) {
          setErrorPrevia(
            extraerMensajeError(
              err,
              "No se pudieron consultar las condiciones de cancelación."
            )
          );
          setCargandoPrevia(false);
        }
      });

    return () => {
      montado = false;
    };
  }, [reserva.id]);

  const handleConfirmar = async () => {
    try {
      setConfirmando(true);
      setErrorConfirmar(null);
      const resultado = await confirmarCancelacion(reserva.id);
      onCanceladaExitosamente(resultado);
      onCerrar();
    } catch (err) {
      setErrorConfirmar(
        extraerMensajeError(
          err,
          "Ocurrió un error al intentar cancelar la reserva."
        )
      );
    } finally {
      setConfirmando(false);
    }
  };

  const numero = reserva.numero || `R-${String(reserva.id).padStart(6, "0")}`;
  const nombreCancha =
    reserva.cancha || reserva.canchaNombre || (reserva.canchaId ? `Cancha ${reserva.canchaId}` : "Cancha");
  const horario = reserva.horaInicio
    ? `${reserva.horaInicio.slice(0, 5)}${reserva.horaFin ? ` a ${reserva.horaFin.slice(0, 5)}` : ""}`
    : "—";

  const senia = reserva.montoSenia ?? reserva.senia ?? 0;

  return (
    <DialogContent className="sm:max-w-xl rounded-2xl p-6 max-h-[90vh] overflow-y-auto">
      <DialogHeader className="space-y-2">
        <DialogTitle className="text-xl font-bold flex items-center gap-2 text-foreground">
          <AlertTriangle className="w-5 h-5 text-destructive" />
          Cancelar reserva
        </DialogTitle>
        <DialogDescription className="text-sm font-mono text-muted-foreground">
          {numero ? `Reserva ${numero}. ` : ""}
          Revisá las condiciones y penalidad aplicables según la antelación.
        </DialogDescription>
      </DialogHeader>

      {/* Estado de carga */}
      {cargandoPrevia && (
        <div className="py-10 flex flex-col items-center justify-center gap-3 text-muted-foreground">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <p className="text-sm">Calculando condiciones de cancelación...</p>
        </div>
      )}

      {/* Error al previsualizar */}
      {!cargandoPrevia && errorPrevia && (
        <div className="p-4 rounded-xl border border-destructive/20 bg-destructive/10 text-destructive text-sm flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Error al consultar cancelación</p>
            <p>{errorPrevia}</p>
          </div>
        </div>
      )}

      {/* Previsualización exitosa según reserva-09-cancelar-dentro.html y reserva-10 */}
      {!cargandoPrevia && previa && (
        <div className="space-y-4 my-2">
          {/* Tarjeta 1: Reserva a cancelar */}
          <div className="rounded-xl border border-border bg-card p-4 shadow-2xs">
            <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground pb-2 border-b border-border/50">
              Reserva a cancelar
            </h3>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 pt-3 text-sm">
              <div>
                <dt className="text-xs text-muted-foreground">Código</dt>
                <dd className="font-mono font-semibold tabular-nums text-foreground">{numero}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Cancha</dt>
                <dd className="font-medium text-foreground">{nombreCancha}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Turno</dt>
                <dd className="font-mono tabular-nums text-foreground">
                  {reserva.fecha ? `${reserva.fecha} ${horario}` : horario}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Seña abonada</dt>
                <dd className="font-mono tabular-nums font-semibold text-foreground">
                  {formatearMoneda(senia)}
                </dd>
              </div>
            </dl>
          </div>

          {/* Tarjeta 2: Cálculo de la ventana de cancelación con línea temporal según mockup L263-285 */}
          <div className="rounded-xl border border-border bg-card p-4 shadow-2xs space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Ventana de cancelación
              </h3>
              <span className="text-xs text-muted-foreground">
                Límite de penalidad: {reglas?.plazoCancelacionHoras ?? "—"} horas
              </span>
            </div>

            {/* Barra temporal visual interactiva */}
            <div className="space-y-1.5 pt-1">
              <div className="relative h-4 text-[11px] text-muted-foreground">
                <span className="absolute left-0 tabular-nums">24 h antes</span>
                <span className="absolute -translate-x-1/2 font-semibold text-foreground" style={{ left: "75%" }}>
                  Límite: {reglas?.plazoCancelacionHoras ?? "—"} h
                </span>
                <span className="absolute right-0 tabular-nums">0 h · turno</span>
              </div>
              <div className="relative h-3 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className="absolute inset-y-0 left-0 rounded-l-full bg-[var(--estado-ok-bg)]"
                  style={{ width: "75%" }}
                />
                <div
                  className="absolute inset-y-0 right-0 rounded-r-full bg-[var(--estado-error-bg)]"
                  style={{ left: "75%" }}
                />
                <div
                  className="absolute inset-y-0 w-0.5 bg-foreground/60"
                  style={{ left: "75%" }}
                />
                <div
                  className={`absolute -top-0.5 h-4 w-1.5 rounded-full ${
                    previa.aTiempo ? "bg-[var(--estado-ok-fg)]" : "bg-[var(--estado-error-fg)]"
                  }`}
                  style={{ left: previa.aTiempo ? "30%" : "90%" }}
                />
              </div>
              <div className="relative h-4 text-[10px] text-muted-foreground">
                <span className="absolute -translate-x-1/2" style={{ left: "37.5%" }}>
                  Reembolso completo
                </span>
                <span className="absolute -translate-x-1/2" style={{ left: "87.5%" }}>
                  Penalidad
                </span>
              </div>
            </div>

            {/* Banner de aviso según antelación */}
            {previa.aTiempo ? (
              <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]">
                <CheckCircle2 className="h-[18px] w-[18px] shrink-0" />
                <p className="text-sm font-semibold">
                  {previa.mensaje || "Cancelás a tiempo: corresponde el 100% de reembolso."}
                </p>
              </div>
            ) : (
              <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]">
                <AlertTriangle className="h-[18px] w-[18px] shrink-0" />
                <p className="text-sm font-semibold">
                  {previa.mensaje || "Cancelás fuera de término: se retiene el 100% de la seña abonada."}
                </p>
              </div>
            )}
          </div>

          {/* Tarjeta 3: Desglose de penalidad y reembolso */}
          <div className="p-4 rounded-xl border border-border bg-card space-y-2.5 text-sm shadow-2xs">
            <div className="flex justify-between items-center">
              <span className="text-muted-foreground">Penalidad aplicada:</span>
              <span
                className={`font-mono font-bold tabular-nums ${
                  previa.montoPenalidad > 0 ? "text-destructive" : "text-foreground"
                }`}
              >
                {formatearMoneda(previa.montoPenalidad || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center pt-2 border-t border-border">
              <span className="font-medium text-foreground">Monto a reembolsar:</span>
              <span className="font-mono font-bold tabular-nums text-foreground">
                {formatearMoneda(previa.montoReembolso || 0)}
              </span>
            </div>
          </div>

          {/* Error al confirmar */}
          {errorConfirmar && (
            <div className="p-3 rounded-lg border border-destructive/20 bg-destructive/10 text-destructive text-xs font-semibold">
              {errorConfirmar}
            </div>
          )}
        </div>
      )}

      <DialogFooter className="mt-4 flex flex-col sm:flex-row gap-2">
        <button
          type="button"
          onClick={onCerrar}
          disabled={confirmando}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors border border-input bg-background hover:bg-muted h-10 px-5 w-full sm:w-auto cursor-pointer disabled:opacity-50"
        >
          <ArrowLeft className="h-[18px] w-[18px] shrink-0" />
          <span>Volver</span>
        </button>
        <button
          type="button"
          onClick={handleConfirmar}
          disabled={cargandoPrevia || confirmando || !previa}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold transition-colors bg-destructive text-destructive-foreground hover:brightness-95 h-10 px-5 w-full sm:w-auto cursor-pointer shadow-xs disabled:pointer-events-none disabled:opacity-50"
        >
          {confirmando ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Cancelando...</span>
            </>
          ) : (
            <span>Confirmar cancelación</span>
          )}
        </button>
      </DialogFooter>
    </DialogContent>
  );
}

/**
 * Diálogo modal para cancelar una reserva con previsualización de penalidad (CU18, RD12, RD13).
 */
export function CancelarReservaDialog({
  reserva,
  abierta,
  open,
  onCerrar = () => {},
  onCanceladaExitosamente = () => {},
}) {
  const estaAbierta = Boolean(abierta ?? open);

  return (
    <Dialog
      open={estaAbierta}
      onOpenChange={(v) => {
        if (!v) onCerrar();
      }}
    >
      {estaAbierta && reserva?.id && (
        <CancelarReservaContent
          key={reserva.id}
          reserva={reserva}
          onCerrar={onCerrar}
          onCanceladaExitosamente={onCanceladaExitosamente}
        />
      )}
    </Dialog>
  );
}

export default CancelarReservaDialog;
