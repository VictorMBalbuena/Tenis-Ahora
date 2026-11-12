import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { CancelarReservaDialog } from "../CancelarReservaDialog";
import * as api from "../../api/reservasApi";

describe("CancelarReservaDialog", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("muestra aviso de penalidad si aTiempo es false antes de confirmar", async () => {
    vi.spyOn(api, "previsualizarCancelacion").mockResolvedValueOnce({
      aTiempo: false,
      horasDeAntelacion: 3,
      montoPenalidad: 8000,
      montoReembolso: 0,
      mensaje: "Cancelás fuera de término: se retiene el 100% de la seña abonada",
    });

    render(
      <CancelarReservaDialog
        reserva={{ id: 101, numero: "R-000101" }}
        abierta={true}
        onCerrar={vi.fn()}
        onCanceladaExitosamente={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/se retiene el 100% de la seña/i)).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: /confirmar cancelación/i })).toBeInTheDocument();
  });

  it("muestra aviso favorable cuando la cancelación es a tiempo", async () => {
    vi.spyOn(api, "previsualizarCancelacion").mockResolvedValueOnce({
      aTiempo: true,
      horasDeAntelacion: 24,
      montoPenalidad: 0,
      montoReembolso: 8000,
      mensaje: "Cancelás a tiempo: corresponde el 100% de reembolso",
    });

    render(
      <CancelarReservaDialog
        reserva={{ id: 102, numero: "R-000102" }}
        abierta={true}
        onCerrar={vi.fn()}
        onCanceladaExitosamente={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/corresponde el 100% de reembolso/i)).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: /confirmar cancelación/i })).toBeInTheDocument();
  });

  it("llama a confirmarCancelacion y ejecuta onCanceladaExitosamente y onCerrar al confirmar", async () => {
    vi.spyOn(api, "previsualizarCancelacion").mockResolvedValueOnce({
      aTiempo: true,
      horasDeAntelacion: 20,
      montoPenalidad: 0,
      montoReembolso: 5000,
      mensaje: "Cancelación sin penalidad",
    });
    const canceladaMock = { id: 103, estado: "CANCELADA" };
    vi.spyOn(api, "confirmarCancelacion").mockResolvedValueOnce(canceladaMock);

    const onCerrar = vi.fn();
    const onCanceladaExitosamente = vi.fn();

    render(
      <CancelarReservaDialog
        reserva={{ id: 103, numero: "R-000103" }}
        abierta={true}
        onCerrar={onCerrar}
        onCanceladaExitosamente={onCanceladaExitosamente}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /confirmar cancelación/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /confirmar cancelación/i }));

    await waitFor(() => {
      expect(api.confirmarCancelacion).toHaveBeenCalledWith(103);
      expect(onCanceladaExitosamente).toHaveBeenCalledWith(canceladaMock);
      expect(onCerrar).toHaveBeenCalled();
    });
  });

  it("no muestra el diálogo si abierta es false", () => {
    render(
      <CancelarReservaDialog
        reserva={{ id: 104, numero: "R-000104" }}
        abierta={false}
        onCerrar={vi.fn()}
        onCanceladaExitosamente={vi.fn()}
      />
    );

    expect(screen.queryByText(/confirmar cancelación/i)).not.toBeInTheDocument();
  });
});
