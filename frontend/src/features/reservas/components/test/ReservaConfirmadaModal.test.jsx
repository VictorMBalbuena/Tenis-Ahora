import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ReservaConfirmadaModal } from "../ReservaConfirmadaModal";

describe("ReservaConfirmadaModal", () => {
  const mockReserva = {
    id: 101,
    numero: "R-000101",
    estado: "PENDIENTE_PAGO",
    montoSenia: 9500,
    total: 19000,
    canchaNombre: "Cancha 1",
    fecha: "2026-10-25",
    horaInicio: "18:00",
  };

  it("renderiza el número de reserva, badge de pendiente de pago y monto de la seña", () => {
    render(
      <ReservaConfirmadaModal
        reserva={mockReserva}
        abierta={true}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(screen.getByText("R-000101")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /turno retenido/i })).toBeInTheDocument();
    expect(screen.getByText(/9\.500/)).toBeInTheDocument();
  });

  it("funciona con la prop open como alias de abierta", () => {
    render(
      <ReservaConfirmadaModal
        reserva={mockReserva}
        open={true}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(screen.getByText("R-000101")).toBeInTheDocument();
  });

  it("muestra la seña que informa el servidor, sin calcularla a partir del total", () => {
    const reservaConSenia = {
      id: 202,
      total: 24000,
      montoSenia: 7000,
      estado: "PENDIENTE_PAGO",
    };
    render(
      <ReservaConfirmadaModal
        reserva={reservaConSenia}
        abierta={true}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(screen.getByText(/7\.000/)).toBeInTheDocument();
    expect(screen.queryByText(/12\.000/)).not.toBeInTheDocument();
    expect(screen.getByText("R-000202")).toBeInTheDocument();
  });

  it("acepta montoTotal como total de la reserva", () => {
    const reservaConMontoTotal = {
      id: 203,
      montoTotal: 30000,
      montoSenia: 15000,
      estado: "PENDIENTE_PAGO",
    };
    render(
      <ReservaConfirmadaModal
        reserva={reservaConMontoTotal}
        abierta={true}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(screen.getByText(/30\.000/)).toBeInTheDocument();
    expect(screen.getByText("R-000203")).toBeInTheDocument();
  });

  it("llama a onCerrar al hacer click en Mis Reservas", () => {
    const onCerrar = vi.fn();
    render(
      <ReservaConfirmadaModal
        reserva={mockReserva}
        abierta={true}
        onCerrar={onCerrar}
        onPagar={vi.fn()}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /mis reservas/i }));
    expect(onCerrar).toHaveBeenCalledTimes(1);
  });

  it("llama a onPagar al hacer click en Pagar seña", () => {
    const onPagar = vi.fn();
    render(
      <ReservaConfirmadaModal
        reserva={mockReserva}
        abierta={true}
        onCerrar={vi.fn()}
        onPagar={onPagar}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /pagar seña/i }));
    expect(onPagar).toHaveBeenCalledTimes(1);
  });

  it("no muestra el contenido cuando abierta es false", () => {
    render(
      <ReservaConfirmadaModal
        reserva={mockReserva}
        abierta={false}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(screen.queryByText("R-000101")).not.toBeInTheDocument();
  });

  it("retorna null de forma segura si reserva es null o undefined", () => {
    const { container } = render(
      <ReservaConfirmadaModal
        reserva={null}
        abierta={true}
        onCerrar={vi.fn()}
        onPagar={vi.fn()}
      />
    );

    expect(container.firstChild).toBeNull();
  });
});
