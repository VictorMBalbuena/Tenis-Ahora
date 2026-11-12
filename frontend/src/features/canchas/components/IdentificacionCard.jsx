import { SelectField } from "@/components/ui/select-field";
import { CampoLabel } from "@/components/ui/campo-label";
import { etiquetaSuperficie } from "../model/cancha";

/** Tarjeta de datos básicos de la cancha: nombre, tipo de cancha, superficie, capacidad y precio. */
export function IdentificacionCard({
  formData,
  onChange,
  tiposCancha = [],
  onTipoCanchaChange,
}) {
  const isTipoFijado = Boolean(formData.tipoCanchaId);

  const opcionesTipos = [
    { value: "", label: "Elija un tipo de cancha" },
    ...tiposCancha.map((t) => ({
      value: String(t.id),
      label: `${t.nombre} (${t.superficie || etiquetaSuperficie(t.tipoSuperficie)} • Cap: ${t.capacidad})`,
    })),
  ];

  const handlePrecioKeyDown = (e) => {
    if (e.key === "ArrowUp") {
      e.preventDefault();
      const val = Number(formData.precioPorHora) || 0;
      onChange({ target: { name: "precioPorHora", value: String(val + 500) } });
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      const val = Number(formData.precioPorHora) || 0;
      onChange({ target: { name: "precioPorHora", value: String(Math.max(0, val - 500)) } });
    }
  };

  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <h2 className="text-lg font-bold text-black">Identificación</h2>
        <span className="text-xs text-primary font-medium">
          La superficie y la capacidad están definidas por el tipo de cancha seleccionado.
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6">
        <div className="space-y-2 lg:col-span-2">
          <CampoLabel>Nombre</CampoLabel>
          <input
            type="text"
            name="identificador"
            placeholder="Ej: Cancha 1"
            value={formData.identificador}
            onChange={onChange}
            required
            className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
          />
        </div>

        <div className="space-y-2 lg:col-span-4">
          <CampoLabel info="Plantilla constructiva que determina superficie y capacidad.">
            Tipo de cancha
          </CampoLabel>
          <SelectField
            name="tipoCanchaId"
            value={formData.tipoCanchaId ? String(formData.tipoCanchaId) : ""}
            onChange={(e) => {
              if (onTipoCanchaChange) {
                onTipoCanchaChange(e.target.value);
              } else {
                onChange(e);
              }
            }}
            placeholder="Elija un tipo de cancha"
            opciones={opcionesTipos}
          />
        </div>

        <div className="space-y-2 lg:col-span-2">
          <CampoLabel info="Fijada por el tipo de cancha elegido.">
            Superficie <span className="text-xs text-muted-foreground font-normal">(fijada)</span>
          </CampoLabel>
          <input
            type="text"
            readOnly
            disabled
            value={formData.superficie || "-"}
            className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] bg-muted/40 text-foreground font-medium text-[15px] cursor-not-allowed select-none"
          />
        </div>

        <div className="space-y-2 lg:col-span-2">
          <CampoLabel info="Fijada por el tipo de cancha elegido.">
            Capacidad <span className="text-xs text-muted-foreground font-normal">(fijada)</span>
          </CampoLabel>
          <input
            type="text"
            readOnly
            disabled
            value={formData.capacidad ? `${formData.capacidad} jugadores` : "-"}
            className="w-full px-5 py-3.5 border border-gray-300 rounded-[20px] bg-muted/40 text-foreground font-medium text-[15px] cursor-not-allowed select-none"
          />
        </div>

        <div className="space-y-2 lg:col-span-2">
          <CampoLabel info="Valor monetario por hora. Flechas suben/bajan de a $500.">
            Precio por hora
          </CampoLabel>
          <div className="relative">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 font-semibold text-[15px] pointer-events-none">
              $
            </span>
            <input
              type="number"
              name="precioPorHora"
              min="0"
              step="500"
              placeholder="8000"
              value={formData.precioPorHora}
              onChange={onChange}
              onKeyDown={handlePrecioKeyDown}
              required
              className="w-full pl-8 pr-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px]"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
