import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { EliminarReservaDialog } from "../EliminarReservaDialog";

const mockReserva = {
  id: 1039,
  codigo: "RES-1039",
  canchaNombre: "Cancha 2",
  fecha: "2026-09-23",
  horaInicio: "14:00",
  horaFin: "15:00",
  montoSenia: 0,
  socioTitular: {
    nombre: "Bruno",
    apellido: "Tealdi",
  },
};

describe("EliminarReservaDialog", () => {
  it("renderiza el diálogo con el título, datos del turno y banner de aviso", () => {
    render(
      <EliminarReservaDialog
        open={true}
        reserva={mockReserva}
        onOpenChange={vi.fn()}
        onConfirm={vi.fn()}
      />
    );

    expect(screen.getByText(/¿Eliminar reserva\?/i)).toBeInTheDocument();
    expect(screen.getByText("RES-1039")).toBeInTheDocument();
    expect(screen.getByText("Bruno Tealdi")).toBeInTheDocument();
    expect(screen.getByText(/23\/09.*14:00/i)).toBeInTheDocument();
    expect(screen.getByText(/\$ 0/i)).toBeInTheDocument();
    expect(screen.getByText(/cancelación dentro del plazo|cancelación/i)).toBeInTheDocument();
  });

  it("al hacer clic en Cancelar, se invoca onOpenChange(false)", async () => {
    const user = userEvent.setup();
    const handleOpenChange = vi.fn();

    render(
      <EliminarReservaDialog
        open={true}
        reserva={mockReserva}
        onOpenChange={handleOpenChange}
        onConfirm={vi.fn()}
      />
    );

    const cancelarBtn = screen.getByRole("button", { name: /^cancelar$/i });
    await user.click(cancelarBtn);

    expect(handleOpenChange).toHaveBeenCalledWith(false);
  });

  it("al hacer clic en Eliminar, se invoca onConfirm con la reserva", async () => {
    const user = userEvent.setup();
    const handleConfirm = vi.fn().mockResolvedValue();

    render(
      <EliminarReservaDialog
        open={true}
        reserva={mockReserva}
        onOpenChange={vi.fn()}
        onConfirm={handleConfirm}
      />
    );

    const eliminarBtn = screen.getByRole("button", { name: /^eliminar$/i });
    await user.click(eliminarBtn);

    expect(handleConfirm).toHaveBeenCalledWith(mockReserva);
  });
});
