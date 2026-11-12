import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatosPersonalesCard } from "../datos-personales-card";

describe("DatosPersonalesCard", () => {
  const datos = {
    nombre: "Bruno",
    apellido: "Tealdi",
    dni: "44966422",
    fechaNacimiento: "",
    email: "bruno@correo.com",
    codigoArea: "11",
    numeroTelefono: "22786604",
    calle: "",
    numeroCalle: "",
  };

  it("muestra los valores actuales", () => {
    render(<DatosPersonalesCard datos={datos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByDisplayValue("Bruno")).toBeInTheDocument();
    expect(screen.getByDisplayValue("44966422")).toBeInTheDocument();
  });

  it("muestra el error de un campo", () => {
    render(<DatosPersonalesCard datos={datos} errores={{ dni: "El DNI son 7 u 8 dígitos, sin puntos." }} onChange={() => vi.fn()} />);
    expect(screen.getByText("El DNI son 7 u 8 dígitos, sin puntos.")).toBeInTheDocument();
  });

  it("escribir en el nombre dispara onChange con el campo correcto", async () => {
    const onChange = vi.fn(() => vi.fn());
    render(<DatosPersonalesCard datos={datos} errores={{}} onChange={onChange} />);

    await userEvent.type(screen.getByDisplayValue("Bruno"), "!");
    expect(onChange).toHaveBeenCalledWith("nombre");
  });
});
