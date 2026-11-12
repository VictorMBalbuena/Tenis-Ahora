import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatosPerfilResumen } from "../DatosPerfilResumen";

const mockPerfil = {
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

describe("DatosPerfilResumen", () => {
  it("renderiza los datos estructurados en listas de definición semánticas <dl>", () => {
    const { container } = render(<DatosPerfilResumen perfil={mockPerfil} onEditar={vi.fn()} />);
    const dls = container.querySelectorAll("dl");
    expect(dls.length).toBeGreaterThan(0);
    expect(screen.getByText("Ana")).toBeInTheDocument();
    expect(screen.getByText("Av. Siempreviva 742")).toBeInTheDocument();
    expect(screen.getByText("(11) 22786604")).toBeInTheDocument();
  });

  it("utiliza clases de padding adaptativo mobile-first en la tarjeta principal", () => {
    const { container } = render(<DatosPerfilResumen perfil={mockPerfil} onEditar={vi.fn()} />);
    const tarjeta = container.firstChild;
    expect(tarjeta.className).toMatch(/p-4\s+sm:p-6\s+lg:p-8/);
  });

  it("dispara onEditar al hacer clic en el botón", async () => {
    const onEditar = vi.fn();
    render(<DatosPerfilResumen perfil={mockPerfil} onEditar={onEditar} />);
    await userEvent.click(screen.getByRole("button", { name: /Editar datos/i }));
    expect(onEditar).toHaveBeenCalledTimes(1);
  });
});
