import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { UsuarioFormDialog } from "./UsuarioFormDialog";
import * as api from "../api/usuariosApi";

vi.mock("../api/usuariosApi", () => ({
  crearUsuario: vi.fn(),
  actualizarUsuario: vi.fn(),
  obtenerUsuario: vi.fn(),
}));

// El calendario de DateField se prueba aparte; acá solo interesa que el valor
// elegido llegue al submit, así que se reemplaza por un input de texto simple
// con la misma API (value/onChange con evento sintético target.name/value).
vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange, id }) => (
    <input
      id={id}
      value={value}
      onChange={(e) => onChange({ target: { name, value: e.target.value } })}
    />
  ),
}));

describe("UsuarioFormDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el formulario de alta con campo de contraseña", () => {
    render(
      <UsuarioFormDialog
        abierto={true}
        tipo="SOCIO"
        onCerrar={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    expect(screen.getByRole("heading", { name: "Registrar Nuevo Socio" })).toBeInTheDocument();
    expect(screen.getByLabelText("Contraseña Inicial")).toBeInTheDocument();
  });

  it("muestra errores de validación si se envía vacío", async () => {
    render(
      <UsuarioFormDialog
        abierto={true}
        tipo="SOCIO"
        onCerrar={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Crear Usuario" }));

    expect(screen.getByText("El nombre es obligatorio.")).toBeInTheDocument();
    expect(screen.getByText("El apellido es obligatorio.")).toBeInTheDocument();
    expect(screen.getByText("El DNI es obligatorio.")).toBeInTheDocument();
    expect(screen.getByText("La contraseña es obligatoria.")).toBeInTheDocument();
    expect(api.crearUsuario).not.toHaveBeenCalled();
  });

  it("envía la petición al backend cuando los campos son válidos", async () => {
    const onGuardado = vi.fn();
    api.crearUsuario.mockResolvedValueOnce({ id: 10 });

    render(
      <UsuarioFormDialog
        abierto={true}
        tipo="SOCIO"
        onCerrar={vi.fn()}
        onGuardado={onGuardado}
      />
    );

    await userEvent.type(screen.getByLabelText("Nombre"), "Ana");
    await userEvent.type(screen.getByLabelText("Apellido"), "Gómez");
    await userEvent.type(screen.getByLabelText("DNI"), "30123456");
    await userEvent.type(screen.getByLabelText("Fecha de Nacimiento"), "1990-05-17");
    await userEvent.type(screen.getByLabelText("Calle"), "Av. Siempreviva");
    await userEvent.type(screen.getByLabelText("Número de Calle"), "742");
    await userEvent.type(screen.getByLabelText("Código de Área"), "11");
    await userEvent.type(screen.getByLabelText("Teléfono"), "12345678");
    await userEvent.type(screen.getByLabelText("Correo Electrónico"), "ana@correo.com");
    await userEvent.type(screen.getByLabelText("Contraseña Inicial"), "PasswordSeguro123");

    await userEvent.click(screen.getByRole("button", { name: "Crear Usuario" }));

    await waitFor(() => {
      expect(api.crearUsuario).toHaveBeenCalledWith("SOCIO", {
        nombre: "Ana",
        apellido: "Gómez",
        dni: "30123456",
        fechaNacimiento: "1990-05-17",
        calle: "Av. Siempreviva",
        numeroCalle: "742",
        codigoArea: "11",
        numeroTelefono: "12345678",
        email: "ana@correo.com",
        password: "PasswordSeguro123",
      });
      expect(onGuardado).toHaveBeenCalled();
    });
  }, 30000);

  it("en modo edición no muestra el campo de contraseña y carga los datos previos", async () => {
    api.obtenerUsuario.mockResolvedValueOnce({
      id: 5,
      nombre: "Carlos",
      apellido: "López",
      dni: "32987654",
      fechaNacimiento: "1988-10-20",
      calle: "San Martín",
      numeroCalle: "100",
      codigoArea: "11",
      numeroTelefono: "98765432",
      email: "carlos@correo.com",
    });

    render(
      <UsuarioFormDialog
        abierto={true}
        tipo="SOCIO"
        usuario={{ id: 5 }}
        onCerrar={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    expect(screen.getByRole("heading", { name: "Modificar Socio" })).toBeInTheDocument();
    expect(screen.queryByLabelText("Contraseña Inicial")).not.toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByLabelText("Nombre")).toHaveValue("Carlos");
      expect(screen.getByLabelText("Apellido")).toHaveValue("López");
    });
  });
});
