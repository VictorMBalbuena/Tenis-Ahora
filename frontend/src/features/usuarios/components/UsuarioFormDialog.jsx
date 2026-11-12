import { useEffect, useState } from "react";
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
import { DateField } from "@/components/ui/date-field";
import { Field, FieldDescription, FieldLabel } from "@/components/ui/field";
import { crearUsuario, actualizarUsuario, obtenerUsuario } from "../api/usuariosApi";
import { ETIQUETA_TIPO, validarPersona } from "../model/usuario";

const ESTADO_INICIAL = {
  nombre: "",
  apellido: "",
  dni: "",
  fechaNacimiento: "",
  calle: "",
  numeroCalle: "",
  codigoArea: "",
  numeroTelefono: "",
  email: "",
  password: "",
};

/**
 * Campo accesible con etiqueta, descripción y mensaje de error vinculado por ARIA.
 */
function Campo({ id, etiqueta, ayuda, error, ancho = "", children }) {
  const idAyuda = ayuda ? `${id}-ayuda` : undefined;
  const idError = error ? `${id}-error` : undefined;

  return (
    <Field data-invalid={Boolean(error)} className={ancho}>
      <FieldLabel htmlFor={id}>{etiqueta}</FieldLabel>
      {children({
        id,
        "aria-describedby": [idAyuda, idError].filter(Boolean).join(" ") || undefined,
        "aria-invalid": error ? true : undefined,
      })}
      {ayuda && !error && (
        <FieldDescription id={idAyuda} className="text-xs text-muted-foreground">
          {ayuda}
        </FieldDescription>
      )}
      {error && (
        <FieldDescription id={idError} role="alert" className="text-xs font-medium text-destructive">
          {error}
        </FieldDescription>
      )}
    </Field>
  );
}

/**
 * Modal de Alta (CU01) y Modificación (CU02) de Usuarios.
 */
export function UsuarioFormDialog({
  abierto = false,
  tipo = "SOCIO",
  usuario = null,
  onCerrar,
  onGuardado,
}) {
  const esEdicion = Boolean(usuario?.id);
  const [form, setForm] = useState(ESTADO_INICIAL);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [cargandoDetalle, setCargandoDetalle] = useState(Boolean(abierto && usuario?.id));

  // Carga de datos si es edición
  useEffect(() => {
    if (!usuario?.id) return;

    let vigente = true;
    obtenerUsuario(tipo, usuario.id)
      .then((detalle) => {
        if (!vigente) return;
        setForm({
          nombre: detalle.nombre || "",
          apellido: detalle.apellido || "",
          dni: detalle.dni || "",
          fechaNacimiento: detalle.fechaNacimiento || "",
          calle: detalle.calle || "",
          numeroCalle: detalle.numeroCalle || "",
          codigoArea: detalle.codigoArea || "",
          numeroTelefono: detalle.numeroTelefono || "",
          email: detalle.email || "",
          password: "",
        });
      })
      .catch((err) => {
        if (!vigente) return;
        console.error("Error al obtener detalle para edición:", err);
        setErrorServidor("No se pudieron cargar todos los datos previos del usuario.");
      })
      .finally(() => {
        if (vigente) setCargandoDetalle(false);
      });

    return () => {
      vigente = false;
    };
  }, [usuario?.id, tipo]);

  const actualizarCampo = (campo, valor) => {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    if (errores[campo]) {
      setErrores((prev) => {
        const copia = { ...prev };
        delete copia[campo];
        return copia;
      });
    }
  };

  const manejarEnvio = async (e) => {
    e.preventDefault();
    setErrorServidor(null);

    const validacion = validarPersona(form, esEdicion);
    if (!validacion.valido) {
      setErrores(validacion.errores);
      return;
    }

    setGuardando(true);

    try {
      if (esEdicion) {
        // En PATCH solo mandamos campos modificados o requeridos
        const payloadPatch = {
          nombre: form.nombre.trim(),
          apellido: form.apellido.trim(),
          dni: form.dni.trim(),
          fechaNacimiento: form.fechaNacimiento || null,
          calle: form.calle.trim(),
          numeroCalle: form.numeroCalle.trim(),
          codigoArea: form.codigoArea.trim(),
          numeroTelefono: form.numeroTelefono.trim(),
          email: form.email.trim(),
        };
        await actualizarUsuario(tipo, usuario.id, payloadPatch);
        onGuardado(`Se modificaron con éxito los datos de ${form.nombre} ${form.apellido}.`);
      } else {
        // En POST mandamos PersonaRequest con password
        const payloadPost = {
          nombre: form.nombre.trim(),
          apellido: form.apellido.trim(),
          dni: form.dni.trim(),
          fechaNacimiento: form.fechaNacimiento,
          calle: form.calle.trim(),
          numeroCalle: form.numeroCalle.trim(),
          codigoArea: form.codigoArea.trim(),
          numeroTelefono: form.numeroTelefono.trim(),
          email: form.email.trim(),
          password: form.password,
        };
        await crearUsuario(tipo, payloadPost);
        onGuardado(`Se registró exitosamente a ${form.nombre} ${form.apellido}.`);
      }
    } catch (err) {
      console.error("Error al guardar usuario:", err);
      const mensaje = err.response?.data?.mensaje || err.response?.data?.message || err.message;

      if (err.response?.status === 409) {
        if (/dni/i.test(mensaje)) {
          setErrores((prev) => ({ ...prev, dni: "Este DNI ya está registrado en el club." }));
        } else if (/correo|email/i.test(mensaje)) {
          setErrores((prev) => ({ ...prev, email: "Este correo ya está en uso." }));
        } else {
          setErrorServidor(mensaje || "El DNI o el correo ya se encuentran registrados.");
        }
      } else if (err.response?.status === 400 && err.response?.data?.errores) {
        // Mapeo de errores de validación de Spring Boot
        const errMap = {};
        err.response.data.errores.forEach((item) => {
          if (item.campo) errMap[item.campo] = item.mensaje;
        });
        setErrores((prev) => ({ ...prev, ...errMap }));
      } else {
        setErrorServidor(mensaje || "Ocurrió un error inesperado al intentar guardar.");
      }
    } finally {
      setGuardando(false);
    }
  };

  const nombreRecursoSingular =
    tipo === "SOCIO" ? "Socio" : tipo === "EMPLEADO" ? "Empleado" : "Administrador";

  return (
    <Dialog open={abierto} onOpenChange={(ab) => !ab && onCerrar()}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto p-6 sm:p-8">
        <form onSubmit={manejarEnvio} noValidate>
          <DialogHeader className="pb-2">
            <DialogTitle>
              {esEdicion
                ? `Modificar ${nombreRecursoSingular}`
                : `Registrar Nuevo ${nombreRecursoSingular}`}
            </DialogTitle>
            <DialogDescription>
              {esEdicion
                ? "Actualizá la información personal y de contacto. Los campos vacíos no se modificarán."
                : `Completá los datos personales y de acceso para registrar un nuevo integrante en ${ETIQUETA_TIPO[tipo]}.`}
            </DialogDescription>
          </DialogHeader>

          {errorServidor && (
            <div
              role="alert"
              className="my-3 flex items-start gap-2.5 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <AlertCircle className="size-4 mt-0.5 shrink-0" />
              <span>{errorServidor}</span>
            </div>
          )}

          {cargandoDetalle ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              Cargando información del usuario...
            </p>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:gap-5 py-4 sm:grid-cols-2">
              <Campo id="campo-nombre" etiqueta="Nombre" error={errores.nombre}>
                {(props) => (
                  <Input
                    {...props}
                    value={form.nombre}
                    placeholder="Ej: Ana"
                    onChange={(e) => actualizarCampo("nombre", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo id="campo-apellido" etiqueta="Apellido" error={errores.apellido}>
                {(props) => (
                  <Input
                    {...props}
                    value={form.apellido}
                    placeholder="Ej: Gómez"
                    onChange={(e) => actualizarCampo("apellido", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo id="campo-dni" etiqueta="DNI" ayuda="7 u 8 dígitos" error={errores.dni}>
                {(props) => (
                  <Input
                    {...props}
                    value={form.dni}
                    placeholder="Ej: 30123456"
                    inputMode="numeric"
                    maxLength={8}
                    onChange={(e) => actualizarCampo("dni", e.target.value.replace(/\D/g, ""))}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-fecha-nac"
                etiqueta="Fecha de Nacimiento"
                error={errores.fechaNacimiento}
              >
                {(props) => (
                  <DateField
                    {...props}
                    name="fechaNacimiento"
                    value={form.fechaNacimiento}
                    onChange={(e) => actualizarCampo("fechaNacimiento", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-calle"
                etiqueta="Calle"
                error={errores.calle}
                ancho="sm:col-span-1"
              >
                {(props) => (
                  <Input
                    {...props}
                    value={form.calle}
                    placeholder="Ej: Av. Siempreviva"
                    onChange={(e) => actualizarCampo("calle", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-numero-calle"
                etiqueta="Número de Calle"
                error={errores.numeroCalle}
              >
                {(props) => (
                  <Input
                    {...props}
                    value={form.numeroCalle}
                    placeholder="Ej: 742"
                    onChange={(e) => actualizarCampo("numeroCalle", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-cod-area"
                etiqueta="Código de Área"
                ayuda="Sin 0 (ej: 11)"
                error={errores.codigoArea}
              >
                {(props) => (
                  <Input
                    {...props}
                    value={form.codigoArea}
                    placeholder="11"
                    onChange={(e) => actualizarCampo("codigoArea", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-telefono"
                etiqueta="Teléfono"
                ayuda="Sin 15 (ej: 12345678)"
                error={errores.numeroTelefono}
              >
                {(props) => (
                  <Input
                    {...props}
                    value={form.numeroTelefono}
                    placeholder="12345678"
                    onChange={(e) => actualizarCampo("numeroTelefono", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              <Campo
                id="campo-email"
                etiqueta="Correo Electrónico"
                error={errores.email}
                ancho={esEdicion ? "sm:col-span-2" : "sm:col-span-1"}
              >
                {(props) => (
                  <Input
                    {...props}
                    type="email"
                    value={form.email}
                    placeholder="ejemplo@correo.com"
                    onChange={(e) => actualizarCampo("email", e.target.value)}
                    disabled={guardando}
                  />
                )}
              </Campo>

              {!esEdicion && (
                <Campo
                  id="campo-password"
                  etiqueta="Contraseña Inicial"
                  ayuda="Mínimo 8 caracteres"
                  error={errores.password}
                  ancho="sm:col-span-1"
                >
                  {(props) => (
                    <Input
                      {...props}
                      type="password"
                      value={form.password}
                      placeholder="••••••••"
                      onChange={(e) => actualizarCampo("password", e.target.value)}
                      disabled={guardando}
                    />
                  )}
                </Campo>
              )}
            </div>
          )}

          <DialogFooter className="mt-6 pt-4 border-t border-border gap-2 sm:gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={onCerrar}
              disabled={guardando}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando || cargandoDetalle}>
              {guardando
                ? "Guardando..."
                : esEdicion
                ? "Guardar Cambios"
                : "Crear Usuario"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
