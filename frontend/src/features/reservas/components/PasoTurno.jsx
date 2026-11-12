import { useState, useEffect, useMemo } from "react";
import { ArrowRight, Info } from "lucide-react";
import { consultarDisponibilidad } from "../api/reservasApi";
import { useReglasDeReserva } from "../hooks/useReglasDeReserva";
import { CampoLabel } from "@/components/ui/campo-label";
import { DateField } from "@/components/ui/date-field";
import { validarFechaAntelacion, extraerMensajeError, formatearFecha, turnoDisponible } from "../model/reserva";
import { GrillaDisponibilidadReserva } from "./GrillaDisponibilidadReserva";

function formatearFechaLocalIso(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Paso 1 del Wizard de Reservas: selección de fecha, duración y turno de cancha.
 * Sigue fielmente reserva-01-turno.html y reserva-01-error-30dias.html.
 */
export function PasoTurno({
  valor = {},
  canchasDisponibles = null,
  onSiguiente = () => {},
  onCambio = () => {},
}) {
  const [fecha, setFecha] = useState(valor?.fecha || "");
  const [duracionHoras, setDuracionHoras] = useState(valor?.duracionHoras || 1);
  const [seleccion, setSeleccion] = useState(
    valor?.canchaId && valor?.horaInicio
      ? {
          canchaId: valor.canchaId,
          canchaNombre: valor.canchaNombre || "",
          precioPorHora: valor.precioPorHora ?? null,
          horaInicio: valor.horaInicio,
        }
      : null
  );

  const [canchasApi, setCanchasApi] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [errorCarga, setErrorCarga] = useState(null);

  const reglas = useReglasDeReserva();
  const antelacion = reglas?.antelacionMaximaDias ?? null;
  const duracionMaxima = reglas?.duracionMaximaHoras ?? null;
  const { hoyIso, maxIso } = useMemo(() => {
    const hoy = new Date();
    const hoyIso = formatearFechaLocalIso(hoy);
    if (antelacion == null) return { hoyIso, maxIso: undefined };
    const max = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + antelacion);
    return { hoyIso, maxIso: formatearFechaLocalIso(max) };
  }, [antelacion]);

  // Validación de fecha contra antelación máxima (RD06)
  const validacionFecha = fecha ? validarFechaAntelacion(fecha, new Date(), antelacion) : { valida: false };
  const errorFecha = !fecha || validacionFecha.valida ? null : validacionFecha.error;
  const fechaPasada = Boolean(fecha) && fecha < hoyIso;
  const fueraDeAntelacion = Boolean(fecha && maxIso) && fecha > maxIso;

  // Si no se pasaron canchas mock/prop, consulta la grilla por API
  useEffect(() => {
    if (canchasDisponibles != null) return;
    if (!fecha || fechaPasada) {
      return;
    }

    let activo = true;
    setCargando(true);
    setErrorCarga(null);

    consultarDisponibilidad({ fecha })
      .then((data) => {
        if (!activo) return;
        const lista = Array.isArray(data) ? data : data?.canchas || [];
        setCanchasApi(lista);
      })
      .catch((err) => {
        if (!activo) return;
        setErrorCarga(extraerMensajeError(err, "No se pudo cargar la disponibilidad."));
      })
      .finally(() => {
        if (activo) setCargando(false);
      });

    return () => {
      activo = false;
    };
  }, [canchasDisponibles, fecha, fechaPasada]);

  const canchas = canchasDisponibles ?? (fecha && !fechaPasada ? canchasApi : []);

  useEffect(() => {
    onCambio({
      fecha,
      duracionHoras,
      canchaId: seleccion?.canchaId ?? null,
      canchaNombre: seleccion?.canchaNombre ?? "",
      precioPorHora: seleccion?.precioPorHora ?? null,
      horaInicio: seleccion?.horaInicio ?? "",
    });
  }, [fecha, duracionHoras, seleccion, onCambio]);

  const handleSeleccionarTurno = (slot) => {
    setSeleccion(slot);
  };

  const handleCambiarDuracion = (horas) => {
    setDuracionHoras(horas);
    if (!seleccion) return;
    const cancha = canchas.find((c) => (c.canchaId ?? c.id) === seleccion.canchaId);
    const franjas = cancha?.franjas ?? [];
    const indice = franjas.findIndex((f) => f.desde?.slice(0, 5) === seleccion.horaInicio);
    if (!turnoDisponible(franjas, indice, { duracionHoras: horas, fecha })) setSeleccion(null);
  };

  const puedeContinuar =
    Boolean(fecha) &&
    validacionFecha.valida &&
    Boolean(seleccion?.canchaId) &&
    Boolean(seleccion?.horaInicio);

  const handleContinuar = () => {
    if (!puedeContinuar) return;
    onSiguiente({
      fecha,
      canchaId: seleccion.canchaId,
      canchaNombre: seleccion.canchaNombre,
      precioPorHora: seleccion.precioPorHora,
      horaInicio: seleccion.horaInicio,
      duracionHoras,
    });
  };

  return (
    <div className="space-y-5">
      {/* Tarjeta 1: Fecha y cancha */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <h2 className="min-w-0 text-lg font-bold text-foreground">
            Fecha y duración
          </h2>
        </div>

        <div className="px-8 pb-8 pt-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <CampoLabel htmlFor="fecha-reserva" info={maxIso ? `Podés reservar hasta el ${formatearFecha(maxIso)}.` : undefined}>
                Fecha de reserva
              </CampoLabel>
              <DateField
                id="fecha-reserva"
                value={fecha}
                min={hoyIso}
                onChange={(e) => {
                  setFecha(e.target.value);
                  setSeleccion(null);
                }}
              />
              {errorFecha ? (
                <p role="alert" className="text-xs font-semibold text-[var(--estado-error-fg)]">
                  {errorFecha}
                </p>
              ) : null}
            </div>

            <div className="space-y-1.5">
              <CampoLabel info={duracionMaxima ? `Máximo ${duracionMaxima} ${duracionMaxima === 1 ? "hora" : "horas"} por turno.` : undefined}>Duración del turno</CampoLabel>
              <div className="flex gap-2">
                {Array.from({ length: duracionMaxima ?? 1 }, (_, i) => i + 1).map((horas) => (
                  <button
                    key={horas}
                    type="button"
                    role="button"
                    aria-pressed={duracionHoras === horas}
                    onClick={() => handleCambiarDuracion(horas)}
                    className={`flex-1 rounded-full py-3.5 text-[15px] font-medium transition-all cursor-pointer ${
                      duracionHoras === horas
                        ? "bg-primary text-primary-foreground font-semibold shadow-xs"
                        : "border border-input bg-background text-foreground hover:bg-muted"
                    }`}
                  >
                    {horas} {horas === 1 ? "hora" : "horas"}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tarjeta 2: Horarios disponibles */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-foreground">
              Horarios disponibles
            </h2>
          </div>
        </div>

        <div className="px-8 pb-8 pt-6">
          {!fecha ? (
            <div className="py-6 text-center text-sm text-muted-foreground">
              Elegí una fecha para ver los horarios.
            </div>
          ) : !fecha || fechaPasada ? (
            <div className="py-6 text-center text-sm text-muted-foreground">
              Elegí una fecha válida para ver los turnos disponibles.
            </div>
          ) : (
            <GrillaDisponibilidadReserva
              canchas={canchas}
              duracionHoras={duracionHoras}
              fecha={fecha}
              soloConsulta={fueraDeAntelacion}
              seleccion={seleccion}
              onSeleccionarTurno={handleSeleccionarTurno}
              cargando={cargando}
              error={errorCarga}
            />
          )}

          {/* Banner de turno seleccionado según reserva-01-turno.html L276 */}
          {seleccion?.horaInicio && (
            <div className="mt-4 space-y-3 border-t border-border pt-4">
              <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
                <Info className="h-[18px] w-[18px] shrink-0" />
                <p className="text-sm font-semibold">
                  Turno seleccionado: {seleccion.horaInicio} {seleccion.canchaNombre ? `en ${seleccion.canchaNombre}` : ""} ({duracionHoras} {duracionHoras === 1 ? "hora" : "horas"})
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Botón de acción Continuar */}
      <div className="flex items-center justify-end gap-4 border-t border-border pt-4">
        <button
          type="button"
          role="button"
          disabled={!puedeContinuar}
          onClick={handleContinuar}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-40 bg-primary text-primary-foreground hover:brightness-95 px-8 py-3 cursor-pointer shadow-xs"
        >
          <span>Continuar</span>
          <ArrowRight className="h-[18px] w-[18px] shrink-0" />
        </button>
      </div>
    </div>
  );
}

export default PasoTurno;
