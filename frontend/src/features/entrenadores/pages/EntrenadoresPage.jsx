import { useEffect, useState } from "react";
import { Plus, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { useValorDemorado } from "@/hooks/use-valor-demorado";
import { ETIQUETA_VALIDACION, EstadoValidacion } from "../model/entrenador";
import { useEntrenadores } from "../hooks/useEntrenadores";
import { imprimirEntrenador } from "../api/entrenadoresApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { EntrenadoresTabla } from "../components/EntrenadoresTabla";
import { EliminarEntrenadorDialog } from "../components/EliminarEntrenadorDialog";

const TODOS = "TODOS";

/** Listado de entrenadores: alta, baja, modificación y consulta (CU25-CU29). */
export function EntrenadoresPage() {
  const navigate = useNavigate();
  const [busqueda, setBusqueda] = useState("");
  const [especialidad, setEspecialidad] = useState(TODOS);
  const [estado, setEstado] = useState(TODOS);
  const [activo, setActivo] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");
  const [errorImprimir, setErrorImprimir] = useState("");
  const [aEliminar, setAEliminar] = useState(null);
  const [especialidadesConocidas, setEspecialidadesConocidas] = useState([]);

  const busquedaDemorada = useValorDemorado(busqueda);

  const { entrenadores, totalPaginas, totalElementos, cargando, error, recargar } = useEntrenadores({
    busqueda: busquedaDemorada,
    especialidad: especialidad === TODOS ? "" : especialidad,
    estado: estado === TODOS ? "" : estado,
    activo: activo === TODOS ? "" : activo,
    pagina,
  });

  const hayFiltros = busqueda !== "" || especialidad !== TODOS || estado !== TODOS || activo !== TODOS;

  useEffect(() => {
    if (entrenadores?.length) {
      setEspecialidadesConocidas((prev) => {
        const actuales = entrenadores.map((e) => e.especialidad?.trim()).filter(Boolean);
        if (!hayFiltros) {
          return Array.from(new Set(actuales)).sort();
        }
        return Array.from(new Set([...prev, ...actuales])).sort();
      });
    }
  }, [entrenadores, hayFiltros]);

  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setBusqueda("");
    setEspecialidad(TODOS);
    setEstado(TODOS);
    setActivo(TODOS);
    setPagina(0);
  }

  function alEliminar(mensaje) {
    setAEliminar(null);
    setAviso(mensaje);
    recargar();
  }

  async function imprimir(entrenador) {
    setErrorImprimir("");
    try {
      const pdf = await imprimirEntrenador(entrenador.id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setErrorImprimir(mensajeDeError(e, "No se pudo generar la ficha."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Entrenadores</h1>
        <Button onClick={() => navigate("/entrenadores/nuevo")}>
          <Plus />
          Nuevo entrenador
        </Button>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      {errorImprimir && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorImprimir}
        </p>
      )}

      <div className="space-y-2">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <div className="flex items-center gap-2 flex-1 max-w-md">
            <div className="relative flex-1">
              <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={busqueda}
                placeholder="Nombre, DNI o correo..."
                className="h-10 rounded-full pl-11"
                onChange={(e) => filtrar(setBusqueda)(e.target.value)}
              />
            </div>
            {hayFiltros && (
              <Button
                variant="ghost"
                size="sm"
                onClick={limpiarFiltros}
                className="text-xs text-muted-foreground hover:text-foreground shrink-0"
              >
                Limpiar filtros
              </Button>
            )}
          </div>

          <div className="flex flex-wrap gap-2 w-full lg:w-auto">
            <FiltroPill
              valor={especialidad}
              onChange={filtrar(setEspecialidad)}
              ancho="w-full sm:w-auto min-w-[190px]"
              opciones={[
                { value: TODOS, label: "Todas las especialidades" },
                ...especialidadesConocidas.map((e) => ({ value: e, label: e })),
              ]}
            />
            <FiltroPill
              valor={estado}
              onChange={filtrar(setEstado)}
              ancho="w-full sm:w-auto min-w-[200px]"
              opciones={[
                { value: TODOS, label: "Todas las certificaciones" },
                ...Object.values(EstadoValidacion).map((e) => ({ value: e, label: ETIQUETA_VALIDACION[e] })),
              ]}
            />
            <FiltroPill
              valor={activo}
              onChange={filtrar(setActivo)}
              ancho="w-full sm:w-auto min-w-[160px]"
              opciones={[
                { value: TODOS, label: "Todos los estados" },
                { value: "true", label: "Activos" },
                { value: "false", label: "Inactivos" },
              ]}
            />
          </div>
        </div>

        {busqueda.trim().length === 1 && (
          <p className="text-xs text-muted-foreground pl-3">
            Ingresá al menos 2 caracteres para buscar por nombre, DNI o correo.
          </p>
        )}
      </div>

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeDeError(error, "No se pudo cargar el listado de entrenadores.")}
        </p>
      )}

      {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando entrenadores...</p>}

      {!cargando && !error && entrenadores.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros ? "Ningún entrenador coincide con la búsqueda." : "Todavía no hay entrenadores cargados."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => navigate("/entrenadores/nuevo")}>
              <Plus />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && entrenadores.length > 0 && (
        <EntrenadoresTabla
          entrenadores={entrenadores}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(e) => navigate(`/entrenadores/${e.id}`)}
          onEditar={(e) => navigate(`/entrenadores/editar/${e.id}`)}
          onImprimir={imprimir}
          onEliminar={setAEliminar}
        />
      )}

      <EliminarEntrenadorDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        entrenador={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alEliminar}
      />
    </div>
  );
}
