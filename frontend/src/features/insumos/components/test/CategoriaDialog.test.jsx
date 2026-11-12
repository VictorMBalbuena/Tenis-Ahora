import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { actualizarCategoriaInsumo, crearCategoriaInsumo } from "../../api/insumosApi";
import { CategoriaDialog } from "../CategoriaDialog";

vi.mock("../../api/insumosApi", () => ({
  crearCategoriaInsumo: vi.fn(),
  actualizarCategoriaInsumo: vi.fn(),
}));

describe("CategoriaDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sin categoría, es alta: título Nueva categoría y botón Crear", () => {
    render(<CategoriaDialog abierto categoria={null} onCerrar={vi.fn()} onGuardada={vi.fn()} />);
    expect(screen.getByText("Nueva categoría")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear" })).toBeInTheDocument();
  });

  it("con categoría, es modificación: precarga el nombre y dice Guardar cambios", () => {
    render(
      <CategoriaDialog abierto categoria={{ id: 1, nombre: "Raqueta" }} onCerrar={vi.fn()} onGuardada={vi.fn()} />
    );
    expect(screen.getByText("Modificar categoría")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Raqueta")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Guardar cambios" })).toBeInTheDocument();
  });

  it("al crear, llama a crearCategoriaInsumo y avisa con la categoría guardada", async () => {
    crearCategoriaInsumo.mockResolvedValueOnce({ id: 9, nombre: "Grip" });
    const onGuardada = vi.fn();

    render(<CategoriaDialog abierto categoria={null} onCerrar={vi.fn()} onGuardada={onGuardada} />);
    await userEvent.type(screen.getByLabelText("Nombre"), "Grip");
    await userEvent.click(screen.getByRole("button", { name: "Crear" }));

    expect(crearCategoriaInsumo).toHaveBeenCalledWith("Grip");
    expect(onGuardada).toHaveBeenCalledWith({ id: 9, nombre: "Grip" });
  });

  it("al modificar, llama a actualizarCategoriaInsumo con el id", async () => {
    actualizarCategoriaInsumo.mockResolvedValueOnce({ id: 1, nombre: "Raquetas" });
    const onGuardada = vi.fn();

    render(
      <CategoriaDialog abierto categoria={{ id: 1, nombre: "Raqueta" }} onCerrar={vi.fn()} onGuardada={onGuardada} />
    );
    await userEvent.type(screen.getByDisplayValue("Raqueta"), "s");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarCategoriaInsumo).toHaveBeenCalledWith(1, "Raquetas");
    expect(onGuardada).toHaveBeenCalledWith({ id: 1, nombre: "Raquetas" });
  });

  it("si falla el guardado, muestra el error y no avisa como guardada", async () => {
    crearCategoriaInsumo.mockRejectedValueOnce({ response: { data: { mensaje: "Ya existe" } } });
    const onGuardada = vi.fn();

    render(<CategoriaDialog abierto categoria={null} onCerrar={vi.fn()} onGuardada={onGuardada} />);
    await userEvent.type(screen.getByLabelText("Nombre"), "Raqueta");
    await userEvent.click(screen.getByRole("button", { name: "Crear" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Ya existe");
    expect(onGuardada).not.toHaveBeenCalled();
  });

  it("el botón Crear está deshabilitado con el nombre vacío", () => {
    render(<CategoriaDialog abierto categoria={null} onCerrar={vi.fn()} onGuardada={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Crear" })).toBeDisabled();
  });
});
