import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EstadoValidacion } from "../../model/profesor";
import { ProfesoresTabla } from "../ProfesoresTabla";

describe("ProfesoresTabla", () => {
  const profesoresMock = [
    {
      id: 1,
      nombre: "Bruno",
      apellido: "Tealdi",
      dni: "44966422",
      email: "bruno@correo.com",
      activo: true,
      estadoValidacion: EstadoValidacion.VALIDADO,
      tituloHabilitante: "Profesor Nacional de Tenis",
    },
  ];

  it("renderiza el profesor con su título y estado", () => {
    render(<ProfesoresTabla profesores={profesoresMock} />);
    expect(screen.getAllByText("Bruno Tealdi")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Profesor Nacional de Tenis")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Validada")[0]).toBeInTheDocument();
  });

  it("las acciones de fila llaman a sus callbacks con el profesor", async () => {
    const onVer = vi.fn();
    const onEditar = vi.fn();
    const onImprimir = vi.fn();
    const onEliminar = vi.fn();

    render(
      <ProfesoresTabla
        profesores={profesoresMock}
        onVer={onVer}
        onEditar={onEditar}
        onImprimir={onImprimir}
        onEliminar={onEliminar}
      />
    );

    await userEvent.click(screen.getAllByRole("button", { name: "Ver el detalle de Bruno Tealdi" })[0]);
    expect(onVer).toHaveBeenCalledWith(profesoresMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Modificar a Bruno Tealdi" })[0]);
    expect(onEditar).toHaveBeenCalledWith(profesoresMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Imprimir la ficha de Bruno Tealdi" })[0]);
    expect(onImprimir).toHaveBeenCalledWith(profesoresMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Eliminar a Bruno Tealdi" })[0]);
    expect(onEliminar).toHaveBeenCalledWith(profesoresMock[0]);
  });

  it("la paginación llama a onCambiarPagina", async () => {
    const onCambiarPagina = vi.fn();
    render(
      <ProfesoresTabla profesores={profesoresMock} pagina={0} totalPaginas={3} totalElementos={50} onCambiarPagina={onCambiarPagina} />
    );

    await userEvent.click(screen.getByRole("button", { name: "Ir a la página siguiente" }));
    expect(onCambiarPagina).toHaveBeenCalledWith(1);
  });
});
