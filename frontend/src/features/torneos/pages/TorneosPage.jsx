import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AlertCircle, Plus, Search, Trophy } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { listarMisInscripcionesATorneos, listarTorneos } from "../api/torneosApi";
import { TorneosTabla } from "../components/TorneosTabla";
import { EliminarTorneoDialog } from "../components/EliminarTorneoDialog";
import { mensajeDeError } from "@/lib/erroresApi";

export function TorneosPage({ vista }) {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const esAdmin = user?.rol === "ADMINISTRADOR" || user?.rol === "EMPLEADO";

  const [torneos, setTorneos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [categoria, setCategoria] = useState("TODAS");
  const [estado, setEstado] = useState(vista === "abiertos" ? "PLANIFICADO" : "TODOS");
  const [misTorneos, setMisTorneos] = useState(new Set());
  const [aEliminar, setAEliminar] = useState(null);
  const [version, setVersion] = useState(0);

  const recargar = useCallback(() => {
    setVersion((v) => v + 1);
  }, []);

  useEffect(() => {
    let cancelado = false;

    async function cargar() {
      try {
        const data = await listarTorneos();
        const mias = vista === "mios" ? await listarMisInscripcionesATorneos() : [];
        if (!cancelado) {
          setTorneos(Array.isArray(data) ? data : []);
          setMisTorneos(new Set(mias.filter((i) => i.estado !== "CANCELADA").map((i) => i.torneoId)));
        }
      } catch (err) {
        if (!cancelado) {
          setError(mensajeDeError(err, "No se pudieron cargar los torneos."));
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
  }, [version, vista]);

  const torneosFiltrados = useMemo(() => {
    return torneos.filter((t) => {
      const cumpleBusqueda =
        !busqueda ||
        t.nombre?.toLowerCase().includes(busqueda.toLowerCase()) ||
        String(t.id).includes(busqueda);
      const cumpleCategoria = categoria === "TODAS" || t.categoria === categoria;
      const cumpleEstado =
        estado === "TODOS" ||
        t.estado === estado ||
        (estado === "PLANIFICADO" && (t.estado === "PLANIFICADO" || t.estado === "INSCRIPCION_ABIERTA"));

      const cumpleVista = vista !== "mios" || misTorneos.has(t.id);
      return cumpleBusqueda && cumpleCategoria && cumpleEstado && cumpleVista;
    });
  }, [torneos, busqueda, categoria, estado, vista, misTorneos]);

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2.5">
            <Trophy className="size-6 text-primary" />
            <h1 className="text-2xl font-bold tracking-tight text-foreground">Torneos y ligas</h1>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            Competencias activas, inscripciones y cuadro de partidos oficiales del club.
          </p>
        </div>

        {esAdmin && (
          <Link to="/torneos/nuevo" className={buttonVariants({ variant: "default", className: "gap-2 shrink-0" })}>
            <Plus className="size-4" />
            Nuevo torneo o liga
          </Link>
        )}
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar por nombre o ID..."
            className="flex h-10 w-full rounded-lg border border-input bg-background pl-9 pr-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <div>
          <label htmlFor="filtro-categoria" className="sr-only">Filtrar por categoría</label>
          <select
            id="filtro-categoria"
            aria-label="Filtrar por categoría"
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
            className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="TODAS">Todas las categorías</option>
            <option value="MASCULINO">Masculino</option>
            <option value="FEMENINO">Femenino</option>
            <option value="MIXTO">Mixto</option>
          </select>
        </div>

        <div>
          <label htmlFor="filtro-estado" className="sr-only">Filtrar por estado</label>
          <select
            id="filtro-estado"
            aria-label="Filtrar por estado"
            value={estado}
            onChange={(e) => setEstado(e.target.value)}
            className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="TODOS">Todos los estados</option>
            <option value="PLANIFICADO">Inscripción abierta</option>
            <option value="EN_CURSO">En curso</option>
            <option value="FINALIZADO">Finalizado</option>
          </select>
        </div>
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

      {cargando ? (
        <div className="p-12 text-center text-sm text-muted-foreground">Cargando torneos...</div>
      ) : (
        <TorneosTabla
          torneos={torneosFiltrados}
          esAdmin={esAdmin}
          onVerDetalle={(t) => navigate(`/torneos/${t.id}`)}
          onEditar={(t) => navigate(`/torneos/editar/${t.id}`)}
          onEliminar={(t) => setAEliminar(t)}
        />
      )}

      <EliminarTorneoDialog
        open={Boolean(aEliminar)}
        torneo={aEliminar}
        onClose={() => setAEliminar(null)}
        onEliminado={() => {
          setAEliminar(null);
          recargar();
        }}
      />
    </div>
  );
}
