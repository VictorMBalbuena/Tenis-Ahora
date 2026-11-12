import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { EliminarCanchaDialog } from "../EliminarCanchaDialog";

vi.mock("@/config/axiosClient", () => ({
  default: { patch: vi.fn() },
}));

describe("EliminarCanchaDialog", () => {
  const cancha = { id: 1, identificador: "Cancha 1", tipoSuperficie: "PASTO" };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra el identificador y la superficie de la cancha a eliminar", () => {
    render(<EliminarCanchaDialog abierto cancha={cancha} onCerrar={vi.fn()} onEliminada={vi.fn()} />);
    expect(screen.getByText("Cancha 1", { exact: false })).toBeInTheDocument();
    expect(screen.getByText(/Pasto/)).toBeInTheDocument();
  });

  it("confirma, deshabilita la cancha y avisa con su id", async () => {
    axiosClient.patch.mockResolvedValueOnce({});
    const onEliminada = vi.fn();

    render(<EliminarCanchaDialog abierto cancha={cancha} onCerrar={vi.fn()} onEliminada={onEliminada} />);
    await userEvent.click(screen.getByRole("button", { name: "Deshabilitar" }));

    expect(axiosClient.patch).toHaveBeenCalledWith("/canchas/1", { activa: false });
    expect(onEliminada).toHaveBeenCalledWith(1);
  });

  it("muestra un error si no se puede deshabilitar", async () => {
    axiosClient.patch.mockRejectedValueOnce(new Error("409"));
    const onEliminada = vi.fn();

    render(<EliminarCanchaDialog abierto cancha={cancha} onCerrar={vi.fn()} onEliminada={onEliminada} />);
    await userEvent.click(screen.getByRole("button", { name: "Deshabilitar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("No se pudo deshabilitar la cancha");
    expect(onEliminada).not.toHaveBeenCalled();
  });

  it("cancelar cierra sin eliminar", async () => {
    const onCerrar = vi.fn();
    render(<EliminarCanchaDialog abierto cancha={cancha} onCerrar={onCerrar} onEliminada={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onCerrar).toHaveBeenCalled();
    expect(axiosClient.patch).not.toHaveBeenCalled();
  });
});
