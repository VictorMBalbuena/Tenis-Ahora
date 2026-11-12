import { useState } from "react";
import { Plus, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { ETIQUETA_VALIDACION, EstadoValidacion } from "../model/profesor";
import { useProfesores, useValorDemorado } from "../hooks/useProfesores";
import { imprimirProfesor } from "../api/profesoresApi";
import { mensajeDeError } from "@/lib/erroresApi";
import { ProfesoresTabla } from "../components/ProfesoresTabla";
import { EliminarProfesorDialog } from "../components/EliminarProfesorDialog";

const TODOS = "TODOS";

/** Listado de profesores: alta, baja, modificación y consulta (CU20-CU24). */
export function ProfesoresPage() {
  const navigate = useNavigate();
  const [busqueda, setBusqueda] = useState("");
  const [estado, setEstado] = useState(TODOS);
  const [activo, setActivo] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");
  const [errorImprimir, setErrorImprimir] = useState("");
  const [aEliminar, setAEliminar] = useState(null);

  const busquedaDemorada = useValorDemorado(busqueda);

  const { profesores, totalPaginas, totalElementos, cargando, error, recargar } = useProfesores({
    busqueda: busquedaDemorada,
    estado: estado === TODOS ? "" : estado,
    activo: activo === TODOS ? "" : activo,
    pagina,
  });

  const hayFiltros = busqueda !== "" || estado !== TODOS || activo !== TODOS;

  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setBusqueda("");
    setEstado(TODOS);
    setActivo(TODOS);
    setPagina(0);
  }

  function alEliminar(mensaje) {
    setAEliminar(null);
    setAviso(mensaje);
    recargar();
  }

  async function imprimir(profesor) {
    setErrorImprimir("");
    try {
      const pdf = await imprimirProfesor(profesor.id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setErrorImprimir(mensajeDeError(e, "No se pudo generar la ficha."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Profesores</h1>
        <Button onClick={() => navigate("/profesores/nuevo")}>
          <Plus />
          Nuevo profesor
        </Button>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      {errorImprimir && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorImprimir}
        </p>
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-sm">
          <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={busqueda}
            placeholder="Nombre, DNI o correo..."
            className="h-10 rounded-full pl-11"
            onChange={(e) => filtrar(setBusqueda)(e.target.value)}
          />
        </div>
        <div className="flex gap-2">
          <FiltroPill
            valor={estado}
            onChange={filtrar(setEstado)}
            ancho="w-[230px]"
            opciones={[
              { value: TODOS, label: "Todas las certificaciones" },
              ...Object.values(EstadoValidacion).map((e) => ({ value: e, label: ETIQUETA_VALIDACION[e] })),
            ]}
          />
          <FiltroPill
            valor={activo}
            onChange={filtrar(setActivo)}
            ancho="w-[190px]"
            opciones={[
              { value: TODOS, label: "Todos los estados" },
              { value: "true", label: "Activos" },
              { value: "false", label: "Inactivos" },
            ]}
          />
        </div>
      </div>

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeDeError(error, "No se pudo cargar el listado de profesores.")}
        </p>
      )}

      {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando profesores...</p>}

      {!cargando && !error && profesores.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros ? "Ningún profesor coincide con la búsqueda." : "Todavía no hay profesores cargados."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => navigate("/profesores/nuevo")}>
              <Plus />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && profesores.length > 0 && (
        <ProfesoresTabla
          profesores={profesores}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(p) => navigate(`/profesores/${p.id}`)}
          onEditar={(p) => navigate(`/profesores/editar/${p.id}`)}
          onImprimir={imprimir}
          onEliminar={setAEliminar}
        />
      )}

      <EliminarProfesorDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        profesor={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alEliminar}
      />
    </div>
  );
}
