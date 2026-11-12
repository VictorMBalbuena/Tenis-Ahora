import { useState } from "react";
import { TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { ErrorBanner } from "@/components/ui/error-banner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { mensajeDeError } from "@/lib/erroresApi";
import { cambiarEstadoDeCuenta } from "../api/permisosApi";
import { validarCambioDeEstado } from "../model/permiso";

/**
 * Baja y alta lógica de una cuenta (CU76 / RF76).
 *
 * Deshabilitar no borra nada: la persona conserva su historial, pero deja de
 * poder entrar y se le cierran las sesiones abiertas. Es el camino para alguien
 * que ya tiene movimientos y por eso no se puede eliminar.
 */
export function EstadoCuentaDialog({ abierto, cuenta, onCerrar, onCambiado }) {
  const [motivo, setMotivo] = useState("");
  const [cancelarReservas, setCancelarReservas] = useState(false);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  if (!cuenta) return null;

  const deshabilitando = cuenta.activa;

  async function confirmar() {
    const { valido, errores: encontrados } = validarCambioDeEstado({ activa: !deshabilitando, motivo });
    if (!valido) {
      setErrores(encontrados);
      return;
    }

    setError("");
    setGuardando(true);
    try {
      const actualizada = await cambiarEstadoDeCuenta(cuenta.id, {
        activa: !deshabilitando,
        motivo,
        cancelarReservasFuturas: deshabilitando && cancelarReservas,
      });
      onCambiado(
        actualizada,
        deshabilitando
          ? `Se deshabilitó la cuenta de ${cuenta.nombre} ${cuenta.apellido}.`
          : `Se habilitó la cuenta de ${cuenta.nombre} ${cuenta.apellido}.`,
      );
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo cambiar el estado de la cuenta."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{deshabilitando ? "Deshabilitar cuenta" : "Habilitar cuenta"}</DialogTitle>
          <DialogDescription>
            {cuenta.nombre} {cuenta.apellido} · DNI {cuenta.dni}
          </DialogDescription>
        </DialogHeader>

        {error && <ErrorBanner>{error}</ErrorBanner>}

        <div className="space-y-5">
          {deshabilitando ? (
            <>
              <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                <TriangleAlert size={18} className="mt-0.5 shrink-0" />
                <div className="space-y-1">
                  <p className="font-medium">No se borra nada.</p>
                  <p>
                    La persona conserva todo su historial, pero deja de poder entrar y se le cierran las
                    sesiones abiertas. Se puede volver a habilitar cuando haga falta.
                  </p>
                </div>
              </div>

              <CampoFormulario id="estado-motivo" etiqueta="Motivo" error={errores.motivo}>
                {({ id }) => (
                  <input
                    id={id}
                    className={CLASE_INPUT}
                    value={motivo}
                    maxLength={300}
                    placeholder="Ej: dejó de ser socio del club"
                    onChange={(e) => {
                      setMotivo(e.target.value);
                      setErrores({});
                    }}
                  />
                )}
              </CampoFormulario>

              <CustomCheckbox
                label="Cancelar también sus reservas de hoy en adelante"
                name="cancelarReservas"
                checked={cancelarReservas}
                onChange={() => setCancelarReservas((v) => !v)}
              />
            </>
          ) : (
            <p className="text-sm text-muted-foreground">
              La cuenta vuelve a poder iniciar sesión con la misma contraseña que tenía.
            </p>
          )}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            type="button"
            variant={deshabilitando ? "destructive" : "default"}
            onClick={confirmar}
            disabled={guardando}
          >
            {guardando ? "Guardando…" : deshabilitando ? "Deshabilitar" : "Habilitar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
