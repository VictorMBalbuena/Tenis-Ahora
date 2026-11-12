import { Wrench, X, CheckCircle, AlertCircle } from "lucide-react";

/**
 * Modal para desbloquear una cancha con mantenimiento activo.
 * Sigue la misma estética del diálogo de bloqueo con los datos de la cancha precargados.
 */
export function DesbloqueoMantenimientoDialog({
  abierto,
  bloqueoInfo,
  onCerrar,
  onDesbloquear,
  isLoading = false,
  error = "",
}) {
  if (!abierto || !bloqueoInfo) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm overflow-y-auto animate-in fade-in duration-200">
      <div className="bg-card rounded-3xl p-8 w-full max-w-2xl shadow-2xl relative my-8 border border-border">
        <button
          type="button"
          onClick={onCerrar}
          className="absolute right-6 top-6 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <X size={20} />
        </button>

        <div className="flex items-center gap-3 mb-2">
          <div className="p-2.5 rounded-2xl bg-orange-100 text-orange-600">
            <Wrench size={22} />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-foreground">Desbloquear cancha por mantenimiento</h2>
            <p className="text-muted-foreground text-sm">
              Datos del bloqueo actual registrado en el sistema.
            </p>
          </div>
        </div>

        {error && (
          <div className="my-4 p-4 rounded-2xl bg-red-50 text-red-700 text-sm flex items-center gap-2 border border-red-200">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 my-6">
          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Cancha
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.canchaNombre || bloqueoInfo.canchaIdentificador || "Cancha"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Motivo
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.motivo || "Mantenimiento general"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Desde
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.fechaDesde || bloqueoInfo.fecha || "-"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Hasta
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.fechaHasta || bloqueoInfo.fecha || "-"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Hora de inicio
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.horaInicio || "08:00"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
              Hora de fin
            </label>
            <input
              type="text"
              readOnly
              disabled
              value={bloqueoInfo.horaFin || "23:00"}
              className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[15px] cursor-not-allowed text-foreground"
            />
          </div>
        </div>

        <div className="space-y-1.5 mb-6">
          <label className="block text-xs font-bold text-muted-foreground uppercase tracking-wide">
            Observaciones registradas
          </label>
          <textarea
            readOnly
            disabled
            rows={2}
            value={bloqueoInfo.observaciones || "Bloqueo operativo para mantenimiento de cancha."}
            className="w-full px-4 py-3 border border-gray-300 rounded-2xl bg-muted/40 font-medium text-[14px] cursor-not-allowed text-foreground resize-none"
          />
        </div>

        <div className="flex justify-end gap-3 pt-6 border-t border-gray-100">
          <button
            type="button"
            onClick={onCerrar}
            disabled={isLoading}
            className="px-6 py-2.5 font-bold text-gray-700 hover:bg-gray-100 rounded-full transition-colors"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onDesbloquear}
            disabled={isLoading}
            className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-6 py-2.5 rounded-full font-bold transition-colors shadow-sm disabled:opacity-50"
          >
            <CheckCircle size={18} />
            {isLoading ? "Levantando..." : "Levantar bloqueo (Desbloquear)"}
          </button>
        </div>
      </div>
    </div>
  );
}

