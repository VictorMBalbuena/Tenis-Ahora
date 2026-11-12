import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { crearClase } from "../../api/clasesApi";
import NuevaClasePage from "../NuevaClasePage";

vi.mock("../../api/clasesApi", () => ({
  crearClase: vi.fn(),
}));

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn() },
}));

// El calendario y el reloj se prueban aparte; acá solo interesa el valor que
// dejan en el formulario, igual que en UsuarioFormDialog.test.
vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange }) => (
    <input aria-label={name === "fechaInicio" ? "Fecha de inicio" : "Fecha de fin"} name={name} value={value} onChange={onChange} />
  ),
}));

vi.mock("@/components/ui/time-field", () => ({
  TimeField: ({ name, value, onChange }) => (
    <input aria-label={name === "horaInicio" ? "Hora de inicio" : "Hora de fin"} name={name} value={value} onChange={onChange} />
  ),
}));

function respuestasDeCatalogo() {
  axiosClient.get.mockImplementation((ruta) => {
    if (ruta === "/canchas") {
      return Promise.resolve({ data: { contenido: [{ id: 3, identificador: "Cancha 3", superficie: "POLVO" }] } });
    }
    if (ruta === "/profesores") {
      return Promise.resolve({ data: { contenido: [{ id: 8, nombre: "Ana", apellido: "Gómez" }] } });
    }
    return Promise.resolve({ data: { contenido: [] } });
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/clases/nueva"]}>
      <NuevaClasePage />
    </MemoryRouter>,
  );
}

async function completarFormulario() {
  await userEvent.type(screen.getByLabelText("Nombre"), "Tenis para principiantes");
  await userEvent.click(await screen.findByText("Elegí la cancha"));
  await userEvent.click(await screen.findByText(/Cancha 3/));
  await userEvent.click(screen.getByText("Elegí el profesor"));
  await userEvent.click(await screen.findByText("Ana Gómez"));
  await userEvent.click(screen.getByText("Elegí el nivel"));
  await userEvent.click(await screen.findByText("Intermedio"));
  await userEvent.click(screen.getByText("Elegí la modalidad"));
  await userEvent.click(await screen.findByText("Grupal"));
  await userEvent.click(screen.getByText("Lunes"));
  await userEvent.type(screen.getByLabelText("Fecha de inicio"), "2026-11-02");
  await userEvent.type(screen.getByLabelText("Fecha de fin"), "2027-03-02");
  await userEvent.type(screen.getByLabelText("Hora de inicio"), "18:00");
  await userEvent.type(screen.getByLabelText("Hora de fin"), "19:30");
}

describe("NuevaClasePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    respuestasDeCatalogo();
  });

  it("trae las canchas y los profesores para poder elegirlos", async () => {
    renderPage();

    await userEvent.click(await screen.findByText("Elegí la cancha"));
    expect(await screen.findByText(/Cancha 3/)).toBeInTheDocument();

    await userEvent.click(screen.getByText("Elegí el profesor"));
    expect(await screen.findByText("Ana Gómez")).toBeInTheDocument();
  }, 30000);

  it("si el formulario está vacío, marca todos los campos y no llama al backend", async () => {
    renderPage();

    await userEvent.click(screen.getByRole("button", { name: /Programar clase/ }));

    expect(await screen.findByText("Escribí un nombre para la clase.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la cancha.")).toBeInTheDocument();
    expect(screen.getByText("Elegí el profesor que la dicta.")).toBeInTheDocument();
    expect(screen.getByText("Elegí el nivel.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la modalidad.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la fecha de inicio.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la fecha de fin.")).toBeInTheDocument();
    expect(screen.getByText("Elegí al menos un día de la semana.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la hora de inicio.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la hora de fin.")).toBeInTheDocument();
    expect(crearClase).not.toHaveBeenCalled();
  });

  it("rechaza un cupo mayor al límite del club (RD01)", async () => {
    renderPage();

    await userEvent.click(await screen.findByText("Elegí la cancha"));
    await userEvent.click(await screen.findByText(/Cancha 3/));
    await userEvent.type(screen.getByLabelText("Cupo máximo"), "31");
    await userEvent.click(screen.getByRole("button", { name: /Programar clase/ }));

    expect(await screen.findByText("El límite del club es 30 alumnos.")).toBeInTheDocument();
    expect(crearClase).not.toHaveBeenCalled();
  });

  it("rechaza una hora de fin anterior a la de inicio", async () => {
    renderPage();

    await userEvent.type(await screen.findByLabelText("Hora de inicio"), "20:00");
    await userEvent.type(screen.getByLabelText("Hora de fin"), "18:00");
    await userEvent.click(screen.getByRole("button", { name: /Programar clase/ }));

    expect(await screen.findByText("Tiene que ser posterior a la hora de inicio.")).toBeInTheDocument();
    expect(crearClase).not.toHaveBeenCalled();
  });

  it("programa la clase con el cupo cargado (CU32 y CU70)", async () => {
    crearClase.mockResolvedValueOnce({ id: 1 });

    renderPage();

    await completarFormulario();
    await userEvent.type(screen.getByLabelText("Cupo máximo"), "12");
    await userEvent.click(screen.getByRole("button", { name: /Programar clase/ }));

    await waitFor(() =>
      expect(crearClase).toHaveBeenCalledWith({
        nombre: "Tenis para principiantes",
        profesorId: 8,
        canchaId: 3,
        nivel: "INTERMEDIO",
        modalidad: "GRUPAL",
        fechaInicio: "2026-11-02",
        fechaFin: "2027-03-02",
        dias: ["MONDAY"],
        horaInicio: "18:00",
        horaFin: "19:30",
        cupoMaximo: 12,
      }),
    );
  }, 30000);

  it("si el backend rechaza el alta, muestra el mensaje y los campos que marcó", async () => {
    crearClase.mockRejectedValueOnce({
      response: {
        status: 422,
        data: { mensaje: "La cancha ya está ocupada en ese horario.", campos: { fechaInicio: "Hay otra clase en ese horario." } },
      },
    });

    renderPage();

    await completarFormulario();
    await userEvent.type(screen.getByLabelText("Cupo máximo"), "12");
    await userEvent.click(screen.getByRole("button", { name: /Programar clase/ }));

    expect(await screen.findByText("La cancha ya está ocupada en ese horario.")).toBeInTheDocument();
    expect(screen.getByText("Hay otra clase en ese horario.")).toBeInTheDocument();
  }, 30000);
});
