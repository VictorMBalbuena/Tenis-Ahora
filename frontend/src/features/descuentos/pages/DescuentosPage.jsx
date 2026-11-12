import { useEffect, useState } from "react";
import { Info, Plus, Search, Tag } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  etiqueta,
  resumirDescuentos,
} from "../model/descuento";
import { useDescuentos, useValorDemorado } from "../hooks/useDescuentos";
import { mensajeDeError } from "../api/errores";
import { DescuentosTabla } from "../components/DescuentosTabla";
import { DescuentoFormDialog } from "../components/DescuentoFormDialog";
import { DescuentoDetalleDialog } from "../components/DescuentoDetalleDialog";
import { EliminarDescuentoDialog } from "../components/EliminarDescuentoDialog";
import { FichaImpresion } from "../components/FichaImpresion";
import "../descuentos.css";
import { KpiCard } from "@/components/ui/kpi-card";

const TODOS = "TODOS";

const ETIQUETA_ACTIVO = { SI: "Activos", NO: "Inactivos" };

/**
 * Pantalla de Descuentos (SCRUM-42, FRONT 14).
 *
 * Cubre CU62 a CU66. La ruta vive bajo el ProtectedRoute de ADMINISTRADOR,
 * que es lo que pide el criterio de aceptación: un empleado que no es
 * administrador no ve el menú ni puede entrar por URL.
 */
export function DescuentosPage() {
  const [busqueda, setBusqueda] = useState("");
  const [activo, setActivo] = useState(TODOS);
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");

  const [formAbierto, setFormAbierto] = useState(false);
  const [enEdicion, setEnEdicion] = useState(null);
  const [detalleId, setDetalleId] = useState(null);
  const [aEliminar, setAEliminar] = useState(null);
  const [aImprimir, setAImprimir] = useState(null);

  const busquedaDemorada = useValorDemorado(busqueda);

  const { contenido, totalPaginas, totalElementos, cargando, error, recargar } =
    useDescuentos({
      busqueda: busquedaDemorada,
      activo: activo === TODOS ? "" : activo,
      page: pagina,
    });

  const resumen = resumirDescuentos(contenido, totalElementos);
  const hayFiltros = busqueda !== "" || activo !== TODOS;
  const hayVariasPaginas = totalPaginas > 1;

  /**
   * CU66 - Imprimir.
   *
   * Hay que esperar a que React pinte la ficha antes de llamar a print(),
   * porque si no el navegador imprime la página sin ella.
   */
  useEffect(() => {
    if (!aImprimir) return;

    const t = setTimeout(() => {
      window.print();
      setAImprimir(null);
    }, 80);

    return () => clearTimeout(t);
  }, [aImprimir]);

  /** Toda operación que modifica termina igual: aviso y listado al día. */
  function alCompletar(mensaje) {
    setFormAbierto(false);
    setEnEdicion(null);
    setAEliminar(null);
    setAviso(mensaje);
    recargar();
  }

  /** Cambiar un filtro vuelve a la primera página. */
  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setBusqueda("");
    setActivo(TODOS);
    setPagina(0);
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Descuentos</h1>
        </div>
        <Button
          onClick={() => {
            setEnEdicion(null);
            setFormAbierto(true);
          }}
        >
          <Plus />
          Nuevo descuento
        </Button>
      </header>

      {/* Avisos de resultado de la última operación */}
      <div aria-live="polite" role="status">
        {aviso && (
          <p className="rounded-lg bg-[var(--estado-ok-bg)] px-3.5 py-2.5 text-sm text-[var(--estado-ok-fg)]">
            {aviso}
          </p>
        )}
      </div>

      <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-info-bg,var(--muted))] p-3.5 text-sm text-muted-foreground">
        <Info className="mt-0.5 size-4 shrink-0" />
        <span>
          La configuración de descuentos está habilitada únicamente para
          administradores. Solo los descuentos activos se ofrecen al cobrar.
        </span>
      </p>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <KpiCard etiqueta="Descuentos" valor={resumen.total} icono={Tag} />
        <KpiCard
          etiqueta="Activos"
          valor={resumen.activos}
          detalle={hayVariasPaginas ? "En esta página" : "Se aplican al cobrar"}
        />
        <KpiCard
          etiqueta="Inactivos"
          valor={resumen.inactivos}
          detalle={hayVariasPaginas ? "En esta página" : undefined}
        />
        <KpiCard
          etiqueta="Sin beneficio cargado"
          valor={resumen.sinBeneficio}
          detalle="Ni porcentaje ni monto"
        />
      </div>

      {/* Filtros */}
      <div className="flex flex-wrap items-end gap-3">
        <div className="min-w-[200px] flex-1 space-y-1.5">
          <Label htmlFor="buscar-descuento">Buscar</Label>
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="buscar-descuento"
              value={busqueda}
              placeholder="Buscar en las condiciones..."
              className="pl-8"
              onChange={(e) => filtrar(setBusqueda)(e.target.value)}
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="filtro-activo">Estado</Label>
          <Select value={activo} onValueChange={filtrar(setActivo)}>
            <SelectTrigger id="filtro-activo" className="w-[150px]">
              <SelectValue>
                {(v) => (v === TODOS ? "Todos" : ETIQUETA_ACTIVO[v])}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={TODOS}>Todos</SelectItem>
              <SelectItem value="SI">Activos</SelectItem>
              <SelectItem value="NO">Inactivos</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Listado */}
      {error && (
        <p
          role="alert"
          className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive"
        >
          {mensajeDeError(error, "No se pudo cargar el listado de descuentos.")}
        </p>
      )}

      {cargando && (
        <p className="py-10 text-center text-sm text-muted-foreground">
          Cargando descuentos...
        </p>
      )}

      {!cargando && !error && contenido.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros
              ? "Ningún descuento coincide con la búsqueda."
              : "Todavía no hay descuentos cargados."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => setFormAbierto(true)}>
              <Plus />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && contenido.length > 0 && (
        <DescuentosTabla
          descuentos={contenido}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(d) => setDetalleId(d.id)}
          onEditar={(d) => {
            setEnEdicion(d);
            setFormAbierto(true);
          }}
          onImprimir={setAImprimir}
          onEliminar={setAEliminar}
        />
      )}

      {/* El `key` remonta cada diálogo al abrirlo. Así los formularios
          arrancan limpios por el valor inicial de useState, sin un efecto
          que los resetee a mano. */}
      <DescuentoFormDialog
        key={`form-${formAbierto}-${enEdicion?.id ?? "nuevo"}`}
        abierto={formAbierto}
        descuento={enEdicion}
        onCerrar={() => {
          setFormAbierto(false);
          setEnEdicion(null);
        }}
        onGuardado={alCompletar}
      />

      <DescuentoDetalleDialog
        key={`detalle-${detalleId ?? "ninguno"}`}
        abierto={detalleId !== null}
        descuentoId={detalleId}
        onCerrar={() => setDetalleId(null)}
      />

      <EliminarDescuentoDialog
        key={`baja-${aEliminar?.id ?? "ninguno"}`}
        abierto={aEliminar !== null}
        descuento={aEliminar}
        onCerrar={() => setAEliminar(null)}
        onEliminado={alCompletar}
      />

      {/* Solo visible en la hoja impresa (CU66) */}
      <FichaImpresion descuento={aImprimir} />

      {/* Lector de pantalla: avisar que se mandó a imprimir */}
      <div className="sr-only" aria-live="polite">
        {aImprimir && `Preparando la ficha del descuento ${etiqueta(aImprimir)}.`}
      </div>
    </div>
  );
}
