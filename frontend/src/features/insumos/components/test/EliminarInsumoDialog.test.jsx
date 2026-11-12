import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { eliminarInsumo } from "../../api/insumosApi";
import { EliminarInsumoDialog } from "../EliminarInsumoDialog";

vi.mock("../../api/insumosApi", () => ({
  eliminarInsumo: vi.fn(),
}));

describe("EliminarInsumoDialog", () => {
  const insumo = {
    id: 1,
    nombre: "Raquetas",
    categoria: { id: 1, nombre: "Raqueta" },
    cantidadDisponible: 10,
    cantidadMinima: 2,
    enUso: 0,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("no renderiza nada sin insumo", () => {
    const { container } = render(<EliminarInsumoDialog abierto insumo={null} onCerrar={vi.fn()} onEliminado={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("muestra el resumen del insumo a eliminar", () => {
    render(<EliminarInsumoDialog abierto insumo={insumo} onCerrar={vi.fn()} onEliminado={vi.fn()} />);
    expect(screen.getByText("Raquetas")).toBeInTheDocument();
    expect(screen.getByText("Raqueta")).toBeInTheDocument();
  });

  it("si tiene unidades en uso, avisa sobre la trazabilidad", () => {
    render(<EliminarInsumoDialog abierto insumo={{ ...insumo, enUso: 3 }} onCerrar={vi.fn()} onEliminado={vi.fn()} />);
    expect(screen.getByText(/3 unidades asignadas a turnos en curso/)).toBeInTheDocument();
  });

  it("sin unidades en uso, no muestra el aviso", () => {
    render(<EliminarInsumoDialog abierto insumo={insumo} onCerrar={vi.fn()} onEliminado={vi.fn()} />);
    expect(screen.queryByText(/unidades asignadas a turnos en curso/)).not.toBeInTheDocument();
  });

  it("confirmar llama al borrado y avisa con el mensaje de éxito", async () => {
    eliminarInsumo.mockResolvedValueOnce({});
    const onEliminado = vi.fn();

    render(<EliminarInsumoDialog abierto insumo={insumo} onCerrar={vi.fn()} onEliminado={onEliminado} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar insumo" }));

    expect(eliminarInsumo).toHaveBeenCalledWith(1);
    expect(onEliminado).toHaveBeenCalledWith("Se elimino el insumo Raquetas.");
  });

  it("si falla, muestra el error y no avisa como eliminado", async () => {
    eliminarInsumo.mockRejectedValueOnce(new Error("500"));
    const onEliminado = vi.fn();

    render(<EliminarInsumoDialog abierto insumo={insumo} onCerrar={vi.fn()} onEliminado={onEliminado} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar insumo" }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(onEliminado).not.toHaveBeenCalled();
  });
});
