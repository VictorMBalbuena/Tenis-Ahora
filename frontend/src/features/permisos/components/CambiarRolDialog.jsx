import { useEffect, useState } from "react";
import { TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { ErrorBanner } from "@/components/ui/error-banner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { codigoDeError, mensajeDeError } from "@/lib/erroresApi";
import { asignarRol, listarRoles } from "../api/permisosApi";
import { ETIQUETA_ROL_BASE, OPCIONES_ROL_BASE, etiquetaDeRolBase } from "../model/permiso";

const SIN_ROL_PERSONALIZADO = "";

/**
 * Asignación de rol a alguien que ya existe (CU74 / RF74).
 *
 * Dos cosas que conviene avisarle a quien lo usa, y por eso están escritas en
 * la pantalla:
 *  - cambiar el rol base muda la cuenta de tabla, así que le cambia el número
 *    de usuario;
 *  - solo se puede si la persona todavía no dejó movimientos con su rol actual.
 *    Si los dejó, el backend lo rechaza y el camino es deshabilitarla.
 */
export function CambiarRolDialog({ abierto, cuenta, onCerrar, onCambiado }) {
  const [rol, setRol] = useState(cuenta?.rol ?? "SOCIO");
  const [rolPersonalizadoId, setRolPersonalizadoId] = useState(
    cuenta?.rolPersonalizadoId ? String(cuenta.rolPersonalizadoId) : SIN_ROL_PERSONALIZADO,
  );
  const [motivo, setMotivo] = useState("");
  const [roles, setRoles] = useState([]);
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    let vigente = true;
    listarRoles()
      .then((lista) => {
        if (vigente) setRoles(lista.filter((r) => !r.base));
      })
      .catch(() => {
        // Sin la lista igual se puede cambiar el rol base: los personalizados son opcionales
        if (vigente) setRoles([]);
      });
    return () => {
      vigente = false;
    };
  }, []);

  if (!cuenta) return null;

  // Un rol personalizado solo se puede asignar si se apoya en el rol base elegido
  const compatibles = roles.filter((r) => r.rolBase === rol);
  const cambiaDeTabla = rol !== cuenta.rol;

  function cambiarRolBase(valor) {
    setRol(valor);
    setRolPersonalizadoId(SIN_ROL_PERSONALIZADO);
  }

  async function confirmar() {
    setError("");
    setGuardando(true);
    try {
      const actualizada = await asignarRol(cuenta.id, {
        rol,
        rolPersonalizadoId: rolPersonalizadoId ? Number(rolPersonalizadoId) : null,
        motivo,
      });
      onCambiado(
        actualizada,
        `${actualizada.nombre} ${actualizada.apellido} ahora es ${etiquetaDeRolBase(actualizada.rol)}` +
          (actualizada.rolPersonalizadoNombre ? ` (${actualizada.rolPersonalizadoNombre}).` : "."),
      );
    } catch (e) {
      setError(
        codigoDeError(e) === "PERSONA_CON_HISTORIAL"
          ? mensajeDeError(e, "No se le puede cambiar el rol: ya tiene movimientos registrados.")
          : mensajeDeError(e, "No se pudo cambiar el rol."),
      );
    } finally {
      setGuardando(false);
    }
  }

  const opcionesPersonalizadas = [
    { value: SIN_ROL_PERSONALIZADO, label: `Permisos completos de ${ETIQUETA_ROL_BASE[rol] ?? rol}` },
    ...compatibles.map((r) => ({ value: String(r.id), label: r.nombre })),
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Cambiar el rol</DialogTitle>
          <DialogDescription>
            {cuenta.nombre} {cuenta.apellido} · DNI {cuenta.dni} · hoy es {etiquetaDeRolBase(cuenta.rol)}
          </DialogDescription>
        </DialogHeader>

        {error && <ErrorBanner>{error}</ErrorBanner>}

        <div className="space-y-5">
          <CampoFormulario id="cambiar-rol-base" etiqueta="Rol">
            {({ id }) => (
              <SelectField
                id={id}
                name="rol"
                opciones={OPCIONES_ROL_BASE}
                value={rol}
                onChange={(e) => cambiarRolBase(e.target.value)}
              />
            )}
          </CampoFormulario>

          {compatibles.length > 0 && (
            <CampoFormulario
              id="cambiar-rol-personalizado"
              etiqueta="Permisos"
              ayuda="Un rol creado por el club acota lo que puede hacer, sin cambiarle el rol."
            >
              {({ id }) => (
                <SelectField
                  id={id}
                  name="rolPersonalizadoId"
                  opciones={opcionesPersonalizadas}
                  value={rolPersonalizadoId}
                  onChange={(e) => setRolPersonalizadoId(e.target.value)}
                />
              )}
            </CampoFormulario>
          )}

          <CampoFormulario id="cambiar-rol-motivo" etiqueta="Motivo (opcional)">
            {({ id }) => (
              <input
                id={id}
                className={CLASE_INPUT}
                value={motivo}
                maxLength={300}
                placeholder="Ej: pasa a atender el mostrador"
                onChange={(e) => setMotivo(e.target.value)}
              />
            )}
          </CampoFormulario>

          {cambiaDeTabla && (
            <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <TriangleAlert size={18} className="mt-0.5 shrink-0" />
              <div className="space-y-1">
                <p className="font-medium">Cambiar de rol le cambia el número de usuario.</p>
                <p>
                  Cada rol se guarda por separado, así que la cuenta se mueve. Solo se puede si la persona
                  todavía no tiene reservas, inscripciones ni asistencias con su rol actual.
                </p>
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button type="button" onClick={confirmar} disabled={guardando}>
            {guardando ? "Cambiando…" : "Cambiar rol"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
