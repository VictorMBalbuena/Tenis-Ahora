import { useEffect, useState } from "react";
import { AlertTriangle, Coins, Printer, Trash2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { Badge } from "@/components/ui/badge";
import { mensajeDeError } from "@/lib/erroresApi";
import {
  conceptoLegible,
  esEditable,
  estaAnulado,
  fechaHora,
  medioLegible,
  numero,
  pesos,
} from "../model/recibo";
import { imprimirRecibo, obtenerRecibo } from "../api/recibosApi";
import { AnularReciboDialog } from "../components/AnularReciboDialog";

const CLASE_BOTON = "w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors";

/** Consultar un recibo de pago (CU59). */
export default function ReciboDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [recibo, setRecibo] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [error, setError] = useState("");
  const [aAnular, setAAnular] = useState(false);

  useEffect(() => {
    obtenerRecibo(id)
      .then(setRecibo)
      .catch(() => setErrorGlobal(true))
      .finally(() => setCargando(false));
  }, [id]);

  async function imprimir() {
    setError("");
    try {
      const pdf = await imprimirRecibo(id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo generar el recibo."));
    }
  }

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">
        Cargando datos...
      </div>
    );
  }

  if (errorGlobal || !recibo) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del recibo.</p>
        <button onClick={() => navigate("/recibos")} className="text-primary font-bold hover:underline">
          Volver a recibos
        </button>
      </div>
    );
  }

  const datos = [
    ["N° de recibo", numero(recibo)],
    ["Concepto", conceptoLegible(recibo)],
    ["Monto", pesos(recibo.monto)],
    ["Medio de pago", medioLegible(recibo)],
    ["N° de operación", recibo.numeroOperacion ?? "—"],
    ["Fecha de emisión", fechaHora(recibo.fechaEmision)],
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Recibo {numero(recibo)}</h1>
        <div className="flex items-center gap-3">
          {estaAnulado(recibo) && (
            <Badge className="bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]">Anulado</Badge>
          )}
          <VolverButton to="/recibos" />
        </div>
      </div>

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {error}
        </p>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Datos del recibo</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              {datos.map(([etiqueta, valor]) => (
                <div key={etiqueta}>
                  <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
                  <dd className="mt-1 text-sm font-medium">{valor}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Cobro que lo originó</h2>
            {recibo.cobro ? (
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border p-3.5">
                <div>
                  <p className="text-sm font-medium">Cobro {recibo.cobro.codigo}</p>
                  <p className="text-xs text-muted-foreground">
                    {pesos(recibo.cobro.montoFinal)} · {fechaHora(recibo.cobro.fecha)}
                  </p>
                </div>
                <button onClick={() => navigate(`/cobros/${recibo.cobro.id}`)} className="text-sm font-bold text-primary hover:underline">
                  Ver el cobro
                </button>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">No se encontró el cobro de este recibo.</p>
            )}
          </div>
        </div>

        <div>
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Acciones</h2>
            <div className="flex flex-col gap-2">
              <button onClick={imprimir} className={CLASE_BOTON}>
                <Printer size={18} /> Imprimir recibo
              </button>
              {recibo.cobro && (
                <button onClick={() => navigate(`/cobros/${recibo.cobro.id}`)} className={CLASE_BOTON}>
                  <Coins size={18} /> Ver el cobro
                </button>
              )}
              {esEditable(recibo) && (
                <button
                  onClick={() => setAAnular(true)}
                  className="w-full flex items-center justify-center gap-2 h-10 px-5 rounded-full text-sm font-semibold text-[var(--estado-error-fg)] hover:bg-[var(--estado-error-bg)] transition-colors"
                >
                  <Trash2 size={18} /> Anular
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      <AnularReciboDialog
        abierto={aAnular}
        recibo={recibo}
        onCerrar={() => setAAnular(false)}
        onAnulado={() => navigate("/recibos")}
      />
    </div>
  );
}
