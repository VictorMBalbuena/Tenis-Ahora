import { Check, FileText } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { imprimirCobro } from "@/features/cobros/api/cobrosApi";
import { formatearMoneda } from "@/features/reservas/model/reserva";

const NOMBRE_MEDIO = { EFECTIVO: "Efectivo", DEBITO: "Débito", CREDITO: "Crédito", QR: "Mercado Pago" };

function Dato({ etiqueta, children }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="mt-1 text-sm font-medium text-foreground">{children}</dd>
    </div>
  );
}

export default function ReservaConfirmada({ reservaInfo, datosPago }) {
  const navigate = useNavigate();

  const verComprobante = async () => {
    const pdf = await imprimirCobro(datosPago.reciboId);
    window.open(URL.createObjectURL(pdf), "_blank");
  };

  return (
    <div className="w-full min-w-0 space-y-6">
      <div className="flex flex-col items-center rounded-xl border border-border bg-card p-8 text-center shadow-sm">
        <span className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]">
          <Check size={24} />
        </span>
        <h1 className="text-xl font-bold tracking-tight text-foreground">Pago registrado</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Reserva {reservaInfo.codigo} · {reservaInfo.cancha} · {reservaInfo.fecha} de {reservaInfo.horario}
        </p>
      </div>

      <div className="rounded-xl border border-border bg-card p-8 shadow-sm">
        <h2 className="mb-5 text-lg font-bold text-foreground">Detalle</h2>
        <dl className="grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2 lg:grid-cols-3">
          <Dato etiqueta="Titular">{reservaInfo.titular}</Dato>
          <Dato etiqueta="Medio de pago">{NOMBRE_MEDIO[datosPago.medio] ?? datosPago.medio}</Dato>
          {datosPago.operacion && <Dato etiqueta="Comprobante">{datosPago.operacion}</Dato>}
          <Dato etiqueta="Total del alquiler">{formatearMoneda(reservaInfo.total)}</Dato>
          <Dato etiqueta="Pagado">{formatearMoneda(reservaInfo.pagado)}</Dato>
          <Dato etiqueta="Saldo pendiente">{formatearMoneda(reservaInfo.saldo)}</Dato>
          {(reservaInfo.insumos ?? []).length > 0 && (
            <Dato etiqueta="Insumos">
              {reservaInfo.insumos.map((i) => `${i.nombre} × ${i.cantidad}`).join(", ")}
            </Dato>
          )}
          <Dato etiqueta="Jugadores">
            {reservaInfo.jugadores.map((j) => `${j.nombre} ${j.apellido}`).join(", ") || "—"}
          </Dato>
        </dl>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <button onClick={verComprobante} className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-full border border-input bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:bg-muted">
          <FileText size={16} /> Ver comprobante
        </button>
        <button onClick={() => navigate("/reservas")} className="inline-flex h-10 flex-1 items-center justify-center rounded-full bg-primary px-5 text-sm font-semibold text-primary-foreground transition-colors hover:brightness-95">
          Volver a reservas
        </button>
      </div>
    </div>
  );
}
