// Fechas AAAA-MM-DD en hora local: toISOString() usa UTC y después de las 21 h en Argentina ya devuelve mañana.

const dosDigitos = (n) => String(n).padStart(2, "0");

export function fechaLocal(fecha = new Date()) {
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;
}

export function hoyLocal() {
  return fechaLocal(new Date());
}

export function inicioDelMes(fecha = new Date()) {
  return fechaLocal(new Date(fecha.getFullYear(), fecha.getMonth(), 1));
}

export function finDelMes(fecha = new Date()) {
  return fechaLocal(new Date(fecha.getFullYear(), fecha.getMonth() + 1, 0));
}
