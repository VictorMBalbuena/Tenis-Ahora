import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  ETIQUETA_CALCULO,
  beneficio,
  calcularSobre,
  etiqueta,
  formaDeCalculo,
  pesos,
} from "../model/descuento";
import { obtenerDescuento } from "../api/descuentosApi";
import { EstadoBadge } from "./DescuentosTabla";

/**
 * Detalle de un descuento (CU64).
 *
 * Incluye el simulador que pide el ticket: escribís un monto y ves el
 * original, lo que se bonifica y el total. Es la forma de que la aplicación
 * del descuento sea visible antes de que exista la pantalla de cobro.
 */
export function DescuentoDetalleDialog({ abierto, descuentoId, onCerrar }) {
  // Arranca cargando y solo se actualiza dentro del `then`: nunca se llama a
  // setState de forma síncrona dentro del efecto.
  const [descuento, setDescuento] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [monto, setMonto] = useState("24000");

  useEffect(() => {
    if (!descuentoId) return;

    let vigente = true;
    obtenerDescuento(descuentoId)
      .then((datos) => vigente && setDescuento(datos))
      .finally(() => vigente && setCargando(false));

    return () => {
      vigente = false;
    };
  }, [descuentoId]);

  const base = Number(monto) || 0;
  const bonificado = descuento ? calcularSobre(descuento, base) : 0;
  const total = base - bonificado;

  const forma = descuento ? formaDeCalculo(descuento) : null;

  const datos = descuento
    ? [
        ["Beneficio", beneficio(descuento)],
        ["Forma de cálculo", forma ? ETIQUETA_CALCULO[forma] : "Sin definir"],
        ["Veces aplicado", descuento.vecesAplicado],
      ]
    : [];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {descuento ? etiqueta(descuento) : "Detalle del descuento"}
          </DialogTitle>
          <DialogDescription>
            {descuento
              ? `Beneficio de ${beneficio(descuento)}`
              : "Cargando los datos del descuento."}
          </DialogDescription>
        </DialogHeader>

        {cargando && !descuento && (
          <p className="py-6 text-center text-sm text-muted-foreground">
            Cargando...
          </p>
        )}

        {descuento && (
          <div className="space-y-5">
            <div>
              <EstadoBadge activo={descuento.activo} />
            </div>

            <dl className="divide-y divide-border rounded-xl border border-border text-sm">
              {datos.map(([clave, valor]) => (
                <div
                  key={clave}
                  className="flex items-center justify-between gap-3 px-3.5 py-2.5"
                >
                  <dt className="text-muted-foreground">{clave}</dt>
                  <dd className="font-medium tabular-nums">{valor}</dd>
                </div>
              ))}
            </dl>

            <section className="space-y-2.5">
              <h3 className="text-sm font-medium">Condiciones</h3>
              <p className="rounded-xl border border-border p-3.5 text-sm">
                {descuento.condiciones || "Sin condiciones cargadas."}
              </p>
            </section>

            {/* Lo que pide el ticket: ver la aplicación sobre el monto */}
            <section className="space-y-3 rounded-xl border border-border p-3.5">
              <h3 className="text-sm font-medium">
                Cómo se aplica sobre un cobro
              </h3>

              <div className="space-y-1.5">
                <Label htmlFor="simulador-monto">Monto a cobrar</Label>
                <Input
                  id="simulador-monto"
                  type="number"
                  min={0}
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  aria-describedby="simulador-resultado"
                />
              </div>

              <dl
                id="simulador-resultado"
                aria-live="polite"
                className="space-y-1.5 text-sm"
              >
                <div className="flex justify-between gap-3">
                  <dt className="text-muted-foreground">Monto original</dt>
                  <dd className="font-mono tabular-nums">{pesos(base)}</dd>
                </div>
                <div className="flex justify-between gap-3">
                  <dt className="text-muted-foreground">Descuento</dt>
                  <dd className="font-mono tabular-nums text-[var(--estado-ok-fg)]">
                    −{pesos(bonificado)}
                  </dd>
                </div>
                <div className="flex justify-between gap-3 border-t border-border pt-1.5">
                  <dt className="font-medium">Total a cobrar</dt>
                  <dd className="font-mono font-semibold tabular-nums">
                    {pesos(total)}
                  </dd>
                </div>
              </dl>

              {!descuento.activo && (
                <p className="text-xs text-muted-foreground">
                  El descuento está inactivo: hoy no se aplicaría a ningún
                  cobro. Lo de arriba es solo una simulación.
                </p>
              )}
            </section>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
