import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "cn";

/**
 * Select con nuestro propio estilo (bordes bien redondeados, acento verde),
 * en vez de dejar el `<select>` nativo con su propio desplegable (gris/azul
 * según el navegador y sin forma de restylearlo).
 *
 * API de input controlado estándar: onChange recibe un evento sintético con
 * target.name/target.value, igual que un `<select>` nativo, para no tener que
 * cambiar nada del lado de quien ya usa `const { name, value } = e.target`.
 */
export function SelectField({ id, name, value, onChange, opciones, placeholder, className, disabled }) {
  const elegir = (nuevoValor) => {
    onChange({ target: { name, value: nuevoValor } });
  };

  return (
    <Select value={value} onValueChange={elegir} items={opciones} disabled={disabled}>
      <SelectTrigger
        id={id}
        disabled={disabled}
        className={cn(
          "w-full justify-between rounded-[20px] border-gray-300 px-5 py-3.5 h-auto text-[15px] font-normal",
          disabled && "opacity-60 cursor-not-allowed bg-muted/40",
          className
        )}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {opciones.map((opcion) => (
          <SelectItem key={opcion.value} value={opcion.value}>
            {opcion.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
