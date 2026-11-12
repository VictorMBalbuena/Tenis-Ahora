import { useState } from "react";
import { AlertTriangle, ArrowRightLeft, CircleAlert, Package, Plus, Search, Tags, Warehouse } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { ETIQUETA_ESTADO, EstadoStock, sinStock } from "../model/insumo";
import { useCategoriasInsumo, useInsumos, useResumenInsumos, useValorDemorado } from "../hooks/useInsumos";
import { mensajeDeError } from "../api/errores";
import { InsumosTabla } from "../components/InsumosTabla";
import { EliminarInsumoDialog } from "../components/EliminarInsumoDialog";
import { MovimientoStockDialog } from "../components/MovimientoStockDialog";
import { KpiCard } from "@/components/ui/kpi-card";

const TODOS = "TODOS";

export function InsumosPage() {
  const navigate = useNavigate();
  const [busqueda, setBusqueda] = useState("");
  const [categoriaId, setCategoriaId] = useState(TODOS);
  const [estado, setEstado] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");
  const [aEliminar, setAEliminar] = useState(null);
  const [enMovimiento, setEnMovimiento] = useState(null);
  const busquedaDemorada = useValorDemorado(busqueda);
  const { categorias } = useCategoriasInsumo();
  const { resumen, recargarResumen } = useResumenInsumos();
  const { contenido, totalPaginas, totalElementos, cargando, error, recargar } =
    useInsumos({
      busqueda: busquedaDemorada,
      categoriaId: categoriaId === TODOS ? "" : categoriaId,
      estado: estado === TODOS ? "" : estado,
      page: pagina,
    });
  const faltantes = contenido.filter(sinStock);
  const hayFiltros = busqueda !== "" || categoriaId !== TODOS || estado !== TODOS;
  function alCompletar(mensaje) {
    setAEliminar(null);
    setEnMovimiento(null);
    setAviso(mensaje);
    recargar();
    recargarResumen();
  }
  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }
  function limpiarFiltros() {
    setBusqueda("");
    setCategoriaId(TODOS);
    setEstado(TODOS);
    setPagina(0);
  }
  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">
          Insumos
        </h1>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => navigate("/insumos/categorias")}>
            <Tags />
            Categorías
          </Button>
          <Button onClick={() => navigate("/insumos/nuevo")}>
            <Plus />
            Nuevo insumo
          </Button>
        </div>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <KpiCard etiqueta="Insumos distintos" valor={resumen.distintos} icono={Package} />
        <KpiCard etiqueta="Unidades en depósito" valor={resumen.enDeposito} icono={Warehouse} />
        <KpiCard etiqueta="Asignadas a turnos" valor={resumen.asignadas} icono={ArrowRightLeft} />
        <KpiCard etiqueta="Alertas de stock" valor={resumen.alertas} icono={AlertTriangle} />
      </div>

      {/* Un insumo en cero no se puede asignar a una reserva.
          La regla RD18/RD19 habla de insumos *obligatorios*, pero la entidad
          todavía no tiene ese campo, así que se avisa por todos. */}
      {faltantes.length > 0 && (
        <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-error-bg)] p-3.5 text-sm text-[var(--estado-error-fg)]">
          <CircleAlert className="mt-0.5 size-4 shrink-0" />
          <span>
            <strong className="font-semibold">
              {faltantes.map((i) => i.nombre).join(", ")} sin stock.
            </strong>{" "}
            No se puede asignar a una reserva un insumo que está en cero.
            Repone stock para habilitarlo.
          </span>
        </p>
      )}

      {/* Filtros */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div className="relative flex-1 sm:max-w-sm">
          <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={busqueda}
            placeholder="Buscar insumo..."
            className="h-10 rounded-full pl-11"
            onChange={(e) => filtrar(setBusqueda)(e.target.value)}
          />
        </div>
        <div className="flex gap-2">
          <FiltroPill
            valor={categoriaId}
            onChange={filtrar(setCategoriaId)}
            ancho="w-[170px]"
            opciones={[
              { value: TODOS, label: "Todos" },
              ...categorias.map((c) => ({ value: String(c.id), label: c.nombre })),
            ]}
          />
          <FiltroPill
            valor={estado}
            onChange={filtrar(setEstado)}
            ancho="w-[170px]"
            opciones={[
              { value: TODOS, label: "Todos" },
              ...Object.values(EstadoStock).map((e) => ({ value: e, label: ETIQUETA_ESTADO[e] })),
            ]}
          />
        </div>
      </div>

      {/* Listado */}
      {error && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive"
        >
          {mensajeDeError(error, "No se pudo cargar el listado de insumos.")}
        </p>
      )}

      {cargando && (
        <p className="py-10 text-center text-sm text-muted-foreground">
          Cargando insumos...
        </p>
      )}

      {!cargando && !error && contenido.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros
              ? "Ningún insumo coincide con la búsqueda."
              : "Todavía no hay insumos cargados."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => navigate("/insumos/nuevo")}>
              <Plus />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && contenido.length > 0 && (
        <InsumosTabla
          insumos={contenido}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(i) => navigate(`/insumos/${i.id}`)}
          onEditar={(i) => navigate(`/insumos/editar/${i.id}`)}
          onMovimiento={setEnMovimiento}
          onEliminar={setAEliminar}
        />
      )}

      <EliminarInsumoDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        insumo={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alCompletar}
      />

      <MovimientoStockDialog
        key={`movimiento-${enMovimiento?.id ?? "ninguno"}`}
        abierto={enMovimiento !== null}
        insumo={enMovimiento}
        onCerrar={() => setEnMovimiento(null)}
        onRegistrado={alCompletar}
      />
    </div>
  );
}
