import { useState } from "react";

export const DRAFT_INICIAL = {
  fecha: "",
  duracionHoras: 1,
  canchaId: null,
  canchaNombre: "",
  precioPorHora: null,
  horaInicio: "",
  titularId: null,
  titularNombre: "",
  titularDni: "",
  modalidad: "SINGLE",
  acompaniantes: [],
  insumos: [],
};

// Gestiona el estado paso a paso del wizard de nueva reserva (FRONT 06)
export function useReservaWizard(initialDraft = {}, initialPaso = 1) {
  const [pasoActual, setPasoActual] = useState(initialPaso);
  const [draft, setDraft] = useState(() => ({
    ...DRAFT_INICIAL,
    ...initialDraft,
  }));
  const [reservaCreada, setReservaCreada] = useState(null);
  const [modalExitoOpen, setModalExitoOpen] = useState(false);

  const avanzarPaso = (datosParciales = {}) => {
    setDraft((prev) => ({
      ...prev,
      ...datosParciales,
    }));
    setPasoActual((prev) => Math.min(prev + 1, 4));
  };

  const retrocederPaso = () => {
    setPasoActual((prev) => Math.max(prev - 1, 1));
  };

  const irAPaso = (paso) => {
    if (typeof paso === "number" && paso >= 1 && paso <= 4) {
      setPasoActual(paso);
    }
  };

  const reiniciar = () => {
    setPasoActual(1);
    setDraft({ ...DRAFT_INICIAL });
    setReservaCreada(null);
    setModalExitoOpen(false);
  };

  const abrirExito = (reserva) => {
    setReservaCreada(reserva);
    setModalExitoOpen(true);
  };

  const cerrarExito = () => {
    setModalExitoOpen(false);
  };

  return {
    pasoActual,
    draft,
    reservaCreada,
    modalExitoOpen,
    avanzarPaso,
    retrocederPaso,
    irAPaso,
    reiniciar,
    abrirExito,
    cerrarExito,
    setDraft,
  };
}

export default useReservaWizard;
