import { useState, useEffect } from "react";
import { X, Loader2, AlertTriangle } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import axiosClient from "@/config/axiosClient";
import { mensajeDeError } from "@/lib/erroresApi";
import { formatearMoneda } from "@/features/reservas/model/reserva";

const SEGUNDOS_POR_DEFECTO = 600;
const INTERVALO_DE_VERIFICACION_MS = 5000;

function segundosHasta(venceEn) {
  if (!venceEn) return SEGUNDOS_POR_DEFECTO;
  const restante = Math.floor((new Date(venceEn).getTime() - Date.now()) / 1000);
  return Math.max(restante, 0);
}

export default function CobroQR({ reservaInfo, cobroId, onCancelar, onCambiarMedio, onPagoExitoso }) {
  const [tiempoRestante, setTiempoRestante] = useState(SEGUNDOS_POR_DEFECTO);
  const [estadoQR, setEstadoQR] = useState("CARGANDO"); // CARGANDO, ACTIVO, VENCIDO, RECHAZADO
  const [qrData, setQrData] = useState("");
  const [mensaje, setMensaje] = useState("Mercado Pago rechazó la operación.");
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    const solicitarQR = async () => {
      try {
        const { data } = await axiosClient.post(`/cobros/${cobroId}/qr`);
        setQrData(data.qrData);
        setTiempoRestante(segundosHasta(data.venceEn));
        setEstadoQR("ACTIVO");
      } catch (err) {
        setMensaje(mensajeDeError(err, "No se pudo generar el QR."));
        setEstadoQR("RECHAZADO");
      }
    };

    solicitarQR();
  }, [cobroId, intento]);

  useEffect(() => {
    if (estadoQR !== "ACTIVO") return;

    const timer = setInterval(() => {
      setTiempoRestante((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setEstadoQR("VENCIDO");
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [estadoQR]);

  // Sigue consultando con el QR vencido: un pago hecho en el último segundo igual tiene que confirmar la reserva.
  useEffect(() => {
    if (estadoQR !== "ACTIVO" && estadoQR !== "VENCIDO") return;

    const verificarPago = async () => {
      try {
        const { data } = await axiosClient.post(`/cobros/${cobroId}/verificacion`);

        if (data.estado === "PAGADO") {
          onPagoExitoso({ medio: data.medioPago, operacion: data.numeroOperacion, reciboId: data.id });
        } else if (data.estado === "RECHAZADO") {
          setMensaje("Mercado Pago rechazó la operación.");
          setEstadoQR("RECHAZADO");
        }
      } catch {
        console.log("Esperando pago...");
      }
    };

    const intervalId = setInterval(verificarPago, INTERVALO_DE_VERIFICACION_MS);
    return () => clearInterval(intervalId);
  }, [estadoQR, cobroId, onPagoExitoso]);

  const minutos = Math.floor(tiempoRestante / 60);
  const segundos = tiempoRestante % 60;
  const tiempoFormateado = `${minutos.toString().padStart(2, "0")}:${segundos.toString().padStart(2, "0")}`;

  return (
    <div className="w-full min-w-0 space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <h1 className="text-[28px] font-extrabold leading-none tracking-[-.02em] text-foreground">Pago con Mercado Pago</h1>
          <p className="mt-1 text-sm text-muted-foreground">{reservaInfo?.subtitulo}</p>
        </div>
        <button onClick={onCancelar} className="inline-flex h-10 shrink-0 items-center justify-center gap-2 self-start rounded-full border border-input bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:bg-muted sm:self-auto">
          <X size={16} /> Cancelar y volver
        </button>
      </div>

      <div className="flex w-full flex-col items-center rounded-xl border border-border bg-card p-8 text-center shadow-sm">
        {estadoQR === "CARGANDO" && (
          <div className="mb-6 flex h-[240px] w-[240px] max-w-full flex-col items-center justify-center rounded-xl border-2 border-dashed border-input">
            <Loader2 className="mb-2 animate-spin text-primary" size={32} />
            <p className="text-sm font-medium text-muted-foreground">Generando QR...</p>
          </div>
        )}

        {estadoQR === "ACTIVO" && (
          <div className="mb-6 flex h-[240px] w-[240px] max-w-full items-center justify-center rounded-xl border border-border bg-white p-4">
            <QRCodeSVG value={qrData} size={200} level="H" />
          </div>
        )}

        {estadoQR === "VENCIDO" && (
          <div className="mb-6 flex h-[240px] w-[240px] max-w-full flex-col items-center justify-center rounded-xl bg-[var(--estado-error-bg)] p-4 text-[var(--estado-error-fg)]">
            <AlertTriangle className="mb-2" size={40} />
            <p className="mb-1 font-bold">QR vencido</p>
            <p className="text-xs">El tiempo para pagar terminó.</p>
          </div>
        )}

        {estadoQR === "RECHAZADO" && (
          <div className="mb-6 flex h-[240px] w-[240px] max-w-full flex-col items-center justify-center rounded-xl bg-[var(--estado-error-bg)] p-4 text-[var(--estado-error-fg)]">
            <X className="mb-2" size={40} />
            <p className="mb-1 font-bold">Pago rechazado</p>
            <p className="text-xs">{mensaje}</p>
          </div>
        )}

        <p className="font-mono text-3xl font-semibold tabular-nums text-foreground">{formatearMoneda(reservaInfo?.importe ?? 0)}</p>
        <p className="mt-2 text-sm text-muted-foreground">Escaneá el código con la app de Mercado Pago.</p>

        {estadoQR === "ACTIVO" && (
          <a href={qrData} target="_blank" rel="noreferrer" className="mt-2 text-sm font-semibold text-primary hover:underline">
            Abrir el link de pago
          </a>
        )}

        <p className="mt-5 rounded-full bg-muted px-6 py-2 font-mono text-lg font-medium tabular-nums text-foreground">
          {estadoQR === "VENCIDO" ? "00:00" : tiempoFormateado}
        </p>

        {estadoQR === "RECHAZADO" && !onCambiarMedio && (
          <p className="mt-4 text-sm text-muted-foreground">Podés reintentar con un QR nuevo o pagar en el club.</p>
        )}

        <div className="mt-6 flex w-full flex-col justify-center gap-3 sm:flex-row">
          {(estadoQR === "VENCIDO" || estadoQR === "RECHAZADO") && (
            <button onClick={() => { setEstadoQR("CARGANDO"); setIntento((n) => n + 1); }} className="inline-flex h-10 items-center justify-center rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground transition-colors hover:brightness-95">
              Generar nuevo QR
            </button>
          )}
          {onCambiarMedio && (
            <button onClick={() => onCambiarMedio("DEBITO")} className="inline-flex h-10 items-center justify-center rounded-full border border-input bg-background px-6 text-sm font-semibold text-foreground transition-colors hover:bg-muted">
              Cambiar medio de pago
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
