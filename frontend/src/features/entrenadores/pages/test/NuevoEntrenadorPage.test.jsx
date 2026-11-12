import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { crearEntrenador } from "../../api/entrenadoresApi";
import NuevoEntrenadorPage from "../NuevoEntrenadorPage";

vi.mock("../../api/entrenadoresApi", () => ({
  crearEntrenador: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenadores/nuevo"]}>
      <NuevoEntrenadorPage />
    </MemoryRouter>
  );
}

async function completarFormulario() {
  await userEvent.type(screen.getByLabelText("Nombre"), "Lucas");
  await userEvent.type(screen.getByLabelText("Apellido"), "Pérez");
  await userEvent.type(screen.getByLabelText("DNI"), "31123456");
  await userEvent.type(screen.getByLabelText("Correo electrónico"), "lucas@correo.com");
  await userEvent.type(screen.getByLabelText("Número de teléfono"), "12345678");
  await userEvent.type(screen.getByLabelText("Contraseña inicial"), "Clave-segura-123");
  await userEvent.type(screen.getByLabelText("Certificación deportiva"), "Entrenador Nacional de Tenis");
  await userEvent.type(screen.getByLabelText("Número de certificación"), "PF-2023-0092");
  await userEvent.type(screen.getByLabelText("Entidad emisora"), "ISEF");

  await userEvent.click(screen.getByLabelText("Fecha de emisión"));
  const diaDelMes = screen.getAllByRole("button").find((b) => /^\d{1,2}$/.test(b.textContent) && !b.disabled);
  await userEvent.click(diaDelMes);
}

describe("NuevoEntrenadorPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra errores de validación si falta completar campos obligatorios", async () => {
    renderPage();
    await userEvent.click(screen.getByRole("button", { name: "Registrar entrenador" }));

    expect(await screen.findByText("Escribí el nombre.")).toBeInTheDocument();
    expect(crearEntrenador).not.toHaveBeenCalled();
  });

  it("al completar los datos obligatorios, registra el entrenador", async () => {
    crearEntrenador.mockResolvedValueOnce({ id: 9 });
    renderPage();

    await completarFormulario();
    await userEvent.click(screen.getByRole("button", { name: "Registrar entrenador" }));

    expect(crearEntrenador).toHaveBeenCalledTimes(1);
    const payload = crearEntrenador.mock.calls[0][0];
    expect(payload.nombre).toBe("Lucas");
    expect(payload.certificacion.numeroCertificacion).toBe("PF-2023-0092");
  }, 30000);

  it("si el backend rechaza el alta, muestra el mensaje de error", async () => {
    crearEntrenador.mockRejectedValueOnce({ response: { data: { mensaje: "Ya existe una persona con ese DNI" } } });
    renderPage();

    await completarFormulario();
    await userEvent.click(screen.getByRole("button", { name: "Registrar entrenador" }));

    expect(await screen.findByText("Ya existe una persona con ese DNI")).toBeInTheDocument();
  }, 30000);
});
