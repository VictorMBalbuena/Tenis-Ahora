import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { UsuariosTabla } from "./UsuariosTabla";

describe("UsuariosTabla", () => {
  const usuariosMock = [
    {
      id: 1,
      nombre: "Ana",
      apellido: "Gómez",
      dni: "30123456",
      email: "ana@correo.com",
      rol: "SOCIO",
      activo: true,
    },
    {
      id: 2,
      nombre: "Carlos",
      apellido: "López",
      dni: "32987654",
      email: "carlos@correo.com",
      rol: "EMPLEADO",
      activo: false,
    },
  ];

  it("renderiza correctamente los usuarios y sus datos", () => {
    render(<UsuariosTabla usuarios={usuariosMock} />);

    expect(screen.getAllByText("Ana Gómez")[0]).toBeInTheDocument();
    expect(screen.getAllByText("ana@correo.com")[0]).toBeInTheDocument();
    expect(screen.getAllByText("30.123.456")[0]).toBeInTheDocument();

    expect(screen.getAllByText("Carlos López")[0]).toBeInTheDocument();
    expect(screen.getAllByText("carlos@correo.com")[0]).toBeInTheDocument();
    expect(screen.getAllByText("32.987.654")[0]).toBeInTheDocument();

    expect(screen.getAllByText("Activo")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Inactivo")[0]).toBeInTheDocument();
  });

  it("los botones de acción tienen aria-label descriptivo y llaman a sus callbacks", async () => {
    const onVer = vi.fn();
    const onEditar = vi.fn();
    const onImprimir = vi.fn();
    const onEliminar = vi.fn();

    render(
      <UsuariosTabla
        usuarios={usuariosMock}
        onVer={onVer}
        onEditar={onEditar}
        onImprimir={onImprimir}
        onEliminar={onEliminar}
      />
    );

    const btnVer = screen.getAllByRole("button", { name: "Ver el detalle de Ana Gómez" })[0];
    const btnEditar = screen.getAllByRole("button", { name: "Modificar a Ana Gómez" })[0];
    const btnImprimir = screen.getAllByRole("button", { name: "Imprimir la ficha de Ana Gómez" })[0];
    const btnEliminar = screen.getAllByRole("button", { name: "Eliminar a Ana Gómez" })[0];

    expect(btnVer).toBeInTheDocument();
    expect(btnEditar).toBeInTheDocument();
    expect(btnImprimir).toBeInTheDocument();
    expect(btnEliminar).toBeInTheDocument();

    await userEvent.click(btnVer);
    expect(onVer).toHaveBeenCalledWith(usuariosMock[0]);

    await userEvent.click(btnEditar);
    expect(onEditar).toHaveBeenCalledWith(usuariosMock[0]);

    await userEvent.click(btnImprimir);
    expect(onImprimir).toHaveBeenCalledWith(usuariosMock[0]);

    await userEvent.click(btnEliminar);
    expect(onEliminar).toHaveBeenCalledWith(usuariosMock[0]);
  });

  it("muestra la barra de paginación cuando hay más de una página", async () => {
    const onCambiarPagina = vi.fn();

    render(
      <UsuariosTabla
        usuarios={usuariosMock}
        pagina={0}
        totalPaginas={3}
        totalElementos={50}
        onCambiarPagina={onCambiarPagina}
      />
    );

    expect(screen.getByText(/página/i)).toBeInTheDocument();
    const btnSiguiente = screen.getByRole("button", { name: /ir a la página siguiente/i });
    expect(btnSiguiente).toBeInTheDocument();

    await userEvent.click(btnSiguiente);
    expect(onCambiarPagina).toHaveBeenCalledWith(1);
  });
});
