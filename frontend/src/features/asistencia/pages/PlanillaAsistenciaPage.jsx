import { hoyLocal } from "@/lib/fechas";
import { useCallback, useEffect, useState } from "react";
import { AlertTriangle, Calendar, Check, Clock, Save, X } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SuccessBanner } from "@/components/ui/success-banner";
import { Button } from "@/components/ui/button";
import { DateField } from "@/components/ui/date-field";
import { CampoFormulario } from "@/components/ui/campo-formulario";
import { mensajeDeError } from "@/lib/erroresApi";
import { obtenerClase } from "@/features/clases/api/clasesApi";
import { obtenerEntrenamiento } from "@/features/entrenamientos/api/entrenamientosApi";
import { guardarPlanilla, obtenerPlanilla } from "../api/asistenciaApi";
import {
  alternar,
  aMarcas,
  contarAusentes,
  contarPresentes,
  fechaSugeridaAsistencia,
  filasDesdePlanilla,
  formatearFechaLocal,
  marcarTodos,
  porcentajePresentes,
  ultimaFechaDictado,
  validarFechaAsistencia,
} from "../model/asistencia";
import { KpiCard } from "@/components/ui/kpi-card";

const hoyIso = hoyLocal;

const hhmm = (h) => (h ? String(h).slice(0, 5) : "—");

/**
 * Planilla de asistencia de una clase (CU42) o de un entrenamiento (CU43).
 *
 * Es la misma pantalla para las dos, porque en el backend la asistencia
 * cuelga de Actividad, que es la clase base de ambas. Lo único que cambia es
 * de dónde se trae la cabecera y a dónde vuelve el botón.
 */
export default function PlanillaAsistenciaPage({ tipo = "clase" }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const esClase = tipo === "clase";
  const volverA = location.state?.from || (esClase ? `/clases/${id}` : `/entrenamientos/${id}`);

  const [actividad, setActividad] = useState(null);
  const [fecha, setFecha] = useState(hoyIso());
  const [filas, setFilas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [mensajeError, setMensajeError] = useState("");
  const [aviso, setAviso] = useState("");

  // La cabecera se trae una sola vez: no depende de la fecha elegida.
  useEffect(() => {
    let vigente = true;
    const traer = esClase ? obtenerClase : obtenerEntrenamiento;

    traer(id)
      .then((datos) => {
        if (!vigente) return;
        setActividad(datos);
        const sugerencia = fechaSugeridaAsistencia(datos);
        if (sugerencia.esPeriodoGracia && sugerencia.fecha) {
          setFecha(sugerencia.fecha);
        }
      })
      .catch((error) => {
        if (vigente) setMensajeError(mensajeDeError(error, "No se pudo cargar la actividad."));
      });

    return () => {
      vigente = false;
    };
  }, [id, esClase]);

  // El estado se toca solo dentro del `then`, nunca de forma sincronica en el
  // cuerpo del efecto: es lo que pide la regla set-state-in-effect del lint.
  // Mientras llega la planilla nueva se siguen viendo las filas anteriores.
  const cargarPlanilla = useCallback(() => {
    let vigente = true;

    obtenerPlanilla(id, fecha)
      .then((datos) => vigente && setFilas(filasDesdePlanilla(datos)))
      .catch((error) => {
        if (!vigente) return;
        setFilas([]);
        setMensajeError(mensajeDeError(error, "No se pudo cargar la planilla."));
      })
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, [id, fecha]);

  useEffect(() => cargarPlanilla(), [cargarPlanilla]);

  const validacionFecha = validarFechaAsistencia(actividad, fecha);
  const sugerencia = fechaSugeridaAsistencia(actividad);
  const esGraciaActiva = sugerencia.esPeriodoGracia && sugerencia.fecha === fecha;
  const ultimaFecha = !validacionFecha.valida ? ultimaFechaDictado(actividad, fecha) : null;

  async function guardar() {
    if (!validacionFecha.valida) return;
    setMensajeError("");
    setGuardando(true);
    try {
      await guardarPlanilla(id, { fecha, marcas: aMarcas(filas) });
      setAviso(`Se registró la asistencia: ${contarPresentes(filas)} de ${filas.length} presentes.`);
    } catch (error) {
      setMensajeError(mensajeDeError(error, "No se pudo registrar la asistencia."));
    } finally {
      setGuardando(false);
    }
  }

  const presentes = contarPresentes(filas);
  const ausentes = contarAusentes(filas);

  return (
    <div className="space-y-6">
      <VolverButton to={volverA} />

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Asistencia {esClase ? "a la clase" : "al entrenamiento"}
          </h1>
          {actividad && (
            <p className="mt-1 text-sm text-muted-foreground">
              {hhmm(actividad.horaInicio)} a {hhmm(actividad.horaFin)} &middot;{" "}
              {actividad.cancha ?? "—"}
            </p>
          )}
        </div>
        <Button
          onClick={guardar}
          disabled={guardando || filas.length === 0 || !validacionFecha.valida}
          title={!validacionFecha.valida ? validacionFecha.motivo : undefined}
        >
          <Save />
          {guardando ? "Guardando..." : "Registrar asistencia"}
        </Button>
      </header>

      {esGraciaActiva && (
        <div
          role="status"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-sm text-blue-950 dark:text-blue-200"
        >
          <div className="flex items-center gap-2.5">
            <Clock className="size-5 shrink-0 text-blue-600 dark:text-blue-400" />
            <div>
              <p className="font-semibold">Período de gracia nocturno activo</p>
              <p className="text-xs opacity-90">
                Se preseleccionó la clase de anoche ({formatearFechaLocal(fecha)}) para registrar la asistencia pendiente.
              </p>
            </div>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => {
              setFecha(hoyLocal());
              setAviso("");
            }}
            className="h-8 shrink-0 text-xs"
          >
            Ir a hoy
          </Button>
        </div>
      )}

      {!validacionFecha.valida && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-900 dark:text-amber-200"
        >
          <div className="flex items-start gap-3">
            <AlertTriangle className="size-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5" />
            <div className="space-y-0.5">
              <p className="font-semibold">No se puede registrar asistencia en esta fecha</p>
              <p className="text-xs opacity-90">{validacionFecha.motivo}</p>
            </div>
          </div>
          {ultimaFecha && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setFecha(ultimaFecha);
                setAviso("");
              }}
              className="h-8 gap-1.5 text-xs border-amber-600/30 text-amber-900 dark:text-amber-200 hover:bg-amber-500/20"
            >
              <Calendar className="size-3.5" />
              Ir a la última clase ({formatearFechaLocal(ultimaFecha)})
            </Button>
          )}
        </div>
      )}

      <SuccessBanner>
        {aviso && (
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span>{aviso}</span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => navigate(volverA)}
              className="h-8 border-current bg-transparent text-current hover:bg-black/5 dark:hover:bg-white/5"
            >
              Volver a la actividad
            </Button>
          </div>
        )}
      </SuccessBanner>
      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <div className="grid gap-5 lg:grid-cols-[320px_1fr]">
        <section className="space-y-5">
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <CampoFormulario id="asistencia-fecha" etiqueta="Fecha de la planilla">
              {() => (
                <DateField
                  name="fecha"
                  value={fecha}
                  onChange={(e) => {
                    setFecha(e.target.value);
                    setAviso("");
                  }}
                />
              )}
            </CampoFormulario>
            <p className="mt-2 text-xs text-muted-foreground">
              Al cambiar la fecha se carga la planilla de ese día.
            </p>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <KpiCard etiqueta="Presentes" valor={presentes} valorClase="text-[var(--estado-ok-fg)]" />
            <KpiCard etiqueta="Ausentes" valor={ausentes} valorClase="text-muted-foreground" />
            <KpiCard etiqueta="Asistencia" valor={<>{porcentajePresentes(filas)}%</>} />
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-bold">Alumnos</h2>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={!validacionFecha.valida || filas.length === 0}
                onClick={() => setFilas(marcarTodos(filas, true))}
              >
                <Check className="size-4" />
                Todos presentes
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={!validacionFecha.valida || filas.length === 0}
                onClick={() => setFilas(marcarTodos(filas, false))}
              >
                <X className="size-4" />
                Todos ausentes
              </Button>
            </div>
          </div>

          {cargando && <p className="py-8 text-center text-sm text-muted-foreground">Cargando la planilla...</p>}

          {!cargando && filas.length === 0 && (
            <p className="rounded-lg border border-dashed border-border py-10 text-center text-sm text-muted-foreground">
              No hay alumnos inscriptos para esta fecha.
            </p>
          )}

          {!cargando && filas.length > 0 && (
            <ul className="divide-y divide-border">
              {filas.map((f) => (
                <li key={f.socioId} className="flex items-center justify-between gap-3 py-1">
                  <label className="flex min-h-[48px] min-w-0 flex-1 cursor-pointer items-center gap-3.5 py-2 px-1 rounded-lg active:bg-muted/40 transition-colors">
                    <input
                      type="checkbox"
                      checked={f.presente}
                      disabled={!validacionFecha.valida}
                      onChange={() => setFilas(alternar(filas, f.socioId))}
                      className="size-5 shrink-0 accent-primary cursor-pointer touch-manipulation disabled:opacity-50 disabled:cursor-not-allowed"
                      aria-label={`Marcar a ${f.nombre} como presente`}
                    />
                    <span className="min-w-0 block truncate text-sm font-medium text-foreground">{f.nombre}</span>
                  </label>
                  <span
                    className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      f.presente
                        ? "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]"
                        : "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]"
                    }`}
                  >
                    {f.presente ? "Presente" : "Ausente"}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <div className="flex justify-end">
        <Button variant="outline" onClick={() => navigate(volverA)}>
          Volver sin guardar
        </Button>
      </div>
    </div>
  );
}
