import { useEffect, useState } from "react";
import { AlertTriangle, Ban, Printer, ReceiptText } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { VolverButton } from "@/components/ui/volver-button";
import { SuccessBanner } from "@/components/ui/success-banner";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";
import {
  ETIQUETA_ORIGEN,
  EstadoCobro,
  descripcionOrigen,
  desgloseDeCobro,
  esAnulable,
  fechaHora,
  origenDe,
  pesos,
} from "../model/cobro";
import { emitirRecibo, imprimirCobro, obtenerCobro } from "../api/cobrosApi";
import { ConceptoCobroBadge } from "../components/ConceptoCobroBadge";
import { EstadoCobroBadge } from "../components/EstadoCobroBadge";
import { MedioPagoBadge } from "../components/MedioPagoBadge";
import { DesgloseMontos } from "../components/DesgloseMontos";
import { AnularCobroDialog } from "../components/AnularCobroDialog";

const CLASE_BOTON = "w-full flex items-center justify-center gap-2 h-10 px-5 border border-input bg-background rounded-full text-sm font-semibold hover:bg-muted transition-colors";

/** Consultar un cobro (CU54), con su desglose, su recibo y las acciones del módulo. */
export default function CobroDetallePage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const esAdmin = useAuthStore((estado) => estado.user?.rol) === "ADMINISTRADOR";

  const [cobro, setCobro] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorGlobal, setErrorGlobal] = useState(false);
  const [error, setError] = useState("");
  const [aviso, setAviso] = useState("");
  const [emitiendo, setEmitiendo] = useState(false);
  const [aAnular, setAAnular] = useState(false);

  useEffect(() => {
    obtenerCobro(id)
      .then(setCobro)
      .catch(() => setErrorGlobal(true))
      .finally(() => setCargando(false));
  }, [id]);

  async function imprimir() {
    setError("");
    try {
      const pdf = await imprimirCobro(id);
      window.open(URL.createObjectURL(pdf), "_blank");
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo generar el comprobante."));
    }
  }

  async function alAnular(mensaje) {
    setAAnular(false);
    setAviso(mensaje);
    try {
      setCobro(await obtenerCobro(id));
    } catch (e) {
      setError(mensajeDeError(e, "No se pudieron actualizar los datos del cobro."));
    }
  }

  async function emitir() {
    setError("");
    setEmitiendo(true);
    try {
      const recibo = await emitirRecibo(id);
      setCobro((prev) => ({ ...prev, recibo }));
      setAviso(`Se emitió el recibo ${recibo.numeroRecibo}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo emitir el recibo."));
    } finally {
      setEmitiendo(false);
    }
  }

  if (cargando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center text-muted-foreground font-medium">
        Cargando datos...
      </div>
    );
  }

  if (errorGlobal || !cobro) {
    return (
      <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-muted-foreground">
        <AlertTriangle size={40} className="text-[var(--estado-aviso-fg)]" />
        <p>No se encontraron los datos del cobro.</p>
        <button onClick={() => navigate("/cobros")} className="text-primary font-bold hover:underline">
          Volver a cobros
        </button>
      </div>
    );
  }

  const desglose = desgloseDeCobro(cobro);

  const datos = [
    ["Tipo de cobro", ETIQUETA_ORIGEN[origenDe(cobro)] ?? "—"],
    ["Se cobró por", descripcionOrigen(cobro)],
    ["Fecha y hora", fechaHora(cobro.fecha)],
    ["Monto final", pesos(cobro.montoFinal)],
    ["N° de operación", cobro.numeroOperacion ?? "—"],
    ...(cobro.cuotas ? [["Cuotas", cobro.cuotas]] : []),
    ...(cobro.estado === EstadoCobro.ANULADO
      ? [
          ["Motivo de anulación", cobro.motivoAnulacion ?? "—"],
          ["Fecha de anulación", fechaHora(cobro.fechaAnulacion)],
        ]
      : []),
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em]">Cobro #{cobro.id}</h1>
        <div className="flex items-center gap-3">
          <ConceptoCobroBadge concepto={cobro.concepto} />
          <EstadoCobroBadge estado={cobro.estado} />
          <VolverButton to="/cobros" />
        </div>
      </div>

      <SuccessBanner>{aviso}</SuccessBanner>

      {error && (
        <p role="alert" className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive">
          {error}
        </p>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Datos del cobro</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-5">
              {datos.map(([etiqueta, valor]) => (
                <div key={etiqueta}>
                  <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
                  <dd className="mt-1 text-sm font-medium">{valor}</dd>
                </div>
              ))}
              <div>
                <dt className="text-xs text-muted-foreground">Medio de pago</dt>
                <dd className="mt-1">
                  <MedioPagoBadge medioPago={cobro.medioPago} />
                </dd>
              </div>
            </dl>
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Desglose</h2>
            <DesgloseMontos desglose={desglose} />
          </div>

          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Recibo</h2>
            {cobro.recibo ? (
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border p-3.5">
                <div>
                  <p className="text-sm font-medium tabular-nums">{cobro.recibo.numeroRecibo}</p>
                  <p className="text-xs text-muted-foreground">Emitido el {fechaHora(cobro.recibo.fechaEmision)}</p>
                </div>
                <button onClick={() => navigate(`/recibos/${cobro.recibo.id}`)} className="text-sm font-bold text-primary hover:underline">
                  Ver el recibo
                </button>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                Este cobro no tiene recibo.
              </p>
            )}
          </div>
        </div>

        <div>
          <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
            <h2 className="text-base font-bold tracking-tight mb-5">Acciones</h2>
            <div className="flex flex-col gap-2">
              <button onClick={imprimir} className={CLASE_BOTON}>
                <Printer size={18} /> Imprimir cobro
              </button>
              {cobro.estado === EstadoCobro.PAGADO && !cobro.recibo && (
                <button onClick={emitir} disabled={emitiendo} className={CLASE_BOTON}>
                  <ReceiptText size={18} /> {emitiendo ? "Emitiendo..." : "Emitir recibo"}
                </button>
              )}
              {esAdmin && esAnulable(cobro) && (
                <button
                  onClick={() => setAAnular(true)}
                  className="w-full flex items-center justify-center gap-2 h-10 px-5 rounded-full text-sm font-semibold text-[var(--estado-error-fg)] hover:bg-[var(--estado-error-bg)] transition-colors"
                >
                  <Ban size={18} /> Anular
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      <AnularCobroDialog
        abierto={aAnular}
        cobro={cobro}
        onCerrar={() => setAAnular(false)}
        onAnulado={alAnular}
      />
    </div>
  );
}
