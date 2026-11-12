import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { crearEntrenamiento } from "../../api/entrenamientosApi";
import NuevoEntrenamientoPage from "../NuevoEntrenamientoPage";

vi.mock("../../api/entrenamientosApi", () => ({
  crearEntrenamiento: vi.fn(),
}));

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn() },
}));

vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange, id }) => (
    <input
      id={id}
      aria-label={name === "fechaInicio" ? "Fecha de inicio" : "Fecha de fin"}
      name={name}
      value={value}
      onChange={onChange}
    />
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
    if (ruta === "/entrenadores") {
      return Promise.resolve({ data: { contenido: [{ id: 9, nombre: "Luis", apellido: "Paz" }] } });
    }
    return Promise.resolve({ data: { contenido: [] } });
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenamientos/nuevo"]}>
      <NuevoEntrenamientoPage />
    </MemoryRouter>,
  );
}

async function completarFormulario() {
  await userEvent.type(screen.getByLabelText("Nombre"), "Entrenamiento Pretemporada");
  await userEvent.click(await screen.findByText("Elegí la cancha"));
  await userEvent.click(await screen.findByText(/Cancha 3/));
  await userEvent.click(screen.getByText("Elegí el entrenador"));
  await userEvent.click(await screen.findByText("Luis Paz"));
  await userEvent.type(screen.getByLabelText("Objetivo"), "Pretemporada");
  await userEvent.click(screen.getByText("Elegí la modalidad"));
  await userEvent.click(await screen.findByText("Grupal"));
  await userEvent.type(screen.getByLabelText("Fecha de inicio"), "2026-11-02");
  await userEvent.type(screen.getByLabelText("Fecha de fin"), "2026-11-30");
  await userEvent.click(screen.getByLabelText("Lunes"));
  await userEvent.type(screen.getByLabelText("Hora de inicio"), "08:00");
  await userEvent.type(screen.getByLabelText("Hora de fin"), "09:30");
}

describe("NuevoEntrenamientoPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    respuestasDeCatalogo();
  });

  it("solo ofrece entrenadores activos con la certificación validada", async () => {
    renderPage();

    await waitFor(() =>
      expect(axiosClient.get).toHaveBeenCalledWith("/entrenadores", {
        params: { size: 50, activo: true, estado: "VALIDADO" },
      }),
    );
  });

  it("si el formulario está vacío, marca todos los campos y no llama al backend", async () => {
    renderPage();

    await userEvent.click(screen.getByRole("button", { name: /Programar entrenamiento/ }));

    expect(await screen.findByText("Escribí el nombre del entrenamiento.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la cancha.")).toBeInTheDocument();
    expect(screen.getByText("Elegí el entrenador que lo dirige.")).toBeInTheDocument();
    expect(screen.getByText("Escribí el objetivo del entrenamiento.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la modalidad.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la fecha de inicio.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la fecha de fin.")).toBeInTheDocument();
    expect(screen.getByText("Elegí al menos un día de la semana.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la hora de inicio.")).toBeInTheDocument();
    expect(screen.getByText("Elegí la hora de fin.")).toBeInTheDocument();
    expect(screen.getByText("Escribí el cupo máximo.")).toBeInTheDocument();
    expect(crearEntrenamiento).not.toHaveBeenCalled();
  });

  it("no acepta un objetivo con solo espacios", async () => {
    renderPage();

    await userEvent.type(await screen.findByLabelText("Objetivo"), "   ");
    await userEvent.click(screen.getByRole("button", { name: /Programar entrenamiento/ }));

    expect(await screen.findByText("Escribí el objetivo del entrenamiento.")).toBeInTheDocument();
  });

  it("rechaza un cupo mayor al límite del club (RD01)", async () => {
    renderPage();

    await userEvent.type(await screen.findByLabelText("Cupo máximo"), "31");
    await userEvent.click(screen.getByRole("button", { name: /Programar entrenamiento/ }));

    expect(await screen.findByText("El límite del club es 30 alumnos.")).toBeInTheDocument();
    expect(crearEntrenamiento).not.toHaveBeenCalled();
  });

  it("programa el entrenamiento con su objetivo y su cupo (CU37)", async () => {
    crearEntrenamiento.mockResolvedValueOnce({ id: 1 });

    renderPage();

    await completarFormulario();
    await userEvent.type(screen.getByLabelText("Cupo máximo"), "8");
    await userEvent.click(screen.getByRole("button", { name: /Programar entrenamiento/ }));

    await waitFor(() =>
      expect(crearEntrenamiento).toHaveBeenCalledWith({
        nombre: "Entrenamiento Pretemporada",
        entrenadorId: 9,
        canchaId: 3,
        objetivo: "Pretemporada",
        modalidad: "GRUPAL",
        fechaInicio: "2026-11-02",
        fechaFin: "2026-11-30",
        dias: ["MONDAY"],
        horaInicio: "08:00",
        horaFin: "09:30",
        cupoMaximo: 8,
      }),
    );
  });

  it("si el backend rechaza el alta, muestra el mensaje y los campos que marcó", async () => {
    crearEntrenamiento.mockRejectedValueOnce({
      response: {
        status: 422,
        data: { mensaje: "El entrenador ya tiene otra actividad en ese horario.", campos: { entrenadorId: "Está ocupado." } },
      },
    });

    renderPage();

    await completarFormulario();
    await userEvent.type(screen.getByLabelText("Cupo máximo"), "8");
    await userEvent.click(screen.getByRole("button", { name: /Programar entrenamiento/ }));

    expect(await screen.findByText("El entrenador ya tiene otra actividad en ese horario.")).toBeInTheDocument();
    expect(screen.getByText("Está ocupado.")).toBeInTheDocument();
  });
});
