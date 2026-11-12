import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { CanchasTabla } from "../CanchasTabla";

describe("CanchasTabla", () => {
  const canchasMock = [
    { id: 1, identificador: "Cancha 1", tipoSuperficie: "PASTO", capacidad: 4, precioPorHora: 7000, activa: true, enMantenimiento: false },
    { id: 2, identificador: "Cancha 2", tipoSuperficie: "CEMENTO", capacidad: 2, precioPorHora: null, activa: false, enMantenimiento: false },
  ];

  it("renderiza las canchas con su superficie, precio y estado", () => {
    render(<CanchasTabla canchas={canchasMock} />);

    expect(screen.getAllByText("Cancha 1")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Pasto")[0]).toBeInTheDocument();
    expect(screen.getAllByText("$ 7.000")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Disponible")[0]).toBeInTheDocument();

    expect(screen.getAllByText("Cancha 2")[0]).toBeInTheDocument();
    expect(screen.getAllByText("-")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Deshabilitada")[0]).toBeInTheDocument();
  });

  it("muestra el mensaje vacío cuando no hay canchas", () => {
    render(<CanchasTabla canchas={[]} />);
    expect(screen.getAllByText("No hay canchas registradas en el sistema.")[0]).toBeInTheDocument();
  });

  it("los botones de acción llaman a sus callbacks con la cancha", async () => {
    const onVer = vi.fn();
    const onEditar = vi.fn();
    const onEliminar = vi.fn();

    render(<CanchasTabla canchas={canchasMock} onVer={onVer} onEditar={onEditar} onEliminar={onEliminar} />);

    await userEvent.click(screen.getAllByRole("button", { name: "Ver detalle" })[0]);
    expect(onVer).toHaveBeenCalledWith(canchasMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Editar" })[0]);
    expect(onEditar).toHaveBeenCalledWith(canchasMock[0]);

    await userEvent.click(screen.getAllByRole("button", { name: "Eliminar" })[0]);
    expect(onEliminar).toHaveBeenCalledWith(canchasMock[0]);
  });

  it("la paginación llama a onCambiarPagina con la página siguiente", async () => {
    const onCambiarPagina = vi.fn();
    render(
      <CanchasTabla
        canchas={canchasMock}
        pagina={0}
        totalPaginas={3}
        totalElementos={50}
        onCambiarPagina={onCambiarPagina}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Pagina siguiente" }));
    expect(onCambiarPagina).toHaveBeenCalledWith(1);

    expect(screen.getByRole("button", { name: "Pagina anterior" })).toBeDisabled();
  });
});
