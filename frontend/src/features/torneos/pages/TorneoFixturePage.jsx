import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  Download,
  FileText,
  ListOrdered,
  ShieldCheck,
  Trophy,
  Users,
} from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuthStore } from "@/store/authStore";
import {
  descargarPdfResultados,
  obtenerPartidos,
  obtenerPosiciones,
  obtenerTorneo,
} from "../api/torneosApi";
import { CampeonBanner } from "../components/CampeonBanner";
import { FixtureTree } from "../components/FixtureTree";
import { CalendarioLiga } from "../components/CalendarioLiga";
import { TablaPosiciones } from "../components/TablaPosiciones";
import { comoSeJuega, esLiga, etiquetaDeTipo } from "../model/competencia";
import { CargarResultadoDialog } from "../components/CargarResultadoDialog";
import { mensajeDeError } from "@/lib/erroresApi";

export function TorneoFixturePage() {
  const { id } = useParams();
  const user = useAuthStore((s) => s.user);
  const esAdmin = user?.rol === "ADMINISTRADOR" || user?.rol === "EMPLEADO";

  const [torneo, setTorneo] = useState(null);
  const [partidos, setPartidos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("cuadro");
  const [posiciones, setPosiciones] = useState([]);
  const [partidoACargar, setPartidoACargar] = useState(null);
  const [descargando, setDescargando] = useState(false);
  const [version, setVersion] = useState(0);

  const recargar = useCallback(() => {
    setVersion((v) => v + 1);
  }, []);

  useEffect(() => {
    let cancelado = false;

    async function cargar() {
      try {
        const [torneoData, partidosData] = await Promise.all([
          obtenerTorneo(id),
          obtenerPartidos(id).catch(() => []),
        ]);
        // La tabla solo existe en una liga; en un torneo el backend responde 400 a propósito
        const posicionesData = esLiga(torneoData) ? await obtenerPosiciones(id).catch(() => []) : [];
        if (!cancelado) {
          setTorneo(torneoData);
          setPartidos(Array.isArray(partidosData) ? partidosData : []);
          setPosiciones(Array.isArray(posicionesData) ? posicionesData : []);
          // En una liga lo primero que se mira es la tabla, no el calendario
          if (esLiga(torneoData)) setTab((actual) => (actual === "cuadro" ? "posiciones" : actual));
        }
      } catch (err) {
        if (!cancelado) {
          setError(mensajeDeError(err, "No se pudo cargar el fixture del torneo."));
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

  async function handleDescargarPdf() {
    setDescargando(true);
    try {
      const blob = await descargarPdfResultados(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `fixture-torneo-${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo generar el PDF de resultados."));
    } finally {
      setDescargando(false);
    }
  }

  const partidosFinalizados = useMemo(() => {
    return partidos.filter((p) => p.sets);
  }, [partidos]);

  const participantesUnicos = useMemo(() => {
    const map = new Map();
    for (const p of partidos) {
      if (p.participanteANombre) {
        map.set(p.participanteAId || p.participanteANombre, {
          id: p.participanteAId,
          nombre: p.participanteANombre,
        });
      } else if (p.participante1) {
        map.set(p.participante1.id || p.participante1.nombre, p.participante1);
      }

      if (p.participanteBNombre) {
        map.set(p.participanteBId || p.participanteBNombre, {
          id: p.participanteBId,
          nombre: p.participanteBNombre,
        });
      } else if (p.participante2) {
        map.set(p.participante2.id || p.participante2.nombre, p.participante2);
      }
    }
    return Array.from(map.values());
  }, [partidos]);

  if (cargando) {
    return <div className="p-12 text-center text-sm text-muted-foreground">Cargando fixture...</div>;
  }

  if (!torneo) {
    return <div className="p-8 text-center text-sm text-destructive">{error || "Torneo no encontrado."}</div>;
  }

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-3">
          <Link
            to={`/torneos/${id}`}
            className={buttonVariants({ variant: "ghost", size: "icon", className: "shrink-0 mt-1" })}
          >
            <ArrowLeft className="size-5" />
            <span className="sr-only">Volver al detalle del torneo</span>
          </Link>
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">{torneo.nombre}</h1>
              <Badge variant={esLiga(torneo) ? "default" : "secondary"}>{etiquetaDeTipo(torneo)}</Badge>
              <Badge variant="outline">{torneo.estado?.replace(/_/g, " ")}</Badge>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              {comoSeJuega(torneo)} &middot; {torneo.categoria} &middot; {torneo.modalidad}
            </p>
          </div>
        </div>

        <Button
          variant="outline"
          disabled={descargando}
          onClick={handleDescargarPdf}
          className="gap-2 shrink-0"
        >
          <Download className="size-4" />
          {descargando ? "Generando PDF..." : esLiga(torneo) ? "Imprimir calendario / resultados" : "Imprimir cuadro / resultados"}
        </Button>
      </div>

      {error && (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-destructive/20 bg-destructive/10 p-4 text-sm text-destructive"
        >
          <AlertCircle className="size-5 shrink-0 mt-0.5" />
          <p>{error}</p>
        </div>
      )}

      <CampeonBanner partidos={partidos} />

      {/* Tabs */}
      <div className="border-b border-border flex gap-4 text-sm font-medium">
        <button
          role="tab"
          aria-selected={tab === "cuadro"}
          onClick={() => setTab("cuadro")}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            tab === "cuadro"
              ? "border-primary text-primary font-bold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Trophy className="size-4" />
          {esLiga(torneo) ? "Calendario" : "Cuadro eliminatorio"}
        </button>

        {esLiga(torneo) && (
          <button
            role="tab"
            aria-selected={tab === "posiciones"}
            onClick={() => setTab("posiciones")}
            className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
              tab === "posiciones"
                ? "border-primary text-primary font-bold"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            <ListOrdered className="size-4" />
            Tabla de posiciones
          </button>
        )}

        <button
          role="tab"
          aria-selected={tab === "resultados"}
          onClick={() => setTab("resultados")}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            tab === "resultados"
              ? "border-primary text-primary font-bold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <CheckCircle2 className="size-4" />
          Resultados ({partidosFinalizados.length})
        </button>

        <button
          role="tab"
          aria-selected={tab === "inscriptos"}
          onClick={() => setTab("inscriptos")}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            tab === "inscriptos"
              ? "border-primary text-primary font-bold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <Users className="size-4" />
          Inscriptos ({participantesUnicos.length})
        </button>

        <button
          role="tab"
          aria-selected={tab === "reglamento"}
          onClick={() => setTab("reglamento")}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-2 ${
            tab === "reglamento"
              ? "border-primary text-primary font-bold"
              : "border-transparent text-muted-foreground hover:text-foreground"
          }`}
        >
          <FileText className="size-4" />
          Reglamento
        </button>
      </div>

      {/* Tab Panels */}
      {tab === "cuadro" &&
        (esLiga(torneo) ? (
          <CalendarioLiga
            partidos={partidos}
            esAdmin={esAdmin}
            onCargarResultado={(p) => setPartidoACargar(p)}
          />
        ) : (
          <FixtureTree
            partidos={partidos}
            esAdmin={esAdmin}
            onCargarResultado={(p) => setPartidoACargar(p)}
          />
        ))}

      {tab === "posiciones" && esLiga(torneo) && <TablaPosiciones posiciones={posiciones} />}

      {tab === "resultados" && (
        <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
          <h2 className="text-base font-bold tracking-tight mb-4">Resultados oficiales registrados</h2>
          {partidosFinalizados.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aún no se han disputado partidos con resultado final.</p>
          ) : (
            <div className="divide-y divide-border rounded-lg border border-border overflow-hidden">
              {partidosFinalizados.map((p) => {
                const nombre1 =
                  p.participanteANombre ||
                  p.participante1?.nombre ||
                  p.participante1?.nombreEquipo ||
                  "Participante 1";
                const nombre2 =
                  p.participanteBNombre ||
                  p.participante2?.nombre ||
                  p.participante2?.nombreEquipo ||
                  "Participante 2";
                return (
                  <div key={p.id} className="flex items-center justify-between p-4 text-sm">
                    <div>
                      <p className="font-semibold text-foreground">
                        {nombre1} vs {nombre2}
                      </p>
                      <p className="text-xs text-muted-foreground mt-0.5">Ronda #{p.ronda}</p>
                    </div>
                    <Badge variant="secondary" className="font-mono text-sm px-3 py-1">
                      {p.sets}
                    </Badge>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {tab === "inscriptos" && (
        <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
          <h2 className="text-base font-bold tracking-tight mb-4">Participantes en el cuadro</h2>
          {participantesUnicos.length === 0 ? (
            <p className="text-sm text-muted-foreground">No hay participantes definidos en el fixture.</p>
          ) : (
            <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {participantesUnicos.map((p, idx) => (
                <li
                  key={p.id || idx}
                  className="flex items-center gap-3 p-3 rounded-lg border border-border bg-muted/20"
                >
                  <Users className="size-4 text-primary shrink-0" />
                  <span className="text-sm font-medium truncate">{p.nombre || p.nombreEquipo}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {tab === "reglamento" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
            <h2 className="text-base font-bold tracking-tight flex items-center gap-2">
              <ShieldCheck className="size-5 text-primary" />
              Reglamento Deportivo Aplicado (RD14)
            </h2>
            <ul className="space-y-3 text-sm text-muted-foreground">
              <li className="flex items-start gap-2.5">
                <CheckCircle2 className="size-4 text-primary mt-0.5 shrink-0" />
                <span>Partidos disputados al mejor de 3 sets (gana el primer participante que alcance 2 sets).</span>
              </li>
              <li className="flex items-start gap-2.5">
                <CheckCircle2 className="size-4 text-primary mt-0.5 shrink-0" />
                <span>Cada set regular se gana al alcanzar 6 games con diferencia de al menos 2 (ej. 6-4, 6-2, 6-0).</span>
              </li>
              <li className="flex items-start gap-2.5">
                <CheckCircle2 className="size-4 text-primary mt-0.5 shrink-0" />
                <span>En caso de empate 5-5, se puede ganar por 7-5. Si empatan 6-6, se disputa tie-break cerrando en 7-6.</span>
              </li>
              <li className="flex items-start gap-2.5">
                <CheckCircle2 className="size-4 text-primary mt-0.5 shrink-0" />
                <span>Validación automática en vivo antes de asentar los resultados oficiales en el sistema.</span>
              </li>
            </ul>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm space-y-4">
            <h2 className="text-base font-bold tracking-tight flex items-center gap-2">
              <Clock className="size-5 text-primary" />
              Tolerancia y Puntualidad
            </h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Los participantes disponen de 15 minutos de tolerancia para presentarse en cancha a la hora estipulada
              en el fixture. La incomparecencia resultará en Walkover (W.O.) registrándose 6-0 6-0 para el rival presente.
            </p>
          </div>
        </div>
      )}

      {partidoACargar && (
        <CargarResultadoDialog
          key={partidoACargar.id}
          open={Boolean(partidoACargar)}
          partido={partidoACargar}
          onClose={() => setPartidoACargar(null)}
          onGuardado={() => {
            setPartidoACargar(null);
            recargar();
          }}
        />
      )}
    </div>
  );
}
