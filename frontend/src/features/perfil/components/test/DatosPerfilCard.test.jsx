import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatosPerfilCard } from "../DatosPerfilCard";

vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange, id }) => (
    <input id={id} aria-label="Fecha de nacimiento" name={name} value={value} onChange={onChange} />
  ),
}));

const mockDatos = {
  nombre: "Ana",
  apellido: "Gómez",
  dni: "30123456",
  fechaNacimiento: "1990-05-17",
  email: "ana@correo.com",
  codigoArea: "11",
  numeroTelefono: "22786604",
  calle: "Av. Siempreviva",
  numeroCalle: "742",
};

describe("DatosPerfilCard", () => {
  it("utiliza padding adaptativo mobile-first en el contenedor", () => {
    const { container } = render(<DatosPerfilCard datos={mockDatos} errores={{}} onChange={() => vi.fn()} />);
    const tarjeta = container.firstChild;
    expect(tarjeta.className).toMatch(/p-4\s+sm:p-6\s+lg:p-8/);
  });

  it("renderiza todos los campos editables y el DNI en solo lectura", () => {
    render(<DatosPerfilCard datos={mockDatos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByLabelText(/Nombre/i)).toHaveValue("Ana");
    expect(screen.getByLabelText(/Apellido/i)).toHaveValue("Gómez");
    expect(screen.getByLabelText(/DNI/i)).toHaveAttribute("readonly");
    expect(screen.getByLabelText(/Correo electrónico/i)).toHaveValue("ana@correo.com");
    expect(screen.getByLabelText(/Código de área/i)).toHaveValue("11");
    expect(screen.getByLabelText(/Teléfono/i)).toHaveValue("22786604");
    expect(screen.getByLabelText(/Calle/i)).toHaveValue("Av. Siempreviva");
    expect(screen.getByLabelText(/Número/i)).toHaveValue("742");
  });

  it("permite la edición disparando el onChange correspondiente", async () => {
    const handleChange = vi.fn(() => vi.fn());
    render(<DatosPerfilCard datos={mockDatos} errores={{}} onChange={handleChange} />);
    const nombreInput = screen.getByLabelText(/Nombre/i);
    await userEvent.type(nombreInput, "a");
    expect(handleChange).toHaveBeenCalledWith("nombre");
  });

  it("muestra la etiqueta Cod. Área adaptada para pantallas reducidas", () => {
    render(<DatosPerfilCard datos={mockDatos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByText(/Cod\. Área/i)).toBeInTheDocument();
  });
});
