import { CustomCheckbox } from "@/components/ui/custom-checkbox";

/** Tarjeta de características opcionales de la cancha y observaciones libres. */
export function CaracteristicasCard({ formData, onChange }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6 flex flex-col">
      <h2 className="text-lg font-bold text-black">Características</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-5">
        <CustomCheckbox label="Iluminación nocturna" name="iluminacion" checked={formData.iluminacion} onChange={onChange} />
        <CustomCheckbox label="Techada" name="techada" checked={formData.techada} onChange={onChange} />
        <CustomCheckbox label="Tribuna para público" name="tribuna" checked={formData.tribuna} onChange={onChange} />
        <CustomCheckbox label="Apta para torneos oficiales" name="torneos" checked={formData.torneos} onChange={onChange} />
      </div>

      <div className="flex flex-1 flex-col space-y-2">
        <label className="block text-[14px] font-semibold text-black">Observaciones</label>
        <textarea
          name="observaciones"
          placeholder="Detalles de la superficie, mantenimiento previsto..."
          value={formData.observaciones}
          onChange={onChange}
          className="w-full flex-1 min-h-[88px] px-5 py-3.5 border border-gray-300 rounded-[20px] focus:outline-none focus:ring-2 focus:ring-primary text-[15px] resize-none"
        ></textarea>
      </div>
    </div>
  );
}
