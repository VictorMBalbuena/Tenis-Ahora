import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { eliminarProfesor } from "../../api/profesoresApi";
import { EstadoValidacion } from "../../model/profesor";
import { EliminarProfesorDialog } from "../EliminarProfesorDialog";

vi.mock("../../api/profesoresApi", () => ({
  eliminarProfesor: vi.fn(),
}));

describe("EliminarProfesorDialog", () => {
  const profesor = {
    id: 1,
    nombre: "Bruno",
    apellido: "Tealdi",
    dni: "44966422",
    email: "bruno@correo.com",
    estadoValidacion: EstadoValidacion.VALIDADO,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("no renderiza nada sin profesor", () => {
    const { container } = render(<EliminarProfesorDialog abierto profesor={null} onCerrar={vi.fn()} onEliminado={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("confirma, llama al borrado y avisa con el nombre completo", async () => {
    eliminarProfesor.mockResolvedValueOnce({});
    const onEliminado = vi.fn();

    render(<EliminarProfesorDialog abierto profesor={profesor} onCerrar={vi.fn()} onEliminado={onEliminado} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar profesor" }));

    expect(eliminarProfesor).toHaveBeenCalledWith(1);
    expect(onEliminado).toHaveBeenCalledWith("Se eliminó a Bruno Tealdi.");
  });

  it("si falla, muestra el error y no avisa como eliminado", async () => {
    eliminarProfesor.mockRejectedValueOnce(new Error("500"));
    const onEliminado = vi.fn();

    render(<EliminarProfesorDialog abierto profesor={profesor} onCerrar={vi.fn()} onEliminado={onEliminado} />);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar profesor" }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(onEliminado).not.toHaveBeenCalled();
  });
});
