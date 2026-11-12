import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { BloqueoMantenimientoDialog } from "../BloqueoMantenimientoDialog";

describe("BloqueoMantenimientoDialog", () => {
  const canchas = [{ id: 1, identificador: "Cancha 1", superficie: "PASTO" }];
  const bloqueoData = {
    canchaId: 1,
    motivo: "Mantenimiento general",
    fechaDesde: "2026-10-23",
    fechaHasta: "2026-10-23",
    horaInicio: "08:00",
    horaFin: "23:00",
    observaciones: "",
  };

  it("no renderiza nada si abierto es false", () => {
    const { container } = render(
      <BloqueoMantenimientoDialog abierto={false} canchas={canchas} bloqueoData={bloqueoData} reservasConflicto={[]} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("muestra el formulario cuando está abierto", () => {
    render(
      <BloqueoMantenimientoDialog
        abierto
        canchas={canchas}
        bloqueoData={bloqueoData}
        reservasConflicto={[]}
        onChange={vi.fn()}
        onCerrar={vi.fn()}
        onAplicar={vi.fn()}
      />
    );
    expect(screen.getByText("Bloquear cancha por mantenimiento")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Aplicar bloqueo/ })).toBeInTheDocument();
  });

  it("al confirmar correctamente llama a onAplicar", async () => {
    const onAplicar = vi.fn();
    render(
      <BloqueoMantenimientoDialog
        abierto
        canchas={canchas}
        bloqueoData={bloqueoData}
        reservasConflicto={[]}
        onChange={vi.fn()}
        onCerrar={vi.fn()}
        onAplicar={onAplicar}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /Aplicar bloqueo/ }));
    expect(onAplicar).toHaveBeenCalled();
  });

  it("con estadoValidacion error, muestra las reservas en conflicto y oculta Aplicar", () => {
    render(
      <BloqueoMantenimientoDialog
        abierto
        canchas={canchas}
        bloqueoData={bloqueoData}
        estadoValidacion="error"
        reservasConflicto={[{ id: "R-1" }]}
        onChange={vi.fn()}
        onCerrar={vi.fn()}
        onAplicar={vi.fn()}
      />
    );

    expect(screen.getByText("No se puede aplicar el bloqueo por superposición")).toBeInTheDocument();
    expect(screen.getByText("R-1")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Aplicar bloqueo/ })).not.toBeInTheDocument();
  });

  it("cancelar llama a onCerrar", async () => {
    const onCerrar = vi.fn();
    render(
      <BloqueoMantenimientoDialog
        abierto
        canchas={canchas}
        bloqueoData={bloqueoData}
        reservasConflicto={[]}
        onChange={vi.fn()}
        onCerrar={onCerrar}
        onAplicar={vi.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(onCerrar).toHaveBeenCalled();
  });
});
