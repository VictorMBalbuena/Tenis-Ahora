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
import { eliminarInsumo } from "../api/insumosApi";
import { mensajeDeError } from "../api/errores";
import { CategoriaInsumoBadge } from "./CategoriaInsumoBadge";

/**
 * Baja de un insumo (CU101).
 *
 * Antes de confirmar se muestra el resumen de lo que se va a borrar, y si el
 * insumo tiene unidades asignadas a turnos en curso se avisa: borrarlo ahi
 * hace perder la trazabilidad del movimiento.
 */
export function EliminarInsumoDialog({
  abierto,
  insumo,
  onCerrar,
  onEliminado,
}) {
  // El padre remonta este dialogo con un `key`, asi que arranca limpio solo.
  const [error, setError] = useState("");
  const [borrando, setBorrando] = useState(false);

  if (!insumo) return null;

  const tieneUnidadesEnUso = insumo.enUso > 0;

  async function confirmar() {
    setError("");
    setBorrando(true);
    try {
      await eliminarInsumo(insumo.id);
      onEliminado(`Se elimino el insumo ${insumo.nombre}.`);
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo eliminar el insumo."));
    } finally {
      setBorrando(false);
    }
  }

  const resumen = [
    ["Insumo", insumo.nombre],
    ["Categoría", <CategoriaInsumoBadge key="categoria" categoria={insumo.categoria} />],
    ["Stock disponible", insumo.cantidadDisponible],
    ["En uso", insumo.enUso],
    ["Stock minimo", insumo.cantidadMinima],
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar insumo</DialogTitle>
          <DialogDescription>
            Se elimina el insumo y deja de asignarse a nuevas reservas. Esta
            accion no se puede deshacer.
          </DialogDescription>
        </DialogHeader>

        <dl className="divide-y divide-border rounded-xl border border-border text-sm">
          {resumen.map(([etiqueta, valor]) => (
            <div
              key={etiqueta}
              className="flex items-center justify-between gap-3 px-3.5 py-2.5"
            >
              <dt className="text-muted-foreground">{etiqueta}</dt>
              <dd className="font-medium tabular-nums">{valor}</dd>
            </div>
          ))}
        </dl>

        {tieneUnidadesEnUso && (
          <p className="flex items-start gap-2 rounded-lg bg-[var(--estado-aviso-bg)] p-3 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>
              Hay {insumo.enUso} unidades asignadas a turnos en curso. Conviene
              esperar a que finalicen para no perder la trazabilidad del
              movimiento.
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
            disabled={borrando}
            onClick={confirmar}
          >
            {borrando ? "Eliminando..." : "Eliminar insumo"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
