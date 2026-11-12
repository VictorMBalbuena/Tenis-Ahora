import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { crearProfesor } from "../../api/profesoresApi";
import NuevoProfesorPage from "../NuevoProfesorPage";

vi.mock("../../api/profesoresApi", () => ({
  crearProfesor: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/profesores/nuevo"]}>
      <NuevoProfesorPage />
    </MemoryRouter>
  );
}

async function completarFormulario() {
  await userEvent.type(screen.getByLabelText("Nombre"), "Ana");
  await userEvent.type(screen.getByLabelText("Apellido"), "Gómez");
  await userEvent.type(screen.getByLabelText("DNI"), "30123456");
  await userEvent.type(screen.getByLabelText("Correo electrónico"), "ana@correo.com");
  await userEvent.type(screen.getByLabelText("Número de teléfono"), "12345678");
  await userEvent.type(screen.getByLabelText("Contraseña inicial"), "Clave-segura-123");
  await userEvent.type(screen.getByLabelText("Título habilitante"), "Profesor Nacional de Tenis");
  await userEvent.type(screen.getByLabelText("Número de certificación"), "CERT-001");
  await userEvent.type(screen.getByLabelText("Entidad emisora"), "FAT");

  await userEvent.click(screen.getByLabelText("Fecha de emisión"));
  const diaDelMes = screen.getAllByRole("button").find((b) => /^\d{1,2}$/.test(b.textContent) && !b.disabled);
  await userEvent.click(diaDelMes);
}

describe("NuevoProfesorPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra errores de validación si falta completar campos obligatorios", async () => {
    renderPage();
    await userEvent.click(screen.getByRole("button", { name: "Registrar profesor" }));

    expect(await screen.findByText("Escribí el nombre.")).toBeInTheDocument();
    expect(crearProfesor).not.toHaveBeenCalled();
  });

  it("al completar los datos obligatorios, registra el profesor", async () => {
    crearProfesor.mockResolvedValueOnce({ id: 9 });
    renderPage();

    await completarFormulario();
    await userEvent.click(screen.getByRole("button", { name: "Registrar profesor" }));

    expect(crearProfesor).toHaveBeenCalledTimes(1);
    const payload = crearProfesor.mock.calls[0][0];
    expect(payload.nombre).toBe("Ana");
    expect(payload.certificacion.numeroCertificacion).toBe("CERT-001");
  }, 30000);

  it("si el backend rechaza el alta, muestra el mensaje de error", async () => {
    crearProfesor.mockRejectedValueOnce({ response: { data: { mensaje: "Ya existe una persona con ese DNI" } } });
    renderPage();

    await completarFormulario();
    await userEvent.click(screen.getByRole("button", { name: "Registrar profesor" }));

    expect(await screen.findByText("Ya existe una persona con ese DNI")).toBeInTheDocument();
  }, 30000);
});
