import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { FormularioJugadores } from "../FormularioJugadores";

describe("FormularioJugadores", () => {
  it("renderiza la modalidad seleccionada y permite cambiar a otra", () => {
    const onChangeModalidad = vi.fn();
    render(
      <FormularioJugadores
        modalidad="SINGLE"
        acompaniantes={[{ personaId: null, nombre: "Juan", apellido: "Perez", documento: "" }]}
        onChangeModalidad={onChangeModalidad}
      />
    );

    const btnDobles = screen.getByRole("button", { name: /dobles/i });
    fireEvent.click(btnDobles);
    expect(onChangeModalidad).toHaveBeenCalledWith("DOBLES");
  });

  it("permite editar campos de texto y dispara onChangeAcompaniante", () => {
    const onChangeAcompaniante = vi.fn();
    render(
      <FormularioJugadores
        modalidad="SINGLE"
        acompaniantes={[{ personaId: null, nombre: "", apellido: "", documento: "" }]}
        onChangeAcompaniante={onChangeAcompaniante}
      />
    );

    const inputNombre = screen.getByPlaceholderText(/nombre/i);
    fireEvent.change(inputNombre, { target: { value: "Franco" } });
    expect(onChangeAcompaniante).toHaveBeenCalledWith(0, "nombre", "Franco");

    const inputApellido = screen.getByPlaceholderText(/apellido/i);
    fireEvent.change(inputApellido, { target: { value: "Armani" } });
    expect(onChangeAcompaniante).toHaveBeenCalledWith(0, "apellido", "Armani");
  });

  it("permite alternar entre tipo Invitado y Socio del club", () => {
    const onChangeAcompaniante = vi.fn();
    render(
      <FormularioJugadores
        modalidad="SINGLE"
        acompaniantes={[{ personaId: null, nombre: "Socio", apellido: "Uno", documento: "" }]}
        onChangeAcompaniante={onChangeAcompaniante}
      />
    );

    const btnSocio = screen.getByRole("button", { name: /socio del club/i });
    fireEvent.click(btnSocio);
    expect(onChangeAcompaniante).toHaveBeenCalledWith(0, {
      tipo: "SOCIO",
      personaId: null,
      nombre: "",
      apellido: "",
      documento: "",
    });
  });

  it("muestra quién quedó como acompañante socio", () => {
    render(
      <FormularioJugadores
        modalidad="SINGLE"
        acompaniantes={[{ personaId: 100, nombre: "Socio", apellido: "Uno", documento: "" }]}
        onChangeAcompaniante={vi.fn()}
      />
    );

    expect(screen.getByPlaceholderText(/dni sin puntos/i)).toBeInTheDocument();
    expect(screen.getByText(/socio uno quedó como acompañante/i)).toBeInTheDocument();
  });
});
