import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { eliminarUsuario } from "../api/usuariosApi";
import { nombreCompleto } from "../model/usuario";

/**
 * Diálogo de confirmación destructiva para la baja de un usuario (CU04).
 * Alerta sobre la revocación de credenciales y sesiones activas.
 */
export function EliminarUsuarioDialog({
  abierto = false,
  tipo = "SOCIO",
  usuario = null,
  onCerrar,
  onEliminado,
}) {
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState(null);

  const nombre = nombreCompleto(usuario);

  const manejarConfirmacion = async () => {
    if (!usuario?.id) return;
    setEliminando(true);
    setError(null);

    try {
      await eliminarUsuario(tipo, usuario.id);
      onEliminado(`El usuario ${nombre} fue dado de baja correctamente.`);
    } catch (err) {
      console.error("Error al eliminar usuario:", err);
      const mensaje =
        err.response?.data?.mensaje ||
        err.response?.data?.message ||
        "No se pudo completar la baja del usuario.";
      setError(mensaje);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <Dialog open={abierto} onOpenChange={(ab) => !ab && onCerrar()}>
      <DialogContent className="sm:max-w-md p-6 sm:p-8">
        <DialogHeader className="pb-2">
          <DialogTitle className="text-destructive flex items-center gap-2">
            <AlertTriangle className="size-5 shrink-0" />
            Dar de Baja Usuario
          </DialogTitle>
          <DialogDescription>
            ¿Estás seguro de que deseás eliminar a{" "}
            <strong className="text-foreground">{nombre}</strong>?
          </DialogDescription>
        </DialogHeader>

        <div className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive space-y-1.5 my-1">
          <p className="font-medium">Esta acción revoca todos los accesos inmediatamente:</p>
          <ul className="list-disc pl-4 text-xs opacity-90 space-y-1">
            <li>Se cerrará cualquier sesión activa que el usuario tenga abierta.</li>
            <li>No podrá volver a ingresar al sistema con sus credenciales.</li>
          </ul>
        </div>

        {error && (
          <div
            role="alert"
            className="rounded-lg bg-destructive/10 p-3.5 text-sm text-destructive"
          >
            {error}
          </div>
        )}

        <DialogFooter className="mt-6 pt-4 border-t border-border gap-2 sm:gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={onCerrar}
            disabled={eliminando}
          >
            Cancelar
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={manejarConfirmacion}
            disabled={eliminando}
          >
            {eliminando ? "Eliminando..." : "Confirmar Baja"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
