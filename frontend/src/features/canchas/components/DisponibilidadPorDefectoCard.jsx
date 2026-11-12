import { TimeField } from "@/components/ui/time-field";
import { SelectField } from "@/components/ui/select-field";
import { CampoLabel } from "@/components/ui/campo-label";

/** Tarjeta de horario y estado con el que arranca una cancha recién creada. */
export function DisponibilidadPorDefectoCard({ formData, onChange, errorHorario }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6 flex flex-col">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-black">Disponibilidad por defecto</h2>
        {errorHorario && (
          <span className="text-xs text-[var(--estado-error-fg)] font-semibold">
            {errorHorario}
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-6">
        <div className="space-y-2">
          <label className="block text-[14px] font-semibold text-black">Apertura</label>
          <TimeField name="apertura" value={formData.apertura} onChange={onChange} pasoMinutos={60} />
        </div>

        <div className="space-y-2">
          <label className="block text-[14px] font-semibold text-black">Cierre</label>
          <TimeField name="cierre" value={formData.cierre} onChange={onChange} pasoMinutos={60} />
        </div>

        <div className="space-y-2">
          <CampoLabel info="Máximo 2 horas por alquiler.">Duración del turno</CampoLabel>
          <SelectField
            name="duracion"
            value={formData.duracion}
            onChange={onChange}
            opciones={[
              { value: "1", label: "1 hora" },
              { value: "1.5", label: "1 hora y media" },
              { value: "2", label: "2 horas" },
            ]}
          />
        </div>

        <div className="space-y-2">
          <label className="block text-[14px] font-semibold text-black">Estado inicial</label>
          <SelectField
            name="estado"
            value={formData.estado}
            onChange={onChange}
            opciones={[
              { value: "Disponible", label: "Activa" },
              { value: "Deshabilitada", label: "Deshabilitada" },
            ]}
          />
        </div>
      </div>
    </div>
  );
}
