// Estado de validación de la certificación de un instructor (profesor o entrenador), compartido entre ambas features.

export const EstadoValidacion = {
  PENDIENTE: "PENDIENTE",
  VALIDADO: "VALIDADO",
  RECHAZADA: "RECHAZADA",
};

export const ETIQUETA_VALIDACION = {
  [EstadoValidacion.PENDIENTE]: "Pendiente de validar",
  [EstadoValidacion.VALIDADO]: "Validada",
  [EstadoValidacion.RECHAZADA]: "Rechazada",
};
