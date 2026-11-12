export const ETIQUETA_SUPERFICIE = {
  LADRILLO: "Ladrillo",
  CEMENTO: "Cemento",
  PASTO: "Pasto",
  SINTETICO: "Césped sintético",
  ALFOMBRA: "Moqueta / Alfombra",
};

export function obtenerClaseSuperficie(superficie = "") {
  const s = String(superficie || "").toLowerCase();
  if (s.includes("ladrillo") || s.includes("arcilla") || s.includes("tru")) {
    return "bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]";
  }
  if (s.includes("cemento") || s.includes("hormig") || s.includes("concreto")) {
    return "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]";
  }
  if (s.includes("pasto") || s.includes("hierba") || s.includes("césped") || s.includes("cesped") || s.includes("grass")) {
    return "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]";
  }
  if (s.includes("acríl") || s.includes("acril") || s.includes("plexicushion")) {
    return "bg-blue-100 text-blue-800";
  }
  if (s.includes("moqueta") || s.includes("alfombra")) {
    return "bg-indigo-100 text-indigo-800";
  }
  if (s.includes("madera") || s.includes("parqué") || s.includes("parquet")) {
    return "bg-amber-100 text-amber-800";
  }
  return "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]";
}

export const CLASES_SUPERFICIE = new Proxy(
  {
    LADRILLO: "bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]",
    CEMENTO: "bg-[var(--estado-neutro-bg)] text-[var(--estado-neutro-fg)]",
    PASTO: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
    SINTETICO: "bg-emerald-100 text-emerald-800",
    ALFOMBRA: "bg-indigo-100 text-indigo-800",
  },
  {
    get(target, prop) {
      if (typeof prop === "string" && target[prop]) {
        return target[prop];
      }
      return obtenerClaseSuperficie(prop);
    },
  }
);

export const SUPERFICIES_SUGERIDAS = [
  "Polvo de ladrillo (arcilla roja)",
  "Har-Tru (arcilla verde)",
  "Cemento / Hormigón",
  "Resina acrílica (Plexicushion / DecoTurf)",
  "Hierba natural",
  "Césped artificial (con arena)",
  "Alfombra textil sintética",
  "Madera (parqué)",
];

export const OPCIONES_SUPERFICIE = [
  { value: "LADRILLO", label: "Polvo de ladrillo" },
  { value: "CEMENTO", label: "Cemento" },
  { value: "PASTO", label: "Césped natural" },
  { value: "SINTETICO", label: "Césped sintético" },
  { value: "ALFOMBRA", label: "Moqueta / Alfombra" },
];

export const CLASES_ESTADO = {
  Mantenimiento: "bg-[var(--estado-aviso-bg)] text-[var(--estado-aviso-fg)]",
  Disponible: "bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]",
  Deshabilitada: "bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]",
};

export function determinarEstado(cancha) {
  if (cancha.enMantenimiento) return "Mantenimiento";
  if (cancha.activa) return "Disponible";
  return "Deshabilitada";
}

export function etiquetaSuperficie(tipoSuperficie) {
  return ETIQUETA_SUPERFICIE[tipoSuperficie] || tipoSuperficie;
}
