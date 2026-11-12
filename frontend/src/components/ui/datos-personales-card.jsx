import { DateField } from "@/components/ui/date-field";
import { CampoFormulario, CLASE_INPUT } from "./campo-formulario";

// Datos personales de la persona: identidad, contacto, dirección y, en el alta, la contraseña inicial.
export function DatosPersonalesCard({ datos, errores, onChange, incluirPassword = false }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <h2 className="text-lg font-bold text-foreground">Datos personales</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <CampoFormulario id="prof-nombre" etiqueta="Nombre" obligatorio error={errores.nombre}>
          {(props) => <input {...props} className={CLASE_INPUT} value={datos.nombre} placeholder="Bruno" onChange={onChange("nombre")} />}
        </CampoFormulario>

        <CampoFormulario id="prof-apellido" etiqueta="Apellido" obligatorio error={errores.apellido}>
          {(props) => <input {...props} className={CLASE_INPUT} value={datos.apellido} placeholder="Tealdi" onChange={onChange("apellido")} />}
        </CampoFormulario>

        <CampoFormulario id="prof-dni" etiqueta="DNI" info="Sin puntos ni espacios" obligatorio error={errores.dni}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.dni} placeholder="44966422" inputMode="numeric" maxLength={8} onChange={(e) => onChange("dni")({ target: { value: e.target.value.replace(/\D/g, "") } })} />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-nacimiento" etiqueta="Fecha de nacimiento" error={errores.fechaNacimiento}>
          {(props) => <DateField {...props} value={datos.fechaNacimiento || ""} onChange={onChange("fechaNacimiento")} />}
        </CampoFormulario>

        <CampoFormulario id="prof-area" etiqueta="Código de área" info="Sin el 0" obligatorio error={errores.codigoArea}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.codigoArea || ""} placeholder="11" inputMode="numeric" maxLength={4} onChange={(e) => onChange("codigoArea")({ target: { value: e.target.value.replace(/\D/g, "") } })} />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-telefono" etiqueta="Número de teléfono" obligatorio error={errores.numeroTelefono}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.numeroTelefono} placeholder="2278-6604" inputMode="numeric" maxLength={10} onChange={(e) => onChange("numeroTelefono")({ target: { value: e.target.value.replace(/\D/g, "") } })} />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-calle" etiqueta="Calle" obligatorio error={errores.calle}>
          {(props) => <input {...props} className={CLASE_INPUT} value={datos.calle || ""} placeholder="Av. Rivadavia" onChange={onChange("calle")} />}
        </CampoFormulario>

        <CampoFormulario id="prof-numero" etiqueta="Número" obligatorio error={errores.numeroCalle}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.numeroCalle || ""} placeholder="4820" inputMode="numeric" maxLength={6} onChange={(e) => onChange("numeroCalle")({ target: { value: e.target.value.replace(/\D/g, "") } })} />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-email" etiqueta="Correo electrónico" obligatorio error={errores.email}>
          {(props) => (
            <input {...props} type="email" className={CLASE_INPUT} value={datos.email} placeholder="nombre@correo.com" onChange={onChange("email")} />
          )}
        </CampoFormulario>

        {incluirPassword && (
          <CampoFormulario id="prof-password" etiqueta="Contraseña inicial" info="Entre 8 y 72 caracteres" obligatorio error={errores.password}>
            {(props) => (
              <input {...props} type="password" className={CLASE_INPUT} value={datos.password} placeholder="Clave-segura-123" onChange={onChange("password")} />
            )}
          </CampoFormulario>
        )}
      </div>
    </div>
  );
}
