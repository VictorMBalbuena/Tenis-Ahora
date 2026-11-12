import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { GrillaDisponibilidadReserva } from "../GrillaDisponibilidadReserva";

describe("GrillaDisponibilidadReserva", () => {
  const mockCanchas = [
    {
      canchaId: 1,
      identificador: "Cancha Central",
      tipoSuperficie: "LADRILLO",
      precioPorHora: 9500,
      franjas: [
        { desde: "10:00:00", hasta: "11:00:00", estado: "LIBRE" },
        { desde: "11:00:00", hasta: "12:00:00", estado: "OCUPADA" },
        { desde: "12:00:00", hasta: "13:00:00", estado: "BLOQUEADA" },
      ],
    },
  ];

  it("muestra estado de carga", () => {
    render(<GrillaDisponibilidadReserva cargando={true} />);
    expect(screen.getByText(/cargando disponibilidad/i)).toBeInTheDocument();
  });

  it("muestra mensaje de error", () => {
    render(<GrillaDisponibilidadReserva error="Fallo de red al cargar canchas" />);
    expect(screen.getByRole("alert")).toHaveTextContent("Fallo de red al cargar canchas");
  });

  it("muestra mensaje si no hay canchas", () => {
    render(<GrillaDisponibilidadReserva canchas={[]} />);
    expect(screen.getByText(/no hay canchas disponibles/i)).toBeInTheDocument();
  });

  it("renderiza canchas, superficies y precio por hora", () => {
    render(<GrillaDisponibilidadReserva canchas={mockCanchas} />);
    expect(screen.getByText("Cancha Central")).toBeInTheDocument();
    expect(screen.getByText(/ladrillo/i)).toBeInTheDocument();
    expect(screen.getByText(/9\.500/)).toBeInTheDocument();
  });

  it("permite seleccionar un turno libre y activa aria-pressed", () => {
    const onSeleccionar = vi.fn();
    const { rerender } = render(
      <GrillaDisponibilidadReserva
        canchas={mockCanchas}
        seleccion={null}
        onSeleccionarTurno={onSeleccionar}
      />
    );

    const btn10 = screen.getByRole("button", { name: /10:00/i });
    expect(btn10).toHaveAttribute("aria-pressed", "false");
    fireEvent.click(btn10);

    expect(onSeleccionar).toHaveBeenCalledWith({
      canchaId: 1,
      canchaNombre: "Cancha Central",
      precioPorHora: 9500,
      horaInicio: "10:00",
    });

    rerender(
      <GrillaDisponibilidadReserva
        canchas={mockCanchas}
        seleccion={{ canchaId: 1, horaInicio: "10:00" }}
        onSeleccionarTurno={onSeleccionar}
      />
    );

    expect(screen.getByRole("button", { name: /10:00/i })).toHaveAttribute("aria-pressed", "true");
  });

  it("deshabilita franjas ocupadas y en mantenimiento", () => {
    render(<GrillaDisponibilidadReserva canchas={mockCanchas} />);
    const btn11 = screen.getByRole("button", { name: /11:00/i });
    expect(btn11).toBeDisabled();

    const btn12 = screen.getByRole("button", { name: /12:00/i });
    expect(btn12).toBeDisabled();
  });
});
