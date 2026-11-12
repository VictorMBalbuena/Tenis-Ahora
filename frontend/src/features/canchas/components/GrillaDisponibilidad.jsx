/** Horarios de 08:00 a 22:00, una franja por hora. */
export const HORARIOS = Array.from({ length: 15 }, (_, i) => `${(i + 8).toString().padStart(2, "0")}:00`);

export function datosDeCelda(grillaData, canchaId, hora) {
  const disponibilidad = grillaData[canchaId];
  if (!disponibilidad) return { estado: "Libre", celda: null };

  const listaFranjas = Array.isArray(disponibilidad) ? disponibilidad : disponibilidad.franjas || [];
  const celda = listaFranjas.find((f) => f.desde && f.desde.startsWith(hora));

  if (celda) {
    if (celda.estado === "BLOQUEADA") return { estado: "Mantenimiento", celda };
    if (celda.estado === "OCUPADA") {
      if (celda.tipo === "CLASE" || celda.origen === "CLASE" || celda.tipoOcupacion === "CLASE") {
        return { estado: "Clase", celda };
      }
      return { estado: "Reservada", celda };
    }
  }
  return { estado: "Libre", celda: null };
}

function estilosDeCelda(estado) {
  switch (estado) {
    case "Reservada":
      return "bg-emerald-50 border-emerald-200 text-emerald-700 font-semibold hover:bg-emerald-100 hover:border-emerald-400 cursor-pointer shadow-xs active:scale-95";
    case "Clase":
      return "bg-blue-50 border-blue-200 text-blue-700 font-semibold hover:bg-blue-100 hover:border-blue-400 cursor-pointer shadow-xs active:scale-95";
    case "Mantenimiento":
      return "bg-orange-50/70 border-orange-200 text-orange-600 font-semibold hover:bg-orange-100 hover:border-orange-400 cursor-pointer shadow-xs active:scale-95";
    default:
      return "bg-white border-gray-200 text-gray-400 hover:border-primary hover:bg-primary/5 hover:text-primary cursor-pointer active:scale-95";
  }
}

function tituloDeCelda(cancha, hora, estado) {
  switch (estado) {
    case "Reservada":
      return `${cancha.identificador} (${hora}): Turno reservado. Clic para ver la reserva.`;
    case "Clase":
      return `${cancha.identificador} (${hora}): Turno ocupado por clase o entrenamiento. Clic para ver actividades.`;
    case "Mantenimiento":
      return `${cancha.identificador} (${hora}): Cancha bloqueada por mantenimiento. Clic para desbloquear.`;
    default:
      return `${cancha.identificador} (${hora}): Turno libre. Clic para crear nueva reserva.`;
  }
}

/** Grilla de disponibilidad: una fila por cancha, una columna por franja horaria. */
export function GrillaDisponibilidad({ canchas, grillaData, onTurnoClick }) {
  return (
    <div className="rounded-xl border border-border bg-card shadow-sm p-6 mb-8 overflow-x-auto">
      <div className="min-w-[1000px]">
        <div className="flex mb-2 pl-24">
          {HORARIOS.map((hora) => (
            <div key={hora} className="flex-1 text-center text-[13px] font-bold text-gray-500">
              {hora}
            </div>
          ))}
        </div>
        <div className="space-y-3">
          {canchas.map((cancha) => (
            <div key={cancha.id} className="flex items-center h-12">
              <div className="w-24 flex-shrink-0 font-medium text-gray-700 text-sm truncate pr-2" title={cancha.identificador}>
                {cancha.identificador}
              </div>
              <div className="flex flex-1 gap-1 h-full">
                {HORARIOS.map((hora) => {
                  const { estado, celda } = datosDeCelda(grillaData, cancha.id, hora);
                  return (
                    <button
                      type="button"
                      key={`${cancha.id}-${hora}`}
                      onClick={() => onTurnoClick?.({ cancha, hora, estado, celda })}
                      title={tituloDeCelda(cancha, hora, estado)}
                      className={`flex-1 border rounded-lg flex items-center justify-center text-[11px] transition-all select-none ${estilosDeCelda(estado)}`}
                    >
                      {estado !== "Libre" && (estado === "Mantenimiento" ? "Mant." : estado)}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
