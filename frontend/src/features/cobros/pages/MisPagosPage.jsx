import { useEffect, useState } from "react";
import { mensajeDeError } from "@/lib/erroresApi";
import { imprimirCobro, listarMisCobros } from "../api/cobrosApi";
import { CobrosTabla } from "../components/CobrosTabla";

// Registro de cobros del socio: las reservas donde es titular, sus inscripciones y sus torneos.
export function MisPagosPage() {
  const [pagina, setPagina] = useState(0);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);
  const [errorAccion, setErrorAccion] = useState("");

  useEffect(() => {
    let vigente = true;
    listarMisCobros({ page: pagina })
      .then((data) => {
        if (vigente) {
          setResultado(data);
          setError(null);
        }
      })
      .catch((e) => {
        if (vigente) setError(e);
      });
    return () => {
      vigente = false;
    };
  }, [pagina]);

  async function imprimir(cobro) {
    setErrorAccion("");
    try {
      const pdf = await imprimirCobro(cobro.id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setErrorAccion(mensajeDeError(e, "No se pudo generar el comprobante."));
    }
  }

  const cobros = resultado?.contenido ?? [];
  const cargando = !resultado && !error;

  return (
    <div className="space-y-6">
      <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Mis pagos</h1>

      {errorAccion && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {errorAccion}
        </p>
      )}

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {mensajeDeError(error, "No se pudieron cargar tus pagos.")}
        </p>
      )}

      {cargando && <p className="py-10 text-center text-sm text-muted-foreground">Cargando tus pagos...</p>}

      {!cargando && !error && cobros.length === 0 && (
        <p className="rounded-xl border border-dashed border-border py-12 text-center text-sm text-muted-foreground">
          Todavía no tenés pagos registrados.
        </p>
      )}

      {!error && cobros.length > 0 && (
        <CobrosTabla
          cobros={cobros}
          pagina={pagina}
          totalPaginas={resultado.totalPaginas}
          totalElementos={resultado.totalElementos}
          onCambiarPagina={setPagina}
          onImprimir={imprimir}
        />
      )}
    </div>
  );
}
