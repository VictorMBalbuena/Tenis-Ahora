import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { eliminarCategoriaInsumo } from "../../api/insumosApi";
import { EliminarCategoriaDialog } from "../EliminarCategoriaDialog";

vi.mock("../../api/insumosApi", () => ({
  eliminarCategoriaInsumo: vi.fn(),
}));

describe("EliminarCategoriaDialog", () => {
  const categoria = { id: 3, nombre: "Grip" };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("no renderiza nada sin categoría", () => {
    const { container } = render(<EliminarCategoriaDialog abierto categoria={null} onCerrar={vi.fn()} onEliminada={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("confirma y avisa con el mensaje de éxito", async () => {
    eliminarCategoriaInsumo.mockResolvedValueOnce({});
    const onEliminada = vi.fn();

    render(<EliminarCategoriaDialog abierto categoria={categoria} onCerrar={vi.fn()} onEliminada={onEliminada} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar" }));

    expect(eliminarCategoriaInsumo).toHaveBeenCalledWith(3);
    expect(onEliminada).toHaveBeenCalledWith("Se eliminó la categoría Grip.");
  });

  it("si el backend la rechaza por tener insumos asignados, muestra el motivo", async () => {
    eliminarCategoriaInsumo.mockRejectedValueOnce({
      response: { data: { mensaje: "Hay 2 insumo(s) con esta categoría. Reasignalos antes de eliminarla" } },
    });
    const onEliminada = vi.fn();

    render(<EliminarCategoriaDialog abierto categoria={categoria} onCerrar={vi.fn()} onEliminada={onEliminada} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Hay 2 insumo(s) con esta categoría");
    expect(onEliminada).not.toHaveBeenCalled();
  });
});
