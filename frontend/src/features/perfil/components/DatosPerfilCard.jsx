import { Home, IdCard, Phone, UserRound } from "lucide-react";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { DateField } from "@/components/ui/date-field";
import { SelectField } from "@/components/ui/select-field";
import { OPCIONES_SEXO } from "../model/perfil";
import { dniFormateado, MOTIVO_DNI_FIJO } from "../model/perfil";

function Seccion({ icono: Icono, titulo, children }) {
  return (
    <div className="space-y-3 sm:space-y-4">
      <div className="flex items-center gap-2 text-muted-foreground">
        <Icono size={16} />
        <h3 className="text-xs font-bold uppercase tracking-wide">{titulo}</h3>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">{children}</div>
    </div>
  );
}

/**
 * Los datos propios editables.
 *
 * Mismo formulario que Usuarios, con dos diferencias: el DNI se muestra como
 * dato y no como campo, y no hay contraseña (se cambia en Configuración).
 */
export function DatosPerfilCard({ datos, errores, onChange }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 sm:p-6 lg:p-8 shadow-sm space-y-6 sm:space-y-8">
      <h2 className="text-lg font-bold text-foreground">Mis datos</h2>

      <Seccion icono={IdCard} titulo="Datos personales">
        <CampoFormulario id="perfil-nombre" etiqueta="Nombre" error={errores.nombre}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.nombre} onChange={onChange("nombre")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="perfil-apellido" etiqueta="Apellido" error={errores.apellido}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.apellido} onChange={onChange("apellido")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="perfil-dni" etiqueta="DNI" info={MOTIVO_DNI_FIJO}>
          {(props) => (
            <input
              {...props}
              readOnly
              className={`${CLASE_INPUT} cursor-not-allowed bg-muted text-muted-foreground`}
              value={dniFormateado(datos.dni)}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="perfil-nacimiento" etiqueta="Fecha de nacimiento" error={errores.fechaNacimiento}>
          {(props) => <DateField {...props} name="fechaNacimiento" value={datos.fechaNacimiento} onChange={onChange("fechaNacimiento")} />}
        </CampoFormulario>
      </Seccion>

      <Seccion icono={UserRound} titulo="Datos deportivos">
        <CampoFormulario id="perfil-sexo" etiqueta="Sexo" info="Define en qué categoría de torneo podés inscribirte." error={errores.sexo}>
          {() => (
            <SelectField
              id="perfil-sexo"
              name="sexo"
              value={datos.sexo}
              onChange={onChange("sexo")}
              opciones={OPCIONES_SEXO}
              placeholder="Elegí una opción"
            />
          )}
        </CampoFormulario>
      </Seccion>

      <Seccion icono={Phone} titulo="Contacto">
        <CampoFormulario
          id="perfil-email"
          etiqueta="Correo electrónico"
          info="Es el correo con el que iniciás sesión"
          error={errores.email}
        >
          {(props) => (
            <input {...props} type="email" className={CLASE_INPUT} value={datos.email} onChange={onChange("email")} />
          )}
        </CampoFormulario>

        <div className="grid grid-cols-3 gap-2">
          <div className="col-span-1 min-w-0">
            <CampoFormulario
              id="perfil-area"
              etiqueta={
                <>
                  <span className="sm:hidden">Cod. Área</span>
                  <span className="hidden sm:inline">Código de área</span>
                </>
              }
              info="Sin el 0"
              error={errores.codigoArea}
            >
              {(props) => (
                <input
                  {...props}
                  inputMode="numeric"
                  className={CLASE_INPUT}
                  value={datos.codigoArea}
                  placeholder="Ej: 11"
                  onChange={onChange("codigoArea")}
                />
              )}
            </CampoFormulario>
          </div>

          <div className="col-span-2 min-w-0">
            <CampoFormulario id="perfil-telefono" etiqueta="Teléfono" info="Sin el 15" error={errores.numeroTelefono}>
              {(props) => (
                <input
                  {...props}
                  inputMode="numeric"
                  className={CLASE_INPUT}
                  value={datos.numeroTelefono}
                  placeholder="Ej: 22786604"
                  onChange={onChange("numeroTelefono")}
                />
              )}
            </CampoFormulario>
          </div>
        </div>
      </Seccion>

      <Seccion icono={Home} titulo="Domicilio">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
            <div className="col-span-2 sm:col-span-3 min-w-0">
              <CampoFormulario id="perfil-calle" etiqueta="Calle" error={errores.calle}>
                {(props) => (
                  <input {...props} className={CLASE_INPUT} value={datos.calle} onChange={onChange("calle")} />
                )}
              </CampoFormulario>
            </div>
            <div className="col-span-1 min-w-0">
              <CampoFormulario id="perfil-numero-calle" etiqueta="Número" error={errores.numeroCalle}>
                {(props) => (
                  <input
                    {...props}
                    inputMode="numeric"
                    className={CLASE_INPUT}
                    value={datos.numeroCalle}
                    placeholder="Ej: 742"
                    onChange={onChange("numeroCalle")}
                  />
                )}
              </CampoFormulario>
            </div>
          </div>

          <CampoFormulario id="perfil-localidad" etiqueta="Localidad" error={errores.localidad}>
            {(props) => (
              <input {...props} className={CLASE_INPUT} value={datos.localidad} onChange={onChange("localidad")} />
            )}
          </CampoFormulario>
        </div>
      </Seccion>
    </div>
  );
}
