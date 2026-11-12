import { AlertTriangle, Wrench, X } from "lucide-react";
import { DateField } from "@/components/ui/date-field";
import { TimeField } from "@/components/ui/time-field";
import { SelectField } from "@/components/ui/select-field";

/** Modal para bloquear una cancha por mantenimiento, con el aviso de conflicto si hay reservas superpuestas. */
export function BloqueoMantenimientoDialog({
  abierto,
  canchas,
  bloqueoData,
  onChange,
  estadoValidacion,
  reservasConflicto,
  onCerrar,
  onAplicar,
}) {
  if (!abierto) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-y-auto">
      <div className="bg-card rounded-3xl p-8 w-full max-w-2xl shadow-2xl relative my-8">
        <button onClick={onCerrar} className="absolute right-6 top-6 text-gray-400 hover:text-gray-600 transition-colors">
          <X size={20} />
        </button>
        <h2 className="text-2xl font-bold mb-8">Bloquear cancha por mantenimiento</h2>

        <div className="grid grid-cols-2 gap-6 mb-6">
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Cancha</label>
            <SelectField
              name="canchaId"
              value={String(bloqueoData.canchaId)}
              onChange={onChange}
              opciones={canchas.map((c) => ({ value: String(c.id), label: `${c.identificador} - ${c.superficie}` }))}
            />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Motivo</label>
            <SelectField
              name="motivo"
              value={bloqueoData.motivo}
              onChange={onChange}
              opciones={[
                { value: "Mantenimiento general", label: "Mantenimiento general" },
                { value: "Resellado de superficie", label: "Resellado de superficie" },
              ]}
            />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Desde</label>
            <DateField name="fechaDesde" value={bloqueoData.fechaDesde} onChange={onChange} />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Hasta</label>
            <DateField name="fechaHasta" value={bloqueoData.fechaHasta} onChange={onChange} />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Hora de inicio</label>
            <TimeField name="horaInicio" value={bloqueoData.horaInicio} onChange={onChange} />
          </div>
          <div className="space-y-2">
            <label className="block text-sm font-semibold">Hora de fin</label>
            <TimeField name="horaFin" value={bloqueoData.horaFin} onChange={onChange} />
          </div>
        </div>

        {estadoValidacion === "error" ? (
          <div className="space-y-6 animate-in fade-in zoom-in duration-200">
            <div className="bg-red-50 text-red-600 px-5 py-3 rounded-2xl flex items-center gap-2 text-sm font-medium">
              <AlertTriangle size={16} /> No se puede aplicar el bloqueo por superposición
            </div>
            <div className="space-y-3 max-h-48 overflow-y-auto">
              {reservasConflicto.length > 0 ? (
                reservasConflicto.map((reserva, idx) => (
                  <div key={idx} className="border border-gray-200 rounded-2xl p-4 flex justify-between items-center">
                    <div>
                      <p className="font-bold text-sm">{reserva.id || "Reserva Confirmada"}</p>
                      <p className="text-gray-500 text-sm">Fecha/Hora en conflicto</p>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-500">Hay reservas vigentes en el rango seleccionado.</p>
              )}
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            <div className="space-y-2">
              <label className="block text-sm font-semibold">Observaciones</label>
              <textarea
                name="observaciones"
                value={bloqueoData.observaciones}
                onChange={onChange}
                placeholder="Detalle del trabajo..."
                rows="3"
                className="w-full px-4 py-3 border border-gray-300 rounded-2xl focus:ring-2 focus:ring-primary focus:outline-none resize-none"
              ></textarea>
            </div>
          </div>
        )}

        <div className="flex justify-end gap-3 pt-6 mt-6 border-t border-gray-100">
          <button onClick={onCerrar} className="px-6 py-2.5 font-bold text-gray-700 hover:bg-gray-100 rounded-full transition-colors">
            Cancelar
          </button>
          {estadoValidacion !== "error" && (
            <button
              onClick={onAplicar}
              className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-white px-6 py-2.5 rounded-full font-bold transition-colors"
            >
              <Wrench size={18} /> Aplicar bloqueo
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
