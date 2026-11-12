import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EntrenamientosTabla } from "../EntrenamientosTabla";

const entrenamientoPrueba = {
  id: 10,
  nombre: "Entrenamiento Funcional",
  fechaInicio: "2026-11-01",
  fechaFin: "2026-11-30",
  dias: ["MONDAY"],
  horaInicio: "08:00:00",
  horaFin: "09:30:00",
  cupoMaximo: 10,
  cupoOcupado: 4,
  inscriptos: 4,
  estado: "PROGRAMADA",
  modalidad: "GRUPAL",
  cancha: "Cancha 2",
  entrenador: "Paz, Luis",
};

describe("EntrenamientosTabla - Mobile", () => {
  it("al presionar Enter en Tomar asistencia ejecuta onAsistencia y no onVer", async () => {
    const onVer = vi.fn();
    const onAsistencia = vi.fn();

    render(
      <EntrenamientosTabla
        entrenamientos={[entrenamientoPrueba]}
        onVer={onVer}
        onAsistencia={onAsistencia}
      />,
    );

    const botonAsistencia = screen.getByRole("button", { name: /Registrar asistencia de/i });
    botonAsistencia.focus();
    await userEvent.keyboard("{Enter}");

    expect(onAsistencia).toHaveBeenCalledTimes(1);
    expect(onVer).not.toHaveBeenCalled();
  });

  it("al presionar Enter sobre la tarjeta ejecuta onVer y no onAsistencia", async () => {
    const onVer = vi.fn();
    const onAsistencia = vi.fn();

    render(
      <EntrenamientosTabla
        entrenamientos={[entrenamientoPrueba]}
        onVer={onVer}
        onAsistencia={onAsistencia}
      />,
    );

    const tarjeta = screen.getByRole("button", { name: /Tarjeta del entrenamiento/i });
    tarjeta.focus();
    await userEvent.keyboard("{Enter}");

    expect(onVer).toHaveBeenCalledTimes(1);
    expect(onAsistencia).not.toHaveBeenCalled();
  });

  it("muestra el numerador con cupoOcupado o inscriptos correctamente", () => {
    const e = { ...entrenamientoPrueba, cupoOcupado: undefined, inscriptos: 7, cupoMaximo: 12 };
    render(<EntrenamientosTabla entrenamientos={[e]} onVer={() => {}} onAsistencia={() => {}} />);
    expect(screen.getAllByText("7").length).toBeGreaterThan(0);
  });

  it("ofrece boton de imprimir en la tarjeta movil y ejecuta onImprimir al cliquear", async () => {
    const onImprimir = vi.fn();
    render(
      <EntrenamientosTabla
        entrenamientos={[entrenamientoPrueba]}
        onVer={() => {}}
        onAsistencia={() => {}}
        onImprimir={onImprimir}
      />,
    );

    const botonImprimir = screen.getAllByRole("button", { name: /Imprimir Entrenamiento Funcional/i })[0];
    await userEvent.click(botonImprimir);
    expect(onImprimir).toHaveBeenCalledWith(entrenamientoPrueba);
  });
});
