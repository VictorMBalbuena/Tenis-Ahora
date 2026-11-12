import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { InsumosTabla } from "../InsumosTabla";

describe("InsumosTabla", () => {
  const insumosMock = [
    {
      id: 1,
      nombre: "Raquetas",
      categoria: { id: 1, nombre: "Raqueta" },
      cantidadDisponible: 24,
      cantidadMinima: 6,
      enUso: 8,
      estado: "DISPONIBLE",
    },
  ];

  it("renderiza el nombre, la categoría y el en-uso del insumo", () => {
    render(<InsumosTabla insumos={insumosMock} />);
    expect(screen.getAllByText("Raquetas")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Raqueta")[0]).toBeInTheDocument();
    expect(screen.getAllByText("8")[0]).toBeInTheDocument();
  });

  it("las acciones de fila llaman a sus callbacks con el insumo", async () => {
    const onVer = vi.fn();
    const onEditar = vi.fn();
    const onMovimiento = vi.fn();
    const onEliminar = vi.fn();

    render(
      <InsumosTabla
        insumos={insumosMock}
        onVer={onVer}
        onEditar={onEditar}
        onMovimiento={onMovimiento}
        onEliminar={onEliminar}
      />
    );

    await userEvent.click(screen.getAllByRole("button", { name: "Ver el detalle de Raquetas" })[0]);
    expect(onVer).toHaveBeenCalledWith(insumosMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Modificar Raquetas" })[0]);
    expect(onEditar).toHaveBeenCalledWith(insumosMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Reponer o descontar stock de Raquetas" })[0]);
    expect(onMovimiento).toHaveBeenCalledWith(insumosMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Eliminar Raquetas" })[0]);
    expect(onEliminar).toHaveBeenCalledWith(insumosMock[0]);
  });

  it("no muestra la paginación con una sola página", () => {
    render(<InsumosTabla insumos={insumosMock} totalPaginas={1} />);
    expect(screen.queryByRole("navigation", { name: "Paginación de insumos" })).not.toBeInTheDocument();
  });

  it("la paginación llama a onCambiarPagina", async () => {
    const onCambiarPagina = vi.fn();
    render(
      <InsumosTabla insumos={insumosMock} pagina={0} totalPaginas={3} totalElementos={50} onCambiarPagina={onCambiarPagina} />
    );

    await userEvent.click(screen.getByRole("button", { name: "Ir a la página siguiente" }));
    expect(onCambiarPagina).toHaveBeenCalledWith(1);
  });
});
