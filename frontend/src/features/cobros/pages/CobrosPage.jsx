import { useState } from "react";
import { Plus, ReceiptText, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FiltroPill } from "@/components/ui/filtro-pill";
import { SuccessBanner } from "@/components/ui/success-banner";
import { DateField } from "@/components/ui/date-field";
import { useValorDemorado } from "@/hooks/use-valor-demorado";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";
import { OPCIONES_CONCEPTO, OPCIONES_MEDIO, pesos, rangoDeFechasValido, resumirCobros } from "../model/cobro";
import { useCobros } from "../hooks/useCobros";
import { emitirRecibo, imprimirCobro } from "../api/cobrosApi";
import { CobrosTabla } from "../components/CobrosTabla";
import { AnularCobroDialog } from "../components/AnularCobroDialog";
import { KpiCard } from "@/components/ui/kpi-card";

const TODOS = "TODOS";

/** Listado de cobros: consulta, impresión y anulación (CU54-CU56). */
export function CobrosPage() {
  const navigate = useNavigate();
  const esAdmin = useAuthStore((estado) => estado.user?.rol) === "ADMINISTRADOR";
  const [busqueda, setBusqueda] = useState("");
  const [concepto, setConcepto] = useState(TODOS);
  const [medioPago, setMedioPago] = useState(TODOS);
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");
  const [errorAccion, setErrorAccion] = useState("");
  const [aAnular, setAAnular] = useState(null);

  const busquedaDemorada = useValorDemorado(busqueda);
  const rangoInvalido = !rangoDeFechasValido(desde, hasta);

  const { cobros, totalPaginas, totalElementos, cargando, error, recargar } = useCobros({
    busqueda: busquedaDemorada,
    concepto: concepto === TODOS ? "" : concepto,
    medioPago: medioPago === TODOS ? "" : medioPago,
    desde,
    hasta,
    pagina,
    omitir: rangoInvalido,
  });

  const hayFiltros = busqueda !== "" || concepto !== TODOS || medioPago !== TODOS || desde !== "" || hasta !== "";
  const resumen = resumirCobros(cobros, totalElementos);

  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setBusqueda("");
    setConcepto(TODOS);
    setMedioPago(TODOS);
    setDesde("");
    setHasta("");
    setPagina(0);
  }

  function alAnular(mensaje) {
    setAAnular(null);
    setAviso(mensaje);
    recargar();
  }

  async function imprimir(cobro) {
    setErrorAccion("");
    try {
      const pdf = await imprimirCobro(cobro.id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setErrorAccion(mensajeDeError(e, "No se pudo generar el comprobante."));
    }
  }

  async function emitir(cobro) {
    setErrorAccion("");
    try {
      const recibo = await emitirRecibo(cobro.id);
      setAviso(`Se emitió el recibo ${recibo.numeroRecibo}.`);
      recargar();
    } catch (e) {
      setErrorAccion(mensajeDeError(e, "No se pudo emitir el recibo."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Cobros</h1>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => navigate("/recibos")}>
            <ReceiptText />
            Ver recibos
          </Button>
          <Button onClick={() => navigate("/cobros/nuevo")}>
            <Plus />
            Registrar cobro
          </Button>
        </div>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      {errorAccion && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorAccion}
        </p>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {[
          ["Cobros listados", String(resumen.total)],
          ["Ingresos de la página", pesos(resumen.ingresos)],
          ["Reembolsos de la página", pesos(resumen.reembolsos)],
        ].map(([etiqueta, valor]) => (
          <KpiCard key={etiqueta} etiqueta={etiqueta} valor={valor} />
        ))}
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1 sm:max-w-sm">
            <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={busqueda}
              placeholder="Buscar por C-000123 o R-000123"
              className="h-10 rounded-full pl-11"
              onChange={(e) => filtrar(setBusqueda)(e.target.value)}
            />
          </div>
          <div className="flex gap-2">
            <FiltroPill
              valor={concepto}
              onChange={filtrar(setConcepto)}
              ancho="w-[200px]"
              opciones={[{ value: TODOS, label: "Todos los conceptos" }, ...OPCIONES_CONCEPTO]}
            />
            <FiltroPill
              valor={medioPago}
              onChange={filtrar(setMedioPago)}
              ancho="w-[215px]"
              opciones={[{ value: TODOS, label: "Todos los medios de pago" }, ...OPCIONES_MEDIO]}
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm text-muted-foreground">Fecha del cobro</span>
          <div className="w-[170px]">
            <DateField name="desde" value={desde} onChange={(e) => filtrar(setDesde)(e.target.value)} />
          </div>
          <span className="text-sm text-muted-foreground">hasta</span>
          <div className="w-[170px]">
            <DateField name="hasta" value={hasta} onChange={(e) => filtrar(setHasta)(e.target.value)} />
          </div>
        </div>
      </div>

      {rangoInvalido && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          La fecha desde no puede ser posterior a la fecha hasta.
        </p>
      )}

      {!rangoInvalido && error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeDeError(error, "No se pudo cargar el listado de cobros.")}
        </p>
      )}

      {!rangoInvalido && cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando cobros...</p>}

      {!rangoInvalido && !cargando && !error && cobros.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros ? "Ningún cobro coincide con la búsqueda." : "Todavía no hay cobros registrados."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => navigate("/cobros/nuevo")}>
              <Plus />
              Registrar el primero
            </Button>
          )}
        </div>
      )}

      {!rangoInvalido && !cargando && !error && cobros.length > 0 && (
        <CobrosTabla
          cobros={cobros}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(c) => navigate(`/cobros/${c.id}`)}
          onImprimir={imprimir}
          onEmitirRecibo={emitir}
          onAnular={esAdmin ? setAAnular : undefined}
        />
      )}

      <AnularCobroDialog
        key={`anular-${aAnular?.id ?? "ninguno"}`}
        abierto={aAnular !== null}
        cobro={aAnular}
        onCerrar={() => setAAnular(null)}
        onAnulado={alAnular}
      />
    </div>
  );
}
