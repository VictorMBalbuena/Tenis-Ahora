import { Home, IdCard, Pencil, Phone, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { OPCIONES_SEXO,
  dniFormateado,
  domicilioFormateado,
  fechaCorta,
  telefonoFormateado,
} from "../model/perfil";

function Seccion({ icono: Icono, titulo, children }) {
  return (
    <div className="space-y-3 sm:space-y-4">
      <div className="flex items-center gap-2 text-muted-foreground">
        <Icono size={16} />
        <h3 className="text-xs font-bold uppercase tracking-wide">{titulo}</h3>
      </div>
      <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
        {children}
      </dl>
    </div>
  );
}

function Dato({ etiqueta, valor }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-muted-foreground">{etiqueta}</dt>
      <dd className="mt-1 text-sm sm:text-[15px] font-medium text-foreground break-words">
        {valor || "—"}
      </dd>
    </div>
  );
}

/** Mis datos en solo lectura: lo que la persona ve al entrar, antes de tocar "Editar datos". */
export function DatosPerfilResumen({ perfil, onEditar }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 sm:p-6 lg:p-8 shadow-sm space-y-6 sm:space-y-8">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-bold text-foreground">Mis datos</h2>
        <Button type="button" variant="outline" onClick={onEditar}>
          <Pencil size={16} />
          Editar datos
        </Button>
      </div>

      <Seccion icono={IdCard} titulo="Datos personales">
        <Dato etiqueta="Nombre" valor={perfil.nombre} />
        <Dato etiqueta="Apellido" valor={perfil.apellido} />
        <Dato etiqueta="DNI" valor={dniFormateado(perfil.dni)} />
        <Dato
          etiqueta="Fecha de nacimiento"
          valor={fechaCorta(perfil.fechaNacimiento)}
        />
      </Seccion>

      <Seccion icono={UserRound} titulo="Datos deportivos">
        <Dato etiqueta="Sexo" valor={OPCIONES_SEXO.find((o) => o.value === perfil.sexo)?.label} />
      </Seccion>

      <Seccion icono={Phone} titulo="Contacto">
        <Dato etiqueta="Correo electrónico" valor={perfil.email} />
        <Dato
          etiqueta="Teléfono"
          valor={telefonoFormateado(perfil.codigoArea, perfil.numeroTelefono)}
        />
      </Seccion>

      <Seccion icono={Home} titulo="Domicilio">
        <Dato
          etiqueta="Calle y número"
          valor={domicilioFormateado(perfil.calle, perfil.numeroCalle)}
        />
        <Dato etiqueta="Localidad" valor={perfil.localidad} />
      </Seccion>
    </div>
  );
}
