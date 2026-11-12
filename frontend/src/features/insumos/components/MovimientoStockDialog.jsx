import { useState } from "react";
import { AlertCircle, Minus, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "cn";
import { TipoMovimiento } from "../model/insumo";
import { registrarMovimiento } from "../api/insumosApi";
import { mensajeDeError } from "../api/errores";

/**
 * Descontar (CU14) y reponer (CU15) stock a mano.
 *
 * El criterio de aceptacion del ticket pide que, al intentar descontar mas de
 * lo que hay, se muestre el error y el stock no cambie. Se valida en los dos
 * lados: aca para avisar antes de pedir nada, y en la API para que la regla
 * valga aunque la llamada venga de otro lugar.
 */
export function MovimientoStockDialog({
  abierto,
  insumo,
  onCerrar,
  onRegistrado,
}) {
  // El padre remonta este dialogo con un `key`, asi que arranca limpio solo.
  const [tipo, setTipo] = useState(TipoMovimiento.REPOSICION);
  const [cantidad, setCantidad] = useState(1);
  const [motivo, setMotivo] = useState("");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  if (!insumo) return null;

  const esDescuento = tipo === TipoMovimiento.DESCUENTO;
  const excedeElStock = esDescuento && cantidad > insumo.cantidadDisponible;
  const resultante = esDescuento
    ? insumo.cantidadDisponible - cantidad
    : insumo.cantidadDisponible + cantidad;

  async function registrar(e) {
    e.preventDefault();
    setError("");

    if (cantidad < 1) {
      setError("La cantidad tiene que ser al menos 1.");
      return;
    }
    if (excedeElStock) {
      setError(
        `Solo hay ${insumo.cantidadDisponible} unidades disponibles de ${insumo.nombre}.`,
      );
      return;
    }

    setGuardando(true);
    try {
      await registrarMovimiento(insumo.id, { tipo, cantidad, motivo });
      onRegistrado(
        esDescuento
          ? `Se descontaron ${cantidad} unidades de ${insumo.nombre}.`
          : `Se repusieron ${cantidad} unidades de ${insumo.nombre}.`,
      );
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo registrar el movimiento."));
    } finally {
      setGuardando(false);
    }
  }

  const opciones = [
    { valor: TipoMovimiento.REPOSICION, texto: "Reponer", icono: Plus },
    { valor: TipoMovimiento.DESCUENTO, texto: "Descontar", icono: Minus },
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Movimiento de stock</DialogTitle>
          <DialogDescription>
            {insumo.nombre} &mdash; {insumo.cantidadDisponible} unidades disponibles.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={registrar} noValidate className="space-y-5">
          <fieldset className="space-y-2">
            <legend className="mb-2 text-sm font-medium">Operacion</legend>
            <div className="grid grid-cols-2 gap-2">
              {opciones.map(({ valor, texto, icono: Icono }) => (
                <label
                  key={valor}
                  className={cn(
                    "flex cursor-pointer items-center justify-center gap-2 rounded-lg border p-3 text-sm font-medium transition-colors",
                    "has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
                    tipo === valor
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border hover:bg-muted",
                  )}
                >
                  <input
                    type="radio"
                    name="tipo-movimiento"
                    value={valor}
                    checked={tipo === valor}
                    onChange={() => setTipo(valor)}
                    className="sr-only"
                  />
                  <Icono className="size-4" />
                  {texto}
                </label>
              ))}
            </div>
          </fieldset>

          <div className="space-y-1.5">
            <Label htmlFor="movimiento-cantidad">Cantidad</Label>
            <Input
              id="movimiento-cantidad"
              type="number"
              min={1}
              max={esDescuento ? insumo.cantidadDisponible : undefined}
              value={cantidad}
              aria-invalid={excedeElStock || undefined}
              aria-describedby="movimiento-resultado"
              onChange={(e) => setCantidad(Number(e.target.value))}
            />
            <p
              id="movimiento-resultado"
              className={cn(
                "text-xs",
                excedeElStock ? "text-destructive" : "text-muted-foreground",
              )}
            >
              {excedeElStock
                ? `No se puede: solo hay ${insumo.cantidadDisponible} disponibles.`
                : `El stock disponible queda en ${resultante}.`}
            </p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="movimiento-motivo">Motivo</Label>
            <Input
              id="movimiento-motivo"
              value={motivo}
              placeholder="Compra proveedor, rotura, ajuste de inventario..."
              onChange={(e) => setMotivo(e.target.value)}
              aria-describedby="movimiento-motivo-ayuda"
            />
            <p
              id="movimiento-motivo-ayuda"
              className="text-xs text-muted-foreground"
            >
              Queda registrado en el historial del insumo.
            </p>
          </div>

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
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando || excedeElStock}>
              {guardando ? "Registrando..." : "Registrar movimiento"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
