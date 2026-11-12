import { useState } from "react";
import { AlertCircle, LogOut, TriangleAlert } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { mensajeDeError } from "@/lib/erroresApi";
import { useAuthStore } from "@/store/authStore";
import { cerrarTodasMisSesiones } from "../api/perfilApi";

/**
 * Cerrar la sesión en todos los dispositivos.
 *
 * Revoca los refresh tokens de la cuenta, así que también cierra esta
 * sesión: por eso al terminar manda al login.
 */
export function SesionesCard() {
  const navigate = useNavigate();
  const logout = useAuthStore((estado) => estado.logout);
  const [abierto, setAbierto] = useState(false);
  const [error, setError] = useState("");
  const [cerrando, setCerrando] = useState(false);

  async function confirmar() {
    setError("");
    setCerrando(true);
    try {
      await cerrarTodasMisSesiones();
      logout();
      navigate("/login");
    } catch (e) {
      setError(mensajeDeError(e, "No se pudieron cerrar las sesiones."));
    } finally {
      setCerrando(false);
    }
  }

  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <div>
        <h2 className="text-lg font-bold text-foreground">Sesiones</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Si usaste el sistema en una computadora que no es tuya, o perdiste el teléfono, cerrá la sesión en todos
          los dispositivos.
        </p>
      </div>

      <Button type="button" variant="outline" onClick={() => setAbierto(true)}>
        <LogOut size={18} />
        Cerrar sesión en todos los dispositivos
      </Button>

      <Dialog open={abierto} onOpenChange={(v) => !v && setAbierto(false)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cerrar todas las sesiones</DialogTitle>
            <DialogDescription>
              Se cierra la sesión en todos los dispositivos donde esté abierta, incluido este.
            </DialogDescription>
          </DialogHeader>

          <p className="flex items-start gap-2.5 rounded-xl bg-[var(--estado-aviso-bg)] p-3.5 text-sm text-[var(--estado-aviso-fg)]">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" />
            <span>Vas a tener que volver a iniciar sesión con tu correo y tu contraseña.</span>
          </p>

          {error && (
            <p role="alert" className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {error}
            </p>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setAbierto(false)}>
              Cancelar
            </Button>
            <Button variant="destructive" disabled={cerrando} onClick={confirmar}>
              {cerrando ? "Cerrando..." : "Cerrar todas"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
