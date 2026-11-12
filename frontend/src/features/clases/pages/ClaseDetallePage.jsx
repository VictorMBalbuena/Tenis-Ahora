import { ClaseDelSocio } from "../components/ClasesDelSocio";
import { useAuthStore } from "@/store/authStore";
import { useCallback, useEffect, useState } from "react";
import { ClipboardCheck, Pencil, Printer, UserCog, UserMinus, UserPlus } from "lucide-react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { ErrorBanner } from "@/components/ui/error-banner";
import { SuccessBanner } from "@/components/ui/success-banner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
//import { useAuthStore } from "@/store/authStore";
import { mensajeDeError } from "@/lib/erroresApi";
import {
  desinscribirAlumno,
  imprimirClase,
  listarInscriptos,
  obtenerClase,
} from "../api/clasesApi";
import {
  ETIQUETA_MODALIDAD,
  ETIQUETA_NIVEL,
  Modalidad,
  admiteAsistencia,
  admiteInscripcion,
  cupoCompleto,
  diasCortos,
  fechaCorta,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreProfesor,
  vigencia,
} from "../model/clase";
import { CupoBadge } from "../components/ClasesTabla";
import { InscribirAlumnoDialog } from "../components/InscribirAlumnoDialog";
import { AsignarInstructorDialog } from "../components/AsignarInstructorDialog";

/**
 * Detalle de una clase (CU34) con sus alumnos inscriptos.
 *
 * Desde acá se inscribe (CU81) y se da de baja una inscripción. Es también la
 * vista que usa el profesor para ver sus alumnos (CU82).
 */
function ClaseDetalleDelPersonal() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const usuario = useAuthStore((estado) => estado.user);
  const esProfesor = usuario?.rol === "PROFESOR";
  const volverA = location.state?.from || "/clases";

  const [clase, setClase] = useState(null);
  const [inscriptos, setInscriptos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mensajeError, setMensajeError] = useState("");
  const [aviso, setAviso] = useState(location.state?.aviso ?? "");
  const [inscribiendo, setInscribiendo] = useState(false);
  const [asignando, setAsignando] = useState(false);
  const [quitando, setQuitando] = useState(null);

  const cargar = useCallback(() => {
    let vigente = true;

    Promise.all([
      obtenerClase(id),
      // El listado de inscriptos es accesorio: si falla, el detalle se muestra igual.
      listarInscriptos(id).catch(() => []),
    ])
      .then(([datos, lista]) => {
        if (!vigente) return;
        setClase(datos);
        setInscriptos(Array.isArray(lista) ? lista : lista.contenido || []);
      })
      .catch((error) => {
        if (vigente) setMensajeError(mensajeDeError(error, "No se pudo cargar la clase."));
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
      const blob = await imprimirClase(id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener");
      URL.revokeObjectURL(url);
    } catch (error) {
      setMensajeError(mensajeDeError(error, "No se pudo generar el PDF de la clase."));
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
    return <p className="py-10 text-center text-sm text-muted-foreground">Cargando la clase...</p>;
  }

  if (!clase) {
    return (
      <div className="space-y-6">
        <VolverButton to={volverA} />
        <ErrorBanner>{mensajeError || "No se encontró la clase."}</ErrorBanner>
      </div>
    );
  }

  const datos = [
    ["Vigencia", vigencia(clase)],
    ["Días", diasCortos(clase)],
    ["Horario", horario(clase)],
    ...(!esProfesor ? [["Profesor", nombreProfesor(clase)]] : []),
    ["Cancha", nombreCancha(clase)],
    ["Nivel", ETIQUETA_NIVEL[clase.nivel] ?? clase.nivel],
    ["Modalidad", ETIQUETA_MODALIDAD[clase.modalidad] ?? clase.modalidad],
  ];

  return (
    <div className="space-y-6">
      <VolverButton to={volverA} />

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{clase.nombre}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {diasCortos(clase)} {horario(clase)}{!esProfesor && ` · ${nombreProfesor(clase)}`} &middot; {nombreCancha(clase)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {!esProfesor && clase.modalidad === Modalidad.PARTICULAR && (
            <Button variant="outline" onClick={() => setAsignando(true)}>
              <UserCog />
              Asignar instructor
            </Button>
          )}
          <Button
            variant={esProfesor ? "default" : "outline"}
            className="touch-manipulation"
            onClick={() => navigate(`/clases/${clase.id}/asistencia`, { state: { from: location.pathname } })}
            disabled={!admiteAsistencia(clase)}
            title={!admiteAsistencia(clase) ? "La clase todavía no empezó" : undefined}
          >
            <ClipboardCheck />
            Asistencia
          </Button>
          <Button variant="outline" onClick={imprimir}>
            <Printer />
            Imprimir
          </Button>
          {!esProfesor && (
            <Button onClick={() => navigate(`/clases/editar/${clase.id}`)}>
              <Pencil />
              Modificar
            </Button>
          )}
        </div>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>
      {mensajeError && <ErrorBanner>{mensajeError}</ErrorBanner>}

      <div className="grid gap-5 lg:grid-cols-2">
        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="mb-4 text-lg font-bold">Datos de la clase</h2>
          <dl className="divide-y divide-border text-sm">
            {datos.map(([clave, valor]) => (
              <div key={clave} className="flex items-center justify-between gap-3 py-2.5">
                <dt className="text-muted-foreground">{clave}</dt>
                <dd className="font-medium">{valor}</dd>
              </div>
            ))}
          </dl>
        </section>

        <section className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h2 className="mb-4 text-lg font-bold">Ocupación del cupo</h2>
          <CupoBadge clase={clase} />

          {cupoCompleto(clase) ? (
            <p className="mt-4 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              <strong className="font-semibold">El cupo está completo.</strong> Para inscribir a alguien más
              hay que liberar un lugar.
            </p>
          ) : (
            <p className="mt-4 text-sm text-muted-foreground">
              Quedan {lugaresLibres(clase)} {lugaresLibres(clase) === 1 ? "lugar" : "lugares"} de{" "}
              {clase.cupoMaximo}.
            </p>
          )}
        </section>
      </div>

      <section className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-bold">
            Alumnos inscriptos{" "}
            <Badge variant="outline" className="ml-1 font-normal">
              {inscriptos.length}
            </Badge>
          </h2>
          {!esProfesor && (
            <Button
              onClick={() => setInscribiendo(true)}
              disabled={!admiteInscripcion(clase)}
              title={cupoCompleto(clase) ? "El cupo está completo" : undefined}
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
                {!esProfesor && (
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    className="text-destructive hover:bg-destructive/10"
                    aria-label={`Dar de baja a ${i.socio}`}
                    disabled={quitando === i.socioId}
                    onClick={() => quitar(i)}
                  >
                    <UserMinus />
                  </Button>
                )}
                {i.estado === "PENDIENTE_PAGO" && i.inscripcionId && (
                  <Link to={`/inscripciones/${i.inscripcionId}/cobro`} className="shrink-0 text-sm font-semibold text-primary hover:underline">
                    Cobrar seña
                  </Link>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <AsignarInstructorDialog
        key={`asignar-${asignando}-${clase.profesorId ?? "sin"}`}
        abierto={asignando}
        clase={clase}
        onCerrar={() => setAsignando(false)}
        onAsignado={(mensaje) => {
          setAsignando(false);
          setAviso(mensaje);
          cargar();
        }}
      />

      <InscribirAlumnoDialog
        key={`inscribir-${inscribiendo}-${clase.cupoOcupado ?? 0}`}
        abierto={inscribiendo}
        clase={clase}
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

export default function ClaseDetallePage() {
  const rol = useAuthStore((estado) => estado.user?.rol);
  return rol === "SOCIO" ? <ClaseDelSocio /> : <ClaseDetalleDelPersonal />;
}
