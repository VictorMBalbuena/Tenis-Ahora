const SOLO_LETRAS = /^\p{L}[\p{L} .'’-]*$/u;

/** De 2026-11-20 a 20/11/2026. */
export function formatearFecha(iso) {
  if (!iso) return "—";
  const [anio, mes, dia] = String(iso).split("-");
  return `${dia}/${mes}/${anio}`;
}

function minutosDe(hora) {
  const [h, m = 0] = String(hora).split(":").map(Number);
  return h * 60 + m;
}

/** Hora de fin de un turno que empieza en punto y dura horas enteras. */
export function horaFinDe(horaInicio, duracionHoras) {
  const fin = minutosDe(horaInicio) + Number(duracionHoras) * 60;
  return `${String(Math.floor(fin / 60)).padStart(2, "0")}:00`;
}

/** Un turno se puede elegir si no pasó todavía y está libre durante toda la duración pedida. */
export function turnoDisponible(franjas, indice, { duracionHoras = 1, fecha = null, ahora = new Date() } = {}) {
  const inicio = franjas[indice];
  if (inicio?.estado !== "LIBRE") return false;
  const hoy = `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, "0")}-${String(ahora.getDate()).padStart(2, "0")}`;
  if (fecha === hoy && minutosDe(inicio.desde) <= ahora.getHours() * 60 + ahora.getMinutes()) return false;
  for (let i = 1; i < duracionHoras; i++) {
    const siguiente = franjas[indice + i];
    if (siguiente?.estado !== "LIBRE" || siguiente.desde !== franjas[indice + i - 1].hasta) return false;
  }
  return true;
}

export const ESTADOS_RESERVA = {
  PENDIENTE_PAGO: {
    label: "Turno retenido",
    clase: "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
  },
  CONFIRMADA: {
    label: "Confirmada",
    clase: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  },
  EN_CURSO: {
    label: "En curso",
    clase: "bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]",
  },
  CANCELADA: {
    label: "Cancelada",
    clase: "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
  },
  FINALIZADA: {
    label: "Finalizada",
    clase: "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
  },
};

/**
 * Valida que la fecha no sea pasada ni supere la antelación máxima que informa el servidor (RD06).
 */
export function validarFechaAntelacion(fecha, hoy = new Date(), antelacionMaximaDias = null) {
  if (!fecha || typeof fecha !== "string") {
    return { valida: false, error: "La fecha es obligatoria." };
  }

  const partes = fecha.split("-").map(Number);
  if (partes.length !== 3 || partes.some(isNaN)) {
    return { valida: false, error: "Formato de fecha inválido." };
  }
  const [targetYear, targetMonth, targetDay] = partes;
  const targetUtc = Date.UTC(targetYear, targetMonth - 1, targetDay);

  const baseYear = hoy.getFullYear();
  const baseMonth = hoy.getMonth();
  const baseDay = hoy.getDate();
  const baseUtc = Date.UTC(baseYear, baseMonth, baseDay);

  const diffDays = Math.round((targetUtc - baseUtc) / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    return { valida: false, error: "No se pueden realizar reservas en fechas pasadas." };
  }
  if (antelacionMaximaDias != null && diffDays > antelacionMaximaDias) {
    return { valida: false, error: `No se puede reservar con más de ${antelacionMaximaDias} días de anticipación.` };
  }

  return { valida: true };
}

/**
 * Valida que la composición de acompañantes sea exactamente 1 para SINGLE o 3 para DOBLES (RD07),
 * que los invitados externos tengan nombre y apellido completos,
 * y que no existan socios duplicados ni coincidentes con el titular.
 */
export function validarAcompaniantes(modalidad, acompaniantes, opciones = {}) {
  const { titularId = null } = opciones;
  if (!Array.isArray(acompaniantes)) {
    return { valido: false, error: "La lista de acompañantes es obligatoria." };
  }

  const mod = modalidad?.toUpperCase();
  const esperado = mod === "SINGLE" ? 1 : mod === "DOBLES" ? 3 : -1;

  if (esperado === -1) {
    return { valido: false, error: "Modalidad inválida." };
  }

  if (acompaniantes.length !== esperado) {
    return {
      valido: false,
      error: `La modalidad ${mod === "SINGLE" ? "Single" : "Dobles"} requiere exactamente ${esperado} acompañante${esperado > 1 ? "s" : ""}.`,
    };
  }

  const sociosVistos = new Set();
  if (titularId != null) {
    sociosVistos.add(Number(titularId));
  }

  for (let i = 0; i < acompaniantes.length; i++) {
    const a = acompaniantes[i];
    if (!a) {
      return { valido: false, error: "Datos de acompañante inválidos." };
    }
    if (a.personaId == null) {
      if (!a.nombre?.trim() || !a.apellido?.trim()) {
        return {
          valido: false,
          error: "Los invitados externos deben tener nombre y apellido completos.",
        };
      }
      if (!SOLO_LETRAS.test(a.nombre.trim()) || !SOLO_LETRAS.test(a.apellido.trim())) {
        return { valido: false, error: "Los datos de los invitados solo admiten letras." };
      }
      if (a.documento?.trim() && !/^\d{7,8}$/.test(a.documento.trim())) {
        return { valido: false, error: "El DNI del invitado va sin puntos, con 7 u 8 números." };
      }
    } else {
      const pid = Number(a.personaId);
      if (isNaN(pid) || pid <= 0) {
        return {
          valido: false,
          error: "El ID o número de socio debe ser un número positivo.",
        };
      }
      if (sociosVistos.has(pid)) {
        if (titularId != null && pid === Number(titularId)) {
          return {
            valido: false,
            error: "El socio titular no puede incluirse como acompañante.",
          };
        }
        return {
          valido: false,
          error: "Un socio no puede figurar más de una vez en el partido.",
        };
      }
      sociosVistos.add(pid);
    }
  }

  return { valido: true };
}

/**
 * Calcula la duración en horas entre hora de inicio y fin (admite formato "HH:mm" y "HH:mm:ss").
 */
export function calcularHorasDuracion(horaInicio, horaFin) {
  if (!horaInicio || !horaFin) return 0;
  const [h1, m1 = 0] = horaInicio.split(":").map(Number);
  const [h2, m2 = 0] = horaFin.split(":").map(Number);
  if (isNaN(h1) || isNaN(h2)) return 0;
  const diffMinutos = h2 * 60 + m2 - (h1 * 60 + m1);
  return diffMinutos > 0 ? diffMinutos / 60 : 0;
}

/**
 * Formatea un importe numérico en formato de moneda local argentina (ARS).
 */
export function formatearMoneda(monto) {
  const numero = typeof monto === "number" ? monto : Number(monto);
  if (isNaN(numero)) return "$ 0,00";
  return new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
  }).format(numero);
}

/**
 * Formatea la franja horaria de un turno como "HH:mm - HH:mm".
 */
export function formatearHorarioTurno(horaInicio, horaFin) {
  if (!horaInicio || !horaFin) return "—";
  const inicio = horaInicio.slice(0, 5);
  const fin = horaFin.slice(0, 5);
  return `${inicio} - ${fin}`;
}

/**
 * Extrae un mensaje amigable y descriptivo de un error de Axios o del backend.
 * Prioriza el formato canónico de ErrorResponse de la API (`mensaje` y `campos` en español).
 */
export function extraerMensajeError(err, fallback = "Ocurrió un error inesperado.") {
  const data = err?.response?.data;
  if (!data) {
    return err?.message || fallback;
  }
  if (data.campos && typeof data.campos === "object" && Object.keys(data.campos).length > 0) {
    return Object.values(data.campos).join(". ");
  }
  if (data.mensaje) {
    return data.mensaje;
  }
  if (data.message) {
    return data.message;
  }
  return err?.message || fallback;
}

const aFechaLocal = (iso) => {
  const [anio, mes, dia] = String(iso).split("T")[0].split("-").map(Number);
  return new Date(anio, mes - 1, dia);
};

/** "Jueves 12 de noviembre". */
export function fechaLarga(iso) {
  const texto = aFechaLocal(iso).toLocaleDateString("es-AR", { weekday: "long", day: "numeric", month: "long" });
  return (texto.charAt(0).toUpperCase() + texto.slice(1)).replace(",", "");
}

/** Día y mes por separado, para mostrarlos como una hoja de calendario. */
export function diaYMes(iso) {
  const fecha = aFechaLocal(iso);
  return {
    dia: fecha.getDate(),
    mes: fecha.toLocaleDateString("es-AR", { month: "short" }).replace(".", ""),
  };
}

/** "Hoy", "Mañana" o "En 8 días". */
export function faltaParaLaFecha(iso, hoy = new Date()) {
  const inicioDeHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const dias = Math.round((aFechaLocal(iso) - inicioDeHoy) / 86400000);
  if (dias <= 0) return "Hoy";
  return dias === 1 ? "Mañana" : `En ${dias} días`;
}

const esPositivo = (monto) => Number(monto) > 0;

/** Qué pasó con la plata al cancelar: sale de los montos guardados, así una reserva nunca pagada no figura como reembolso. */
export function resultadoDeCancelacion(reserva) {
  if (esPositivo(reserva.montoReembolso)) return `Reembolso de ${formatearMoneda(reserva.montoReembolso)}`;
  if (esPositivo(reserva.montoPenalidad)) return "Seña perdida";
  return "Sin cargo";
}

export function resumirCancelaciones(canceladas) {
  const conReembolso = canceladas.filter((r) => esPositivo(r.montoReembolso)).length;
  const conSeniaPerdida = canceladas.filter((r) => esPositivo(r.montoPenalidad)).length;
  return { sinCargo: canceladas.length - conReembolso - conSeniaPerdida, conReembolso, conSeniaPerdida };
}
