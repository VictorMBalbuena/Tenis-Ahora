import { useState } from "react";
import { Coins, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { SuccessBanner } from "@/components/ui/success-banner";
import { DateField } from "@/components/ui/date-field";
import { useValorDemorado } from "@/hooks/use-valor-demorado";
import { mensajeDeError } from "@/lib/erroresApi";
import { pesos, resumirRecibos } from "../model/recibo";
import { useRecibos } from "../hooks/useRecibos";
import { imprimirRecibo } from "../api/recibosApi";
import { RecibosTabla } from "../components/RecibosTabla";
import { AnularReciboDialog } from "../components/AnularReciboDialog";
import { KpiCard } from "@/components/ui/kpi-card";

/** Listado de recibos de pago: consulta, modificación, anulación e impresión (CU58-CU61). */
export function RecibosPage() {
  const navigate = useNavigate();
  const [busqueda, setBusqueda] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [pagina, setPagina] = useState(0);
  const [aviso, setAviso] = useState("");
  const [errorAccion, setErrorAccion] = useState("");
  const [aAnular, setAAnular] = useState(null);

  const busquedaDemorada = useValorDemorado(busqueda);

  const { recibos, totalPaginas, totalElementos, cargando, error, recargar } = useRecibos({
    busqueda: busquedaDemorada,
    desde,
    hasta,
    pagina,
  });

  const hayFiltros = busqueda !== "" || desde !== "" || hasta !== "";
  const resumen = resumirRecibos(recibos, totalElementos);

  function filtrar(setter) {
    return (valor) => {
      setter(valor);
      setPagina(0);
    };
  }

  function limpiarFiltros() {
    setBusqueda("");
    setDesde("");
    setHasta("");
    setPagina(0);
  }

  function alAnular(mensaje) {
    setAAnular(null);
    setAviso(mensaje);
    recargar();
  }

  async function imprimir(recibo) {
    setErrorAccion("");
    try {
      const pdf = await imprimirRecibo(recibo.id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setErrorAccion(mensajeDeError(e, "No se pudo generar el recibo."));
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Recibos de pago</h1>
        </div>
        <Button variant="outline" onClick={() => navigate("/cobros")}>
          <Coins />
          Ir a cobros
        </Button>
      </header>

      <SuccessBanner>{aviso}</SuccessBanner>

      {errorAccion && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorAccion}
        </p>
      )}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {[
          ["Recibos listados", String(resumen.total)],
          ["Vigentes en la página", String(resumen.vigentes)],
          ["Monto de la página", pesos(resumen.monto)],
        ].map(([etiqueta, valor]) => (
          <KpiCard key={etiqueta} etiqueta={etiqueta} valor={valor} />
        ))}
      </div>

      <div className="flex flex-col gap-3">
        <div className="relative flex-1 sm:max-w-sm">
          <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={busqueda}
            placeholder="N° de recibo o concepto..."
            className="h-10 rounded-full pl-11"
            onChange={(e) => filtrar(setBusqueda)(e.target.value)}
          />
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <span className="text-sm text-muted-foreground">Fecha de emisión</span>
          <div className="w-[170px]">
            <DateField name="desde" value={desde} onChange={(e) => filtrar(setDesde)(e.target.value)} />
          </div>
          <span className="text-sm text-muted-foreground">hasta</span>
          <div className="w-[170px]">
            <DateField name="hasta" value={hasta} onChange={(e) => filtrar(setHasta)(e.target.value)} />
          </div>
        </div>
      </div>

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeDeError(error, "No se pudo cargar el listado de recibos.")}
        </p>
      )}

      {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando recibos...</p>}

      {!cargando && !error && recibos.length === 0 && (
        <div className="space-y-3 rounded-xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">
            {hayFiltros ? "Ningún recibo coincide con la búsqueda." : "Todavía no se emitió ningún recibo."}
          </p>
          {hayFiltros ? (
            <Button variant="outline" onClick={limpiarFiltros}>
              Limpiar filtros
            </Button>
          ) : (
            <Button onClick={() => navigate("/cobros")}>
              <Coins />
              Ir a cobros a emitir el primero
            </Button>
          )}
        </div>
      )}

      {!cargando && !error && recibos.length > 0 && (
        <RecibosTabla
          recibos={recibos}
          pagina={pagina}
          totalPaginas={totalPaginas}
          totalElementos={totalElementos}
          onCambiarPagina={setPagina}
          onVer={(r) => navigate(`/recibos/${r.id}`)}
          onImprimir={imprimir}
          onAnular={setAAnular}
        />
      )}

      <AnularReciboDialog
        key={`anular-${aAnular?.id ?? "ninguno"}`}
        abierto={aAnular !== null}
        recibo={aAnular}
        onCerrar={() => setAAnular(null)}
        onAnulado={alAnular}
      />
    </div>
  );
}
