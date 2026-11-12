import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { IniciarAlquilerDialog } from "../IniciarAlquilerDialog";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi", () => ({
  iniciarAlquiler: vi.fn(),
}));

const mockReservaConInsumos = {
  id: 1,
  numero: "R-000001",
  canchaNombre: "Cancha 1",
  fecha: "2026-11-05",
  horaInicio: "15:00",
  horaFin: "16:00",
  estado: "CONFIRMADA",
  insumos: [
    { id: 10, nombre: "Redes", cantidad: 3 },
  ],
};

const mockReservaSinInsumos = {
  id: 2,
  numero: "R-000002",
  canchaNombre: "Cancha 2",
  fecha: "2026-11-05",
  horaInicio: "16:00",
  horaFin: "17:00",
  estado: "CONFIRMADA",
  insumos: [],
};

describe("IniciarAlquilerDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el diálogo con datos del turno y lista de insumos a entregar", () => {
    render(
      <IniciarAlquilerDialog
        open={true}
        reserva={mockReservaConInsumos}
        onOpenChange={vi.fn()}
        onIniciado={vi.fn()}
      />
    );

    expect(screen.getByText(/Iniciar alquiler de cancha/i)).toBeInTheDocument();
    expect(screen.getByText(/R-000001/i)).toBeInTheDocument();
    expect(screen.getByText(/Redes/i)).toBeInTheDocument();
    expect(screen.getByText(/3 unidades/i)).toBeInTheDocument();
  });

  it("muestra mensaje adecuado cuando la reserva no tiene insumos adicionales", () => {
    render(
      <IniciarAlquilerDialog
        open={true}
        reserva={mockReservaSinInsumos}
        onOpenChange={vi.fn()}
        onIniciado={vi.fn()}
      />
    );

    expect(screen.getByText(/Esta reserva no solicitó insumos adicionales/i)).toBeInTheDocument();
  });

  it("llama a iniciarAlquiler y ejecuta callbacks al confirmar", async () => {
    const user = userEvent.setup();
    const handleOpenChange = vi.fn();
    const handleIniciado = vi.fn();
    const reservaActualizada = { ...mockReservaConInsumos, estado: "EN_CURSO" };

    reservasApi.iniciarAlquiler.mockResolvedValueOnce(reservaActualizada);

    render(
      <IniciarAlquilerDialog
        open={true}
        reserva={mockReservaConInsumos}
        onOpenChange={handleOpenChange}
        onIniciado={handleIniciado}
      />
    );

    const botonConfirmar = screen.getByRole("button", { name: /Confirmar inicio/i });
    await user.click(botonConfirmar);

    await waitFor(() => {
      expect(reservasApi.iniciarAlquiler).toHaveBeenCalledWith(1);
      expect(handleIniciado).toHaveBeenCalledWith(reservaActualizada);
      expect(handleOpenChange).toHaveBeenCalledWith(false);
    });
  });

  it("muestra mensaje de error si la API falla", async () => {
    const user = userEvent.setup();
    reservasApi.iniciarAlquiler.mockRejectedValueOnce({
      response: { data: { message: "No hay stock suficiente" } },
    });

    render(
      <IniciarAlquilerDialog
        open={true}
        reserva={mockReservaConInsumos}
        onOpenChange={vi.fn()}
        onIniciado={vi.fn()}
      />
    );

    const botonConfirmar = screen.getByRole("button", { name: /Confirmar inicio/i });
    await user.click(botonConfirmar);

    await waitFor(() => {
      expect(screen.getByText("No hay stock suficiente")).toBeInTheDocument();
    });
  });
});

