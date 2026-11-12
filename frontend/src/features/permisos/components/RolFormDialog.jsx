import { useState } from "react";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { Button } from "@/components/ui/button";
import { ErrorBanner } from "@/components/ui/error-banner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { camposDeError, mensajeDeError } from "@/lib/erroresApi";
import { crearRol, renombrarRol } from "../api/permisosApi";
import { OPCIONES_ROL_BASE, validarRol } from "../model/permiso";

/**
 * Alta de un rol nuevo (CU85 / RF85) y cambio de nombre de uno ya creado.
 *
 * El rol se apoya siempre en uno de los cinco roles base, porque la persona
 * tiene que vivir en alguna de las cinco tablas de rol. Ese rol base también es
 * el techo: el rol nuevo nunca puede dar un permiso que el base no tenga.
 */
export function RolFormDialog({ abierto, rol, onCerrar, onGuardado }) {
  const editando = !!rol;
  const [datos, setDatos] = useState({
    nombre: rol?.nombre ?? "",
    descripcion: rol?.descripcion ?? "",
    rolBase: rol?.rolBase ?? "EMPLEADO",
  });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  function cambiar(campo, valor) {
    setDatos((previos) => ({ ...previos, [campo]: valor }));
    setErrores((previos) => ({ ...previos, [campo]: undefined }));
  }

  async function guardar(evento) {
    evento.preventDefault();
    const { valido, errores: encontrados } = validarRol(datos);
    if (!valido) {
      setErrores(encontrados);
      return;
    }

    setError("");
    setGuardando(true);
    try {
      const guardado = editando
        ? await renombrarRol(rol.id, { nombre: datos.nombre, descripcion: datos.descripcion })
        : await crearRol({ ...datos, permisos: { modulos: {}, especiales: [] } });

      onGuardado(
        guardado,
        editando
          ? `Se actualizó el rol "${guardado.nombre}".`
          : `Se creó el rol "${guardado.nombre}". Ahora marcale los permisos.`,
      );
    } catch (e) {
      setError(mensajeDeError(e, "No se pudo guardar el rol."));
      setErrores(camposDeError(e));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent>
        <form onSubmit={guardar} className="space-y-5">
          <DialogHeader>
            <DialogTitle>{editando ? "Editar rol" : "Nuevo rol"}</DialogTitle>
            <DialogDescription>
              {editando
                ? "Cambiá el nombre o la descripción. Los permisos se marcan en la grilla."
                : "El rol arranca sin permisos: después se marcan en la grilla."}
            </DialogDescription>
          </DialogHeader>

          {error && <ErrorBanner>{error}</ErrorBanner>}

          <CampoFormulario id="rol-nombre" etiqueta="Nombre" error={errores.nombre}>
            {({ id }) => (
              <input
                id={id}
                className={CLASE_INPUT}
                value={datos.nombre}
                maxLength={60}
                placeholder="Ej: Recepción"
                onChange={(e) => cambiar("nombre", e.target.value)}
              />
            )}
          </CampoFormulario>

          <CampoFormulario id="rol-descripcion" etiqueta="Descripción" error={errores.descripcion}>
            {({ id }) => (
              <input
                id={id}
                className={CLASE_INPUT}
                value={datos.descripcion}
                maxLength={200}
                placeholder="Ej: Atiende el mostrador pero no anula cobros"
                onChange={(e) => cambiar("descripcion", e.target.value)}
              />
            )}
          </CampoFormulario>

          {!editando && (
            <CampoFormulario
              id="rol-base"
              etiqueta="Se apoya en"
              error={errores.rolBase}
              info="Define en qué tabla vive la persona y cuál es el máximo de permisos que puede tener este rol."
            >
              {({ id }) => (
                <SelectField
                  id={id}
                  name="rolBase"
                  opciones={OPCIONES_ROL_BASE}
                  value={datos.rolBase}
                  onChange={(e) => cambiar("rolBase", e.target.value)}
                />
              )}
            </CampoFormulario>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando}>
              {guardando ? "Guardando…" : editando ? "Guardar" : "Crear rol"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
