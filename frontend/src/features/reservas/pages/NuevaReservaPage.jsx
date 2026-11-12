import { useCallback } from "react";
import { useNavigate, useLocation, useSearchParams } from "react-router-dom";
import { Check } from "lucide-react";
import { VolverButton } from "@/components/ui/volver-button";
import { PasoTurno } from "../components/PasoTurno";
import { PasoJugadores } from "../components/PasoJugadores";
import { PasoInsumos } from "../components/PasoInsumos";
import { PasoResumen } from "../components/PasoResumen";
import { ReservaConfirmadaModal } from "../components/ReservaConfirmadaModal";
import { useReglasDeReserva } from "../hooks/useReglasDeReserva";
import { useReservaWizard } from "../hooks/useReservaWizard";
import { formatearFecha, formatearMoneda, horaFinDe } from "../model/reserva";

const PASOS = [
  { numero: 1, label: "Turno", titulo: "Turno" },
  { numero: 2, label: "Jugadores", titulo: "Jugadores" },
  { numero: 3, label: "Insumos", titulo: "Insumos" },
  { numero: 4, label: "Resumen", titulo: "Resumen" },
];

// Pantalla principal del Wizard de Nueva Reserva (FRONT 06 / SCRUM-34)
// Siguiendo los mockups reserva-01-turno.html a reserva-04-resumen.html
export function NuevaReservaPage({ initialDraft, initialPaso = 1 } = {}) {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const draftFromParams = {
    ...initialDraft,
    ...(location.state || {}),
    ...(searchParams.get("canchaId") ? { canchaId: Number(searchParams.get("canchaId")) } : {}),
    ...(searchParams.get("fecha") ? { fecha: searchParams.get("fecha") } : {}),
    ...(searchParams.get("hora") ? { horaInicio: searchParams.get("hora") } : {}),
    ...(searchParams.get("canchaNombre") ? { canchaNombre: searchParams.get("canchaNombre") } : {}),
  };

  const {
    pasoActual,
    draft,
    reservaCreada,
    modalExitoOpen,
    avanzarPaso,
    retrocederPaso,
    irAPaso,
    abrirExito,
    cerrarExito,
    setDraft,
  } = useReservaWizard(draftFromParams, initialPaso);

  const actualizarDraft = useCallback((parcial) => setDraft((prev) => ({ ...prev, ...parcial })), [setDraft]);

  const handleCerrarModal = () => {
    cerrarExito();
    navigate("/reservas");
  };

  const handlePagarModal = () => {
    cerrarExito();
    navigate(
      reservaCreada?.id ? `/reservas/${reservaCreada.id}/cobro` : "/reservas",
    );
  };

  // Cálculo de subtotales para la columna de resumen lateral
  const duracion = Number(draft?.duracionHoras || 1);
  const precioHora = Number(draft?.precioPorHora || 0);
  const subtotalCancha = precioHora * duracion;
  const insumos = Array.isArray(draft?.insumos) ? draft.insumos : [];
  const subtotalInsumos = insumos.reduce(
    (acc, it) =>
      acc + Number(it.cantidad || 0) * Number(it.precioUnitario || 0),
    0,
  );
  const totalEstimado = subtotalCancha + subtotalInsumos;
  const reglas = useReglasDeReserva();
  const seniaEstimada = reglas ? (totalEstimado * reglas.porcentajeSenia) / 100 : null;

  return (
    <div data-testid="nueva-reserva-page" className="w-full min-w-0 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">Nueva reserva</h1>
        <VolverButton to="/reservas" />
      </div>

      {/* Stepper bar interactivo conectado con líneas según mockup */}
      <nav
        aria-label="Progreso de la reserva"
        className="overflow-x-auto scroll-thin pb-1"
      >
        <ol className="flex w-full min-w-max items-center gap-2">
          {PASOS.map((paso, idx) => {
            const esActivo = paso.numero === pasoActual;
            const esCompletado = paso.numero < pasoActual;
            const esPendiente = paso.numero > pasoActual;

            return (
              <li key={paso.numero} className={`flex items-center gap-2 ${idx < PASOS.length - 1 ? "flex-1" : ""}`}>
                <button
                  type="button"
                  disabled={esPendiente || esActivo}
                  onClick={() => esCompletado && irAPaso(paso.numero)}
                  aria-current={esActivo ? "step" : undefined}
                  className={`flex items-center gap-2 whitespace-nowrap rounded-full px-2 py-1 text-sm transition-colors ${
                    esActivo
                      ? "font-bold text-foreground cursor-default"
                      : esCompletado
                        ? "font-medium text-foreground hover:bg-muted cursor-pointer"
                        : "text-muted-foreground opacity-60 cursor-not-allowed"
                  }`}
                >
                  <span
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold transition-all ${
                      esActivo
                        ? "border-2 border-primary text-foreground shadow-xs"
                        : esCompletado
                          ? "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
                          : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {esCompletado ? (
                      <Check className="w-4 h-4 stroke-[2.5]" />
                    ) : (
                      paso.numero
                    )}
                  </span>
                  <span>{paso.label}</span>
                </button>
                {idx < PASOS.length - 1 && (
                  <span className="h-px min-w-6 flex-1 bg-input" />
                )}
              </li>
            );
          })}
        </ol>
      </nav>

      {/* Layout de 2 columnas según mockups reserva-01 a reserva-04 */}
      <div
        className={`grid grid-cols-1 gap-5 ${pasoActual < 4 ? "lg:grid-cols-3" : ""}`}
      >
        {/* Columna principal del paso actual */}
        <main
          className={`min-w-0 space-y-5 ${pasoActual < 4 ? "lg:col-span-2" : "w-full"}`}
        >
          {pasoActual === 1 && (
            <PasoTurno valor={draft} onSiguiente={avanzarPaso} onCambio={actualizarDraft} />
          )}

          {pasoActual === 2 && (
            <PasoJugadores
              valor={draft}
              onVolver={retrocederPaso}
              onSiguiente={avanzarPaso}
            />
          )}

          {pasoActual === 3 && (
            <PasoInsumos
              valor={draft}
              turno={draft}
              onVolver={retrocederPaso}
              onSiguiente={avanzarPaso}
            />
          )}

          {pasoActual === 4 && (
            <PasoResumen
              draft={draft}
              onVolver={retrocederPaso}
              onConfirmarExitoso={abrirExito}
            />
          )}
        </main>

        {/* Columna lateral: Tarjeta de Resumen en vivo para pasos 1 a 3 */}
        {pasoActual < 4 && (
          <aside className="min-w-0 space-y-4 lg:sticky lg:top-0 lg:self-start">
            <div className="rounded-xl border border-border bg-card shadow-sm">
              <div className="flex items-start justify-between gap-4 px-8 pt-8">
                <div className="min-w-0">
                  <h2 className="text-lg font-bold text-foreground">
                    Resumen
                  </h2>
                </div>
              </div>

              <div className="px-8 pb-8 pt-6">
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between gap-3">
                    <span className="text-muted-foreground">Cancha</span>
                    <span className="text-right font-medium text-foreground">
                      {draft?.canchaNombre ||
                        (draft?.canchaId ? `Cancha ${draft.canchaId}` : "—")}
                    </span>
                  </div>

                  <div className="flex justify-between gap-3">
                    <span className="text-muted-foreground">Fecha</span>
                    <span className="text-right font-medium text-foreground">
                      {draft?.fecha ? formatearFecha(draft.fecha) : "—"}
                    </span>
                  </div>

                  <div className="flex justify-between gap-3">
                    <span className="text-muted-foreground">Horario</span>
                    <span className="text-right font-medium font-mono tabular-nums text-foreground">
                      {draft?.horaInicio ? `${draft.horaInicio} a ${horaFinDe(draft.horaInicio, duracion)}` : "—"}
                    </span>
                  </div>

                  <div className="flex justify-between gap-3">
                    <span className="text-muted-foreground">Duración</span>
                    <span className="text-right font-medium text-foreground">
                      {duracion} {duracion === 1 ? "hora" : "horas"}
                    </span>
                  </div>

                  <div className="flex justify-between gap-3">
                    <span className="text-muted-foreground">Jugadores</span>
                    <span className="text-right font-medium text-foreground">
                      {draft?.modalidad === "DOBLES"
                        ? "4 (dobles)"
                        : "2 (single)"}
                    </span>
                  </div>

                  <div className="space-y-2 border-t border-border pt-3">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Subtotal</span>
                      <span className="font-mono tabular-nums font-medium text-foreground">
                        {formatearMoneda(subtotalCancha)}
                      </span>
                    </div>
                    {subtotalInsumos > 0 && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Insumos</span>
                        <span className="font-mono tabular-nums font-medium text-foreground">
                          {formatearMoneda(subtotalInsumos)}
                        </span>
                      </div>
                    )}
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Descuento</span>
                      <span className="text-muted-foreground">—</span>
                    </div>
                    <div className="flex justify-between text-base font-semibold pt-1 border-t border-border/40">
                      <span className="text-foreground">Total</span>
                      <span className="font-mono tabular-nums text-foreground">
                        {formatearMoneda(totalEstimado)}
                      </span>
                    </div>
                  </div>

                  <div className="rounded-lg bg-[var(--estado-brand-bg)] p-3 mt-2">
                    <div className="flex justify-between font-medium text-[var(--estado-brand-fg)]">
                      <span>Seña a abonar{reglas ? ` (${reglas.porcentajeSenia}%)` : ""}</span>
                      <span className="font-mono tabular-nums font-semibold">
                        {seniaEstimada == null ? "—" : formatearMoneda(seniaEstimada)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-[var(--estado-brand-fg)] opacity-90">
                      El saldo restante se cobra al finalizar el partido.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </aside>
        )}
      </div>

      {/* Modal de confirmación final */}
      <ReservaConfirmadaModal
        abierta={modalExitoOpen}
        reserva={reservaCreada}
        onCerrar={handleCerrarModal}
        onPagar={handlePagarModal}
      />
    </div>
  );
}

export default NuevaReservaPage;
