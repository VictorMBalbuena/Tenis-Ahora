import { EntrenamientoDelSocio } from "../components/EntrenamientosDelSocio";
import { useAuthStore } from "@/store/authStore";
import { useCallback, useEffect, useState } from "react";
import { ClipboardCheck, Pencil, Printer, UserMinus, UserPlus } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SuccessBanner } from "@/components/ui/success-banner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { mensajeDeError } from "@/lib/erroresApi";
import {
  desinscribirAlumno,
  imprimirEntrenamiento,
  listarInscriptos,
  obtenerEntrenamiento,
} from "../api/entrenamientosApi";
import {
  ETIQUETA_ESTADO,
  ETIQUETA_MODALIDAD,
  admiteAsistencia,
  admiteInscripcion,
  cupoCompleto,
  diasTexto,
  fechaCorta,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreEntrenador,
  porcentajeOcupado,
  rangoFechas,
} from "../model/entrenamiento";
import { InscribirAlumnoDialog } from "../components/InscribirAlumnoDialog";

/**
 * Detalle de un entrenamiento (CU39) con sus alumnos inscriptos.
 *
 * Desde acá se inscribe (CU82), se da de baja una inscripción y se entra a la
 * planilla de asistencia (CU43). Es también la vista que usa el entrenador
 * para ver sus alumnos (CU84).
 */
function EntrenamientoDetalleDelPersonal() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((estado) => estado.user);
  const esEntrenador = user?.rol === "ENTRENADOR";

  const [entrenamiento, setEntrenamiento] = useState(null);
  const [inscriptos, setInscriptos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mensajeError, setMensajeError] = useState("");
  const [aviso, setAviso] = useState(location.state?.aviso ?? "");
  const [inscribiendo, setInscribiendo] = useState(false);
  const [quitando, setQuitando] = useState(null);

  const cargar = useCallback(() => {
    let vigente = true;

    Promise.all([obtenerEntrenamiento(id), listarInscriptos(id).catch(() => [])])
      .then(([datos, lista]) => {
        if (!vigente) return;
        setEntrenamiento(datos);
        setInscriptos(Array.isArray(lista) ? lista : []);
      })
      .catch((error) => {
        if (vigente) setMensajeError(mensajeDeError(error, "No se pudo cargar el entrenamiento."));
      })
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, [id]);

  useEffect(() => cargar(), [cargar]);

  async function imprimir() {
    setMensajeError("");
    try {
      const blob = await imprimirEntrenamiento(id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) {
      setMensajeError(mensajeDeError(error, "No se pudo generar el PDF del entrenamiento."));
    }
  }

  async function quitar(inscripcion) {
    setMensajeError("");
    setQuitando(inscripcion.socioId);
    try {
      await desinscribirAlumno(id, inscripcion.socioId);
      setAviso("Se liberó el lugar del alumno.");
      cargar();
    } catch (error) {
      setMensajeError(mensajeDeError(error, "No se pudo dar de baja la inscripción."));
    } finally {
      setQuitando(null);
    }
  }

  if (cargando) {
    return <p className="py-10 text-center text-sm text-muted-foreground">Cargando el entrenamiento...</p>;
  }

  if (!entrenamiento) {
    return (
      <div className="space-y-6">
        <VolverButton to="/entrenamientos" />
        <ErrorBanner>{mensajeError || "No se encontró el entrenamiento."}</ErrorBanner>
      </div>
    );
  }

  const datos = [
    ["Vigencia", rangoFechas(entrenamiento)],
    ["Días", diasTexto(entrenamiento)],
    ["Horario", horario(entrenamiento)],
    ...(!esEntrenador ? [["Entrenador", nombreEntrenador(entrenamiento)]] : []),
    ["Cancha", nombreCancha(entrenamiento)],
    ["Modalidad", ETIQUETA_MODALIDAD[entrenamiento.modalidad] ?? entrenamiento.modalidad],
    ["Estado", ETIQUETA_ESTADO[entrenamiento.estado] ?? entrenamiento.estado],
    ["Objetivo", entrenamiento.objetivo || "—"],
  ];

  const volverA = location.state?.from || "/entrenamientos";

  return (
    <div className="space-y-6">
      <VolverButton to={volverA} />

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{entrenamiento.nombre}</h1>
            <Badge variant="outline" className="font-normal text-xs">
              {ETIQUETA_ESTADO[entrenamiento.estado] ?? entrenamiento.estado}
            </Badge>
            {entrenamiento.modalidad && (
              <Badge variant="secondary" className="font-normal text-xs">
                {ETIQUETA_MODALIDAD[entrenamiento.modalidad] ?? entrenamiento.modalidad}
              </Badge>
            )}
          </div>
          <p className="text-xs sm:text-sm text-muted-foreground tabular-nums">
            {diasTexto(entrenamiento)} &middot; {horario(entrenamiento)}
          </p>
        </div>

        <div className="flex w-full sm:w-auto flex-wrap items-center gap-2.5">
          <Button
            variant={esEntrenador ? "default" : "outline"}
            className="h-11 sm:h-10 flex-1 sm:flex-initial gap-2 text-sm font-medium touch-manipulation"
            onClick={() =>
              navigate(`/entrenamientos/${entrenamiento.id}/asistencia`, { state: { from: location.pathname } })
            }
            disabled={!admiteAsistencia(entrenamiento)}
            title={!admiteAsistencia(entrenamiento) ? "El entrenamiento está cancelado" : undefined}
          >
            <ClipboardCheck className="size-4" />
            Asistencia
          </Button>
          <Button variant="outline" className="h-11 sm:h-10 gap-2 text-sm touch-manipulation" onClick={imprimir}>
            <Printer className="size-4" />
            Imprimir
          </Button>
          {!esEntrenador && (
            <Button
              className="h-11 sm:h-10 gap-2 text-sm touch-manipulation"
              onClick={() =>
                navigate(`/entrenamientos/editar/${entrenamiento.id}`, { state: { from: location.pathname } })
              }
            >
              <Pencil className="size-4" />
              Modificar
            </Button>
          )}
        </div>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>
      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <div className="grid gap-4 sm:gap-5 md:grid-cols-2">
        <section className="rounded-2xl border border-border bg-card p-4 sm:p-6 shadow-xs">
          <h2 className="mb-4 text-base sm:text-lg font-bold">Datos del entrenamiento</h2>
          <dl className="divide-y divide-border text-sm">
            {datos.map(([clave, valor]) => (
              <div
                key={clave}
                className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-1 sm:gap-4 py-2.5"
              >
                <dt className="text-xs sm:text-sm font-medium text-muted-foreground shrink-0">{clave}</dt>
                <dd className="text-xs sm:text-sm font-medium text-foreground sm:text-right break-words">{valor}</dd>
              </div>
            ))}
          </dl>
        </section>

        <section className="rounded-2xl border border-border bg-card p-4 sm:p-6 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between gap-2 mb-4">
              <h2 className="text-base sm:text-lg font-bold">Ocupación del cupo</h2>
              <Badge variant={cupoCompleto(entrenamiento) ? "destructive" : "secondary"} className="font-medium text-xs">
                {cupoCompleto(entrenamiento) ? "Completo" : `${lugaresLibres(entrenamiento)} libres`}
              </Badge>
            </div>

            <div className="space-y-3">
              <div className="flex items-baseline justify-between">
                <span className="font-mono text-3xl sm:text-4xl font-semibold tabular-nums text-foreground">
                  {entrenamiento.cupoOcupado ?? entrenamiento.inscriptos ?? 0}
                  <span className="text-base sm:text-lg font-normal text-muted-foreground ml-1">
                    / {entrenamiento.cupoMaximo}
                  </span>
                </span>
                <span className="font-mono text-sm text-muted-foreground tabular-nums">
                  {porcentajeOcupado(entrenamiento)}%
                </span>
              </div>

              <div aria-hidden="true" className="h-2.5 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className={cn(
                    "h-full rounded-full transition-all duration-300",
                    cupoCompleto(entrenamiento) ? "bg-red-500" : "bg-primary",
                  )}
                  style={{ width: `${porcentajeOcupado(entrenamiento)}%` }}
                />
              </div>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-border/60">
            {cupoCompleto(entrenamiento) ? (
              <p className="text-xs sm:text-sm text-destructive font-medium">
                El cupo máximo está cubierto ({entrenamiento.cupoMaximo} alumnos). Para inscribir a otro socio se debe liberar un lugar.
              </p>
            ) : (
              <p className="text-xs sm:text-sm text-muted-foreground">
                Quedan <strong className="text-foreground">{lugaresLibres(entrenamiento)} {lugaresLibres(entrenamiento) === 1 ? "lugar disponible" : "lugares disponibles"}</strong> para nuevas inscripciones.
              </p>
            )}
          </div>
        </section>
      </div>

      <section className="rounded-2xl border border-border bg-card p-4 sm:p-6 shadow-xs">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-base sm:text-lg font-bold">
            Alumnos inscriptos{" "}
            <Badge variant="outline" className="ml-1 font-normal">
              {inscriptos.length}
            </Badge>
          </h2>
          {!esEntrenador && (
            <Button
              onClick={() => setInscribiendo(true)}
              disabled={!admiteInscripcion(entrenamiento)}
              title={cupoCompleto(entrenamiento) ? "El cupo está completo" : undefined}
            >
              <UserPlus />
              Inscribir alumno
            </Button>
          )}
        </div>

        {inscriptos.length === 0 ? (
          <p className="rounded-lg border border-dashed border-border py-8 text-center text-sm text-muted-foreground">
            Todavía no hay alumnos inscriptos.
          </p>
        ) : (
          <ul className="divide-y divide-border">
            {inscriptos.map((i) => (
              <li key={i.socioId} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{i.socio}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    DNI {i.dni} &middot; inscripto el {fechaCorta(i.fechaInscripcion)}
                  </p>
                </div>
                {i.estado === "PENDIENTE_PAGO" && i.inscripcionId && !esEntrenador && (
                  <Link to={`/inscripciones/${i.inscripcionId}/cobro`} className="shrink-0 text-sm font-semibold text-primary hover:underline">
                    Cobrar seña
                  </Link>
                )}
                {!esEntrenador && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-10 w-10 text-destructive hover:bg-destructive/10 touch-manipulation"
                    aria-label={`Dar de baja a ${i.socio}`}
                    disabled={quitando === i.socioId}
                    onClick={() => quitar(i)}
                  >
                    <UserMinus className="size-4" />
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <InscribirAlumnoDialog
        key={`inscribir-${inscribiendo}-${entrenamiento.cupoOcupado ?? 0}`}
        abierto={inscribiendo}
        entrenamiento={entrenamiento}
        onCerrar={() => setInscribiendo(false)}
        onInscripto={(mensaje) => {
          setInscribiendo(false);
          setAviso(mensaje);
          cargar();
        }}
      />
    </div>
  );
}

export default function EntrenamientoDetallePage() {
  const rol = useAuthStore((estado) => estado.user?.rol);
  return rol === "SOCIO" ? <EntrenamientoDelSocio /> : <EntrenamientoDetalleDelPersonal />;
}
