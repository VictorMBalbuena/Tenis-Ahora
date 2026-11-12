import { useState } from "react";
import { AlertCircle, AlertTriangle, ArrowLeft, Loader2 } from "lucide-react";
import { crearReserva } from "../api/reservasApi";
import { formatearMoneda, formatearHorarioTurno, extraerMensajeError } from "../model/reserva";
import { InfoTip } from "@/components/ui/campo-label";
import { useReglasDeReserva } from "../hooks/useReglasDeReserva";
import { ErrorBanner } from "@/components/ui/error-banner";
import { CodigoDeDescuento } from "./CodigoDeDescuento";

// Calcula la hora de fin estimada si el draft no la provee
function calcularHoraFin(horaInicio, duracionHoras = 1) {
  if (!horaInicio) return "";
  const [h, m = 0] = horaInicio.split(":").map(Number);
  const finMinutos = h * 60 + m + duracionHoras * 60;
  const finH = String(Math.floor(finMinutos / 60) % 24).padStart(2, "0");
  const finM = String(finMinutos % 60).padStart(2, "0");
  return `${finH}:${finM}`;
}

function obtenerIniciales(nombre, apellido) {
  const p1 = (nombre || "").trim().charAt(0);
  const p2 = (apellido || "").trim().charAt(0);
  return (p1 + p2).toUpperCase() || "TA";
}

/**
 * Paso 4 del Wizard de Reservas: Resumen general y confirmación definitiva.
 * Sigue el diseño de `reserva-04-resumen.html`.
 */
export function PasoResumen({
  draft = {},
  onVolver = () => {},
  onConfirmar,
  onConfirmarExitoso = () => {},
}) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  const duracion = Number(draft?.duracionHoras || 1);
  const precioHora = Number(draft?.precioPorHora || 0);
  const subtotalCancha = precioHora * duracion;

  const insumos = Array.isArray(draft?.insumos) ? draft.insumos : [];
  const subtotalInsumos = insumos.reduce(
    (acc, it) => acc + Number(it.cantidad || 0) * Number(it.precioUnitario || 0),
    0
  );

  // El descuento se calcula sobre el alquiler de la cancha, que es el total que
  // maneja el servidor. Los insumos no entran: hoy no los suma al total de la reserva.
  const [descuento, setDescuento] = useState(null);
  const montoDescontado = Number(descuento?.montoDescontado || 0);

  const totalEstimado = Math.max(0, subtotalCancha + subtotalInsumos - montoDescontado);
  const reglas = useReglasDeReserva();
  const seniaEstimada = reglas ? (totalEstimado * reglas.porcentajeSenia) / 100 : null;

  const horaFin = draft?.horaFin || calcularHoraFin(draft?.horaInicio, duracion);
  const horario = formatearHorarioTurno(draft?.horaInicio, horaFin);

  const modalidadTexto =
    draft?.modalidad?.toUpperCase() === "DOBLES" ? "Dobles (4 jugadores)" : "Single (2 jugadores)";
  const acompaniantes = Array.isArray(draft?.acompaniantes) ? draft.acompaniantes : [];

  const canchaNombre =
    draft?.canchaNombre || draft?.cancha?.nombre || (draft?.canchaId ? `Cancha ${draft.canchaId}` : "Cancha");

  const titularNombre = draft?.titularNombre || (draft?.titularId ? `Socio #${draft.titularId}` : null);

  const handleConfirmar = async () => {
    if (onConfirmar) {
      onConfirmar(draft);
      return;
    }

    try {
      setEnviando(true);
      setError(null);

      const payload = {
        canchaId: draft.canchaId,
        fecha: draft.fecha,
        horaInicio: draft.horaInicio,
        duracionHoras: duracion,
        modalidad: draft.modalidad,
        acompaniantes: acompaniantes.map((a) => ({
          personaId:
            a.personaId != null && a.personaId !== "" ? Number(a.personaId) : null,
          nombre: a.nombre?.trim() || "",
          apellido: a.apellido?.trim() || "",
          documento: a.documento?.trim() || undefined,
        })),
        insumos: insumos.map((item) => ({
          insumoId: item.insumoId ?? item.id,
          cantidad: Number(item.cantidad || 0),
        })),
      };

      if (descuento?.codigo) {
        payload.codigoDescuento = descuento.codigo;
      }

      if (draft.titularId) {
        payload.titularId = Number(draft.titularId);
      }

      const respuesta = await crearReserva(payload);
      onConfirmarExitoso(respuesta);
    } catch (err) {
      setError(
        extraerMensajeError(
          err,
          "No se pudo confirmar la reserva. Por favor intentalo nuevamente."
        )
      );
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Título de encabezado exacto para pruebas y accesibilidad */}
      <div className="space-y-1">
        <h3 className="text-xl font-bold tracking-tight text-foreground">
          Resumen de la Reserva
        </h3>
        <p className="text-xs text-muted-foreground">
          Revisá los detalles de tu turno antes de confirmar la reserva.
        </p>
      </div>

      {/* Tarjeta 1: Confirma los datos de la reserva */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Confirma los datos de la reserva
            </h2>
          </div>
        </div>
        <div className="px-8 pb-8 pt-6">
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Cancha</dt>
              <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                {canchaNombre}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Fecha</dt>
              <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                {draft?.fecha || "—"}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Horario</dt>
              <dd className="mt-1 text-sm font-semibold font-mono tabular-nums text-foreground break-words">
                {horario}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Duración</dt>
              <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                {duracion} {duracion === 1 ? "hora" : "horas"}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Modalidad</dt>
              <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                {modalidadTexto}
              </dd>
            </div>
            <div className="min-w-0">
              <dt className="text-xs text-muted-foreground">Titular</dt>
              <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                {titularNombre || "Vos"}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      {/* Tarjeta 2: Jugadores */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Jugadores
            </h2>
          </div>
        </div>
        <div className="px-8 pb-8 pt-6">
          <div className="space-y-2.5">
            {/* Jugador Titular si está especificado */}
            {titularNombre && (
              <div className="flex items-center justify-between gap-3 rounded-lg border border-border p-3 bg-card">
                <div className="flex items-center gap-3 min-w-0">
                  <span className="inline-flex h-9 w-9 text-[11px] shrink-0 items-center justify-center rounded-full bg-muted font-bold text-foreground">
                    {obtenerIniciales(draft.titularNombre || "Titular", "")}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-foreground text-sm">
                      Titular: {draft.titularNombre || `Socio #${draft.titularId}`}
                    </p>
                    {draft.titularDni ? (
                      <p className="truncate text-xs text-muted-foreground font-mono">
                        DNI {draft.titularDni}
                      </p>
                    ) : (
                      <p className="truncate text-xs text-muted-foreground font-mono">
                        ID {draft.titularId}
                      </p>
                    )}
                  </div>
                </div>
                <span className="inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
                  Titular
                </span>
              </div>
            )}

            {/* Acompañantes */}
            {acompaniantes.length === 0 ? (
              <p className="text-xs text-muted-foreground italic py-2">
                Sin acompañantes registrados.
              </p>
            ) : (
              acompaniantes.map((a, idx) => {
                const nombreCompleto = `${a.nombre || ""} ${a.apellido || ""}`.trim();
                return (
                  <div
                    key={idx}
                    className="flex items-center justify-between gap-3 rounded-lg border border-border p-3 bg-card"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="inline-flex h-9 w-9 text-[11px] shrink-0 items-center justify-center rounded-full bg-muted font-bold text-foreground">
                        {obtenerIniciales(a.nombre, a.apellido)}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate font-semibold text-foreground text-sm">
                          {nombreCompleto || `Acompañante ${idx + 1}`}
                        </p>
                        {a.documento ? (
                          <p className="truncate text-xs text-muted-foreground font-mono">
                            DNI {a.documento}
                          </p>
                        ) : a.personaId ? (
                          <p className="truncate text-xs text-muted-foreground font-mono">
                            Socio #{a.personaId}
                          </p>
                        ) : null}
                      </div>
                    </div>
                    <span className="inline-flex items-center rounded-full whitespace-nowrap font-semibold px-2.5 py-1 text-xs bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]">
                      Acompañante
                    </span>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Tarjeta 3: Insumos asignados */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="flex min-w-0 items-center gap-1.5">
            <h2 className="text-lg font-bold text-foreground">
              Insumos asignados
            </h2>
            <InfoTip>Se descuentan del stock al iniciar el alquiler</InfoTip>
          </div>
        </div>
        <div className="px-8 pb-8 pt-6">
          {insumos.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Sin insumos seleccionados para este turno.
            </p>
          ) : (
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              {insumos.map((item, idx) => (
                <div key={idx} className="min-w-0">
                  <dt className="text-xs text-muted-foreground">{item.nombre}</dt>
                  <dd className="mt-1 text-sm font-semibold text-foreground break-words">
                    {item.cantidad} {Number(item.cantidad) === 1 ? "unidad" : "unidades"}
                  </dd>
                </div>
              ))}
              <div className="min-w-0">
                <dt className="text-xs text-muted-foreground">Reposición</dt>
                <dd className="mt-1 text-sm font-medium text-muted-foreground break-words">
                  Automática al cierre
                </dd>
              </div>
            </dl>
          )}
        </div>
      </div>

      {/* Tarjeta 4: Detalle económico */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Detalle económico
            </h2>
          </div>
        </div>
        <div className="px-8 pb-8 pt-6">
          <div className="space-y-2.5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">
                Alquiler {duracion} hora{duracion > 1 ? "s" : ""} - {canchaNombre}
              </span>
              <span className="font-mono tabular-nums font-semibold text-foreground">
                {formatearMoneda(subtotalCancha)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Insumos deportivos</span>
              <span className="font-mono tabular-nums font-semibold text-foreground">
                {subtotalInsumos > 0 ? formatearMoneda(subtotalInsumos) : "Sin cargo"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">
                Descuento aplicado
                {descuento ? ` (${descuento.codigo})` : ""}
              </span>
              <span
                className={
                  montoDescontado > 0
                    ? "font-mono tabular-nums font-semibold text-[var(--estado-ok-fg)]"
                    : "text-muted-foreground"
                }
              >
                {montoDescontado > 0 ? `- ${formatearMoneda(montoDescontado)}` : "—"}
              </span>
            </div>

            <div className="pt-1">
              <CodigoDeDescuento
                montoBase={subtotalCancha}
                aplicado={descuento}
                onAplicar={setDescuento}
                onQuitar={() => setDescuento(null)}
                deshabilitado={enviando}
              />
            </div>
            <div className="flex justify-between border-t border-border pt-2.5 text-base font-semibold">
              <span className="text-foreground">Total del alquiler</span>
              <span className="font-mono tabular-nums text-foreground">
                {formatearMoneda(totalEstimado)}
              </span>
            </div>
            <div className="flex justify-between rounded-lg bg-[var(--estado-brand-bg)] p-3 font-semibold text-[var(--estado-brand-fg)]">
              <span>Seña a pagar{reglas ? ` (${reglas.porcentajeSenia}%)` : ""}</span>
              <span className="font-mono tabular-nums">
                {seniaEstimada == null ? "—" : formatearMoneda(seniaEstimada)}
              </span>
            </div>
            <p className="px-1 text-xs text-muted-foreground">
              El saldo restante se cobrará al finalizar el partido.
            </p>
          </div>
        </div>
      </div>

      {/* Warning banner según mockup reserva-04-resumen.html L339 */}
      <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]">
        <AlertTriangle className="h-[18px] w-[18px] shrink-0" />
        <p className="text-sm font-semibold">
          La reserva no queda confirmada hasta registrar el pago
        </p>
      </div>

      {/* Error alert si ocurre fallo en API */}
      {error && (
        <div role="alert"><ErrorBanner>{error}</ErrorBanner></div>
      )}

      {/* Acciones de navegación */}
      <div className="flex items-center justify-between pt-4 border-t border-border">
        <button
          type="button"
          role="button"
          onClick={onVolver}
          disabled={enviando}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors border border-input bg-background hover:bg-muted px-8 py-3 cursor-pointer disabled:opacity-50"
        >
          <ArrowLeft className="h-[18px] w-[18px] shrink-0" />
          <span>Volver</span>
        </button>

        <button
          type="button"
          role="button"
          onClick={handleConfirmar}
          disabled={enviando}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors bg-primary text-primary-foreground hover:brightness-95 px-8 py-3 cursor-pointer shadow-xs disabled:pointer-events-none disabled:opacity-50"
        >
          {enviando && <Loader2 className="w-4 h-4 animate-spin" />}
          <span>{enviando ? "Confirmando reserva..." : "Confirmar reserva"}</span>
        </button>
      </div>
    </div>
  );
}

export default PasoResumen;
