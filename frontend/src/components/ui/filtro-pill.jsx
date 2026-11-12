import { SelectField } from "@/components/ui/select-field";

/**
 * Filtro de listado con forma de píldora (mismo Select que usa toda la app,
 * solo que con el estilo de botón compacto en vez del campo de formulario).
 */
export function FiltroPill({ id, opciones, valor, onChange, ancho = "w-[230px]", ...props }) {
  return (
    <SelectField
      id={id}
      name="filtro"
      value={valor}
      onChange={(e) => onChange(e.target.value)}
      opciones={opciones}
      className={`${ancho} h-10 rounded-full px-5 py-0 text-sm font-medium`}
      {...props}
    />
  );
}
