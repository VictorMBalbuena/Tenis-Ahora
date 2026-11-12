import { useState } from "react";
import { AlertCircle } from "lucide-react";
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
import { actualizarCategoriaInsumo, crearCategoriaInsumo } from "../api/insumosApi";
import { mensajeDeError } from "../api/errores";

/**
 * Alta o modificación de una categoría de insumo (ej. "Grip / overgrip").
 *
 * Mismo diálogo para los dos casos: si viene `categoria` es edición
 * (PATCH con el nombre nuevo), si no, alta (POST).
 */
export function CategoriaDialog({ abierto, categoria, onCerrar, onGuardada }) {
  const esModificacion = Boolean(categoria);
  const [nombre, setNombre] = useState(categoria?.nombre ?? "");
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  async function guardar(e) {
    e.preventDefault();
    if (!nombre.trim()) return;
    setError("");
    setGuardando(true);
    try {
      const guardada = esModificacion
        ? await actualizarCategoriaInsumo(categoria.id, nombre.trim())
        : await crearCategoriaInsumo(nombre.trim());
      setNombre("");
      onGuardada(guardada);
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo guardar la categoría."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog
      open={abierto}
      onOpenChange={(v) => {
        if (!v) {
          setError("");
          onCerrar();
        }
      }}
    >
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>{esModificacion ? "Modificar categoría" : "Nueva categoría"}</DialogTitle>
          <DialogDescription>
            {esModificacion
              ? "No afecta a los insumos que ya la tienen asignada."
              : "Se suma a la lista y queda disponible para cualquier insumo."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={guardar} className="space-y-3">
          {error && (
            <p role="alert" className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {error}
            </p>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="categoria-nombre">Nombre</Label>
            <Input
              id="categoria-nombre"
              autoFocus
              placeholder="Ej: Grip / overgrip"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando || !nombre.trim()}>
              {guardando ? "Guardando..." : esModificacion ? "Guardar cambios" : "Crear"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
