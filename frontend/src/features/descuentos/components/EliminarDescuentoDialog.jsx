import { useState } from "react";
import { AlertCircle, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { beneficio, etiqueta } from "../model/descuento";
import { eliminarDescuento } from "../api/descuentosApi";
import { mensajeDeError } from "../api/errores";

/**
 * Baja de un descuento (CU65).
 *
 * Si ya se aplico a cobros no se permite borrarlo: el cobro quedaria
 * apuntando a un descuento inexistente y se perderia la trazabilidad de lo
 * facturado. En ese caso el camino es desactivarlo.
 */
export function EliminarDescuentoDialog({
  abierto,
  descuento,
  onCerrar,
  onEliminado,
}) {
  // El padre remonta este dialogo con un `key`, asi que arranca limpio solo.
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!descuento) return null;

  const yaSeAplico = descuento.vecesAplicado > 0;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarDescuento(descuento.id);
      onEliminado(`Se elimino el descuento ${etiqueta(descuento)}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el descuento."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Descuento", etiqueta(descuento)],
    ["Beneficio", beneficio(descuento)],
    ["Estado", descuento.activo ? "Activo" : "Inactivo"],
    ["Veces aplicado", descuento.vecesAplicado],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar descuento</DialogTitle>
          <DialogDescription>
            Se elimina la regla y deja de ofrecerse al calcular un cobro. Esta
            accion no se puede deshacer.
          </DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([clave, valor]) => (
            <div
              key={clave}
              className="flex items-center justify-between gap-3 px-3.5 py-2.5"
            >
              <dt className="text-muted-foreground">{clave}</dt>
              <dd className="font-medium tabular-nums">{valor}</dd>
            </div>
          ))}
        </dl>

        {yaSeAplico && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>
              Ya se aplico a {descuento.vecesAplicado} cobros. Desactivalo en
              lugar de borrarlo, asi no se pierde el historial de lo facturado.
            </span>
          </p>
        )}

        {error && (
          <p
            role="alert"
            className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            {error}
          </p>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            variant="destructive"
            disabled={borrando || yaSeAplico}
            onClick={confirmar}
          >
            {borrando ? "Eliminando..." : "Eliminar descuento"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
