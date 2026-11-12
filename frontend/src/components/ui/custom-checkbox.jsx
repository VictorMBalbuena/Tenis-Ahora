import { Check } from "lucide-react";

/**
 * Checkbox con el estilo redondo del diseño, en vez del input nativo.
 *
 * Sin `label` se dibuja solo la casilla: lo usa la matriz de permisos, donde el
 * texto ya lo ponen los encabezados de la fila y de la columna. En ese caso hay
 * que pasar `ariaLabel`, para que un lector de pantalla sepa qué casilla es.
 */
export function CustomCheckbox({ label, name, checked, onChange, disabled = false, ariaLabel }) {
  return (
    <label
      className={`flex items-center gap-3 group ${disabled ? "cursor-not-allowed opacity-60" : "cursor-pointer"}`}
    >
      <div className="relative flex items-center justify-center w-6 h-6">
        <input
          type="checkbox"
          name={name}
          checked={checked}
          onChange={onChange}
          disabled={disabled}
          aria-label={label ? undefined : ariaLabel}
          className="peer sr-only"
        />
        <div
          className={`w-6 h-6 rounded-full border-2 transition-colors flex items-center justify-center
          peer-focus-visible:ring-2 peer-focus-visible:ring-primary peer-focus-visible:ring-offset-2
          ${checked ? "bg-primary border-primary" : "bg-background border-input group-hover:border-primary"}`}
        >
          {checked && <Check size={14} className="text-white" strokeWidth={3} />}
        </div>
      </div>
      {label && <span className="text-[14.5px] text-foreground font-medium select-none">{label}</span>}
    </label>
  );
}
