import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { FinalizarAlquilerDialog } from "../FinalizarAlquilerDialog";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi", () => ({
  finalizarAlquiler: vi.fn(),
}));

const mockReservaEnCurso = {
  id: 1,
  numero: "R-000001",
  canchaNombre: "Cancha 1",
  fecha: "2026-11-05",
  horaInicio: "15:00",
  horaFin: "16:00",
  estado: "EN_CURSO",
  montoTotal: 8000,
  montoSenia: 3000,
  insumos: [
    { id: 10, nombre: "Redes", cantidad: 3 },
  ],
};

describe("FinalizarAlquilerDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el diálogo con datos del turno, insumos a reintegrar y saldo pendiente", () => {
    render(
      <FinalizarAlquilerDialog
        open={true}
        reserva={mockReservaEnCurso}
        onOpenChange={vi.fn()}
        onFinalizado={vi.fn()}
      />
    );

    expect(screen.getByText(/Finalizar alquiler de cancha/i)).toBeInTheDocument();
    expect(screen.getByText(/R-000001/i)).toBeInTheDocument();
    expect(screen.getByText(/Redes/i)).toBeInTheDocument();
    expect(screen.getByText(/3 unidades/i)).toBeInTheDocument();
    expect(screen.getByText(/Saldo restante a abonar/i)).toBeInTheDocument();
    expect(screen.getByText(/\$ 5\.000/i)).toBeInTheDocument();
  });

  it("llama a finalizarAlquiler y ejecuta callbacks al confirmar", async () => {
    const user = userEvent.setup();
    const handleOpenChange = vi.fn();
    const handleFinalizado = vi.fn();
    const reservaFinalizada = { ...mockReservaEnCurso, estado: "FINALIZADA" };

    reservasApi.finalizarAlquiler.mockResolvedValueOnce(reservaFinalizada);

    render(
      <FinalizarAlquilerDialog
        open={true}
        reserva={mockReservaEnCurso}
        onOpenChange={handleOpenChange}
        onFinalizado={handleFinalizado}
      />
    );

    const botonConfirmar = screen.getByRole("button", { name: /Confirmar finalización/i });
    await user.click(botonConfirmar);

    await waitFor(() => {
      expect(reservasApi.finalizarAlquiler).toHaveBeenCalledWith(1);
      expect(handleFinalizado).toHaveBeenCalledWith(reservaFinalizada);
      expect(handleOpenChange).toHaveBeenCalledWith(false);
    });
  });

  it("muestra mensaje de error si la API falla", async () => {
    const user = userEvent.setup();
    reservasApi.finalizarAlquiler.mockRejectedValueOnce({
      response: { data: { message: "La reserva no está en curso" } },
    });

    render(
      <FinalizarAlquilerDialog
        open={true}
        reserva={mockReservaEnCurso}
        onOpenChange={vi.fn()}
        onFinalizado={vi.fn()}
      />
    );

    const botonConfirmar = screen.getByRole("button", { name: /Confirmar finalización/i });
    await user.click(botonConfirmar);

    await waitFor(() => {
      expect(screen.getByText("La reserva no está en curso")).toBeInTheDocument();
    });
  });
});

