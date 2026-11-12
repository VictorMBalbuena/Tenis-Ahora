import { Banknote, CreditCard, Landmark, QrCode } from "lucide-react";
import { MedioPago, ETIQUETA_MEDIO } from "../model/cobro";

// El medio de pago lleva icono y texto: el icono solo no alcanza para distinguir débito de crédito.
const ICONOS = {
  [MedioPago.DEBITO]: Landmark,
  [MedioPago.CREDITO]: CreditCard,
  [MedioPago.QR]: QrCode,
  [MedioPago.EFECTIVO]: Banknote,
};

export function MedioPagoBadge({ medioPago }) {
  if (!medioPago) return <span className="text-muted-foreground">—</span>;

  const Icono = ICONOS[medioPago];

  return (
    <span className="inline-flex items-center gap-1.5 text-sm">
      {Icono && <Icono className="size-4 shrink-0 text-muted-foreground" />}
      {ETIQUETA_MEDIO[medioPago] ?? medioPago}
    </span>
  );
}
