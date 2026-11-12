import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  Calendar,
  DollarSign,
  Download,
  GitFork,
  MapPin,
  Plus,
  Trophy,
  Users,
} from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuthStore } from "@/store/authStore";
import {
  descargarPdfResultados,
  generarFixture,
  listarInscripciones,
  obtenerTorneo,
} from "../api/torneosApi";
import { comoSeJuega, esLiga, etiquetaDeTipo, puedeGenerarFixture } from "../model/competencia";
import { InscribirTorneoDialog } from "../components/InscribirTorneoDialog";
import { mensajeDeError } from "@/lib/erroresApi";

const ESTADO_LABELS = {
  PLANIFICADO: "Inscripción abierta",
  INSCRIPCION_ABIERTA: "Inscripción abierta",
  EN_CURSO: "En curso",
  FINALIZADO: "Finalizado",
  CANCELADO: "Cancelado",
};

const SUPERFICIES_MAP = {
  LADRILLO: "Polvo de ladrillo",
  CEMENTO: "Cemento",
  PASTO: "Césped",
};

export function TorneoDetallePage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const esAdmin = user?.rol === "ADMINISTRADOR" || user?.rol === "EMPLEADO";
  // El socio se inscribe a sí mismo y el personal del club inscribe socios. Un profesor o un
  // entrenador no hacen ni una cosa ni la otra: el backend les cierra la ruta, así que no tiene
  // sentido ofrecerles el botón.
  const esSocio = user?.rol === "SOCIO";

  const [torneo, setTorneo] = useState(null);
  const [inscripciones, setInscripciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [mensajeExito, setMensajeExito] = useState("");
  const [dialogInscripcionAbierto, setDialogInscripcionAbierto] = useState(false);
  const [generandoCuadro, setGenerandoCuadro] = useState(false);
  const [descargandoPdf, setDescargandoPdf] = useState(false);
  const [version, setVersion] = useState(0);

  const recargar = useCallback(() => {
    setVersion((v) => v + 1);
  }, []);

  useEffect(() => {
    let cancelado = false;

    async function cargar() {
      try {
        const [t, insecs] = await Promise.all([
          obtenerTorneo(id),
          listarInscripciones(id).catch(() => []),
        ]);
        if (!cancelado) {
          setTorneo(t);
          setInscripciones(Array.isArray(insecs) ? insecs : []);
        }
      } catch (err) {
        if (!cancelado) {
          setError(mensajeDeError(err, "No se pudo cargar el detalle del torneo."));
        }
      } finally {
        if (!cancelado) {
          setCargando(false);
        }
      }
    }

    cargar();
    return () => {
      cancelado = true;
    };
  }, [id, version]);

  async function handleGenerarFixture() {
    setGenerandoCuadro(true);
    setError("");
    try {
      await generarFixture(id);
      setMensajeExito("¡Cuadro y fixture generados con éxito!");
      recargar();
      navigate(`/torneos/${id}/fixture`);
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo generar el fixture. Verificá que haya inscriptos suficientes."));
    } finally {
      setGenerandoCuadro(false);
    }
  }

  async function handleDescargarPdf() {
    setDescargandoPdf(true);
    try {
      const blob = await descargarPdfResultados(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `resultados-torneo-${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo descargar el PDF de resultados."));
    } finally {
      setDescargandoPdf(false);
    }
  }

  if (cargando) {
    return <div className="p-12 text-center text-sm text-muted-foreground">Cargando detalle del torneo...</div>;
  }

  if (!torneo) {
    return (
      <div className="p-8 text-center text-sm text-destructive">
        {error || "Torneo no encontrado."}
      </div>
    );
  }

  const inscripcionAbierta = torneo.estado === "INSCRIPCION_ABIERTA" || torneo.estado === "PLANIFICADO";
  const puedeInscribirse = inscripcionAbierta && (esSocio || esAdmin);
  const puedeGenerar = esAdmin && inscripcionAbierta;
  // Un torneo por eliminación necesita una potencia de 2; una liga, dos inscriptos y nada más
  const fixturePosible = puedeGenerarFixture(torneo, inscripciones.length);

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-3">
          <Link
            to="/torneos"
            className={buttonVariants({ variant: "ghost", size: "icon", className: "shrink-0 mt-1" })}
          >
            <ArrowLeft className="size-5" />
            <span className="sr-only">Volver a torneos</span>
          </Link>
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{torneo.nombre}</h1>
              <Badge variant={esLiga(torneo) ? "default" : "secondary"}>{etiquetaDeTipo(torneo)}</Badge>
              <Badge variant="outline">{ESTADO_LABELS[torneo.estado] || torneo.estado?.replace(/_/g, " ")}</Badge>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              {comoSeJuega(torneo)} &middot; Categoría {torneo.categoria} &middot; {torneo.modalidad} &middot; {SUPERFICIES_MAP[torneo.tipoSuperficie] || torneo.tipoSuperficie?.replace(/_/g, " ")}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {puedeInscribirse && (
            <Button onClick={() => setDialogInscripcionAbierto(true)} className="gap-2">
              <Plus className="size-4" />
              {esAdmin ? "Inscribir socios" : "Inscribirme"}
            </Button>
          )}
          <Link
            to={`/torneos/${id}/fixture`}
            className={buttonVariants({ variant: "outline", className: "gap-2" })}
          >
            <GitFork className="size-4" />
            Ver fixture
          </Link>
          {puedeGenerar && (
            <Button
              variant="secondary"
              disabled={generandoCuadro || !fixturePosible.puede}
              title={fixturePosible.motivo || undefined}
              onClick={handleGenerarFixture}
              className="gap-2"
            >
              <Trophy className="size-4" />
              {generandoCuadro
                ? "Generando..."
                : esLiga(torneo)
                  ? "Generar calendario"
                  : "Generar cuadro"}
            </Button>
          )}
          <Button
            variant="outline"
            disabled={descargandoPdf}
            onClick={handleDescargarPdf}
            className="gap-2"
          >
            <Download className="size-4" />
            {descargandoPdf ? "Descargando..." : "Imprimir resultados"}
          </Button>
        </div>
      </div>

      {mensajeExito && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 dark:bg-emerald-950/20 p-4 text-sm text-emerald-800 dark:text-emerald-300">
          {mensajeExito}
        </div>
      )}

      {error && (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-destructive/20 bg-destructive/10 p-4 text-sm text-destructive"
        >
          <AlertCircle className="size-5 shrink-0 mt-0.5" />
          <p>{error}</p>
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-4 flex items-center gap-2">
              <Users className="size-4 text-primary" />
              Participantes inscriptos ({inscripciones.length})
            </h2>

            {inscripciones.length === 0 ? (
              <p className="text-sm text-muted-foreground">Todavía no hay participantes inscriptos.</p>
            ) : (
              <div className="divide-y divide-border rounded-lg border border-border overflow-hidden">
                {inscripciones.map((insc, idx) => (
                  <div key={insc.id || idx} className="flex items-center justify-between p-3 text-sm">
                    <span className="font-medium text-foreground">
                      {insc.jugador1Nombre || insc.nombreJugador1 || insc.nombreEquipo || `Participante #${insc.jugador1Id}`}
                      {(insc.parejaNombre || insc.parejaId) ? ` & ${insc.parejaNombre || `Pareja #${insc.parejaId}`}` : ""}
                    </span>
                    <span className="text-xs text-muted-foreground font-mono">#{insc.id}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
            <h2 className="text-base font-bold tracking-tight">Ficha técnica</h2>
            <dl className="divide-y divide-border text-sm">
              <div className="flex items-center justify-between py-2">
                <dt className="flex items-center gap-1.5 text-muted-foreground">
                  <Calendar className="size-4" /> Fechas
                </dt>
                <dd className="font-medium">{torneo.fechaInicio} al {torneo.fechaFin}</dd>
              </div>
              <div className="flex items-center justify-between py-2">
                <dt className="flex items-center gap-1.5 text-muted-foreground">
                  <MapPin className="size-4" /> Sede
                </dt>
                <dd className="font-medium">
                  {torneo.identificadorCancha || (torneo.canchaId ? `Cancha #${torneo.canchaId}` : "Por definir")}
                </dd>
              </div>
              <div className="flex items-center justify-between py-2">
                <dt className="flex items-center gap-1.5 text-muted-foreground">
                  <DollarSign className="size-4" /> Arancel
                </dt>
                <dd className="font-bold text-primary">
                  ${Number(torneo.costoInscripcion || 0).toLocaleString("es-AR")}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>

      <InscribirTorneoDialog
        open={dialogInscripcionAbierto}
        torneo={torneo}
        onClose={() => setDialogInscripcionAbierto(false)}
        onInscripto={(inscripcion) => {
          setDialogInscripcionAbierto(false);
          if (inscripcion?.estado === "PENDIENTE_PAGO") {
            navigate(`/torneos/inscripciones/${inscripcion.id}/cobro`);
            return;
          }
          recargar();
        }}
      />
    </div>
  );
}
