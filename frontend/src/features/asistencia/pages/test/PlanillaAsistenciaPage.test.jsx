import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { obtenerClase } from "@/features/clases/api/clasesApi";
import { obtenerEntrenamiento } from "@/features/entrenamientos/api/entrenamientosApi";
import { guardarPlanilla, obtenerPlanilla } from "../../api/asistenciaApi";
import PlanillaAsistenciaPage from "../PlanillaAsistenciaPage";

vi.mock("../../api/asistenciaApi", () => ({
  obtenerPlanilla: vi.fn(),
  guardarPlanilla: vi.fn(),
}));

vi.mock("@/features/clases/api/clasesApi", () => ({
  obtenerClase: vi.fn(),
}));

vi.mock("@/features/entrenamientos/api/entrenamientosApi", () => ({
  obtenerEntrenamiento: vi.fn(),
}));

// El calendario se prueba aparte; acá solo interesa la fecha que deja.
vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange, id }) => (
    <input id={id} aria-label="Fecha de la planilla" name={name} value={value} onChange={onChange} />
  ),
}));

const actividad = {
  id: 1,
  horaInicio: "18:00:00",
  horaFin: "19:30:00",
  cancha: "Cancha 3",
};

const planilla = [
  { actividadId: 1, socioId: 20, socio: "López, Juan", fecha: "2026-11-02", presente: true },
  { actividadId: 1, socioId: 21, socio: "Ruiz, Eva", fecha: "2026-11-02", presente: false },
];

function renderPage(tipo = "clase") {
  const base = tipo === "clase" ? "/clases" : "/entrenamientos";
  return render(
    <MemoryRouter initialEntries={[`${base}/1/asistencia`]}>
      <Routes>
        <Route path={`${base}/:id/asistencia`} element={<PlanillaAsistenciaPage tipo={tipo} />} />
        <Route path={`${base}/:id`} element={<p>Volvio a la actividad</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("PlanillaAsistenciaPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    obtenerClase.mockResolvedValue(actividad);
    obtenerEntrenamiento.mockResolvedValue(actividad);
    obtenerPlanilla.mockResolvedValue(planilla);
  });

  it("para una clase trae la cabecera de la clase (CU42)", async () => {
    renderPage("clase");

    expect(await screen.findByRole("heading", { name: "Asistencia a la clase" })).toBeInTheDocument();
    expect(screen.getByText(/18:00 a 19:30/)).toBeInTheDocument();
    expect(obtenerClase).toHaveBeenCalledWith("1");
    expect(obtenerEntrenamiento).not.toHaveBeenCalled();
  });

  it("para un entrenamiento trae la cabecera del entrenamiento (CU43)", async () => {
    renderPage("entrenamiento");

    expect(await screen.findByRole("heading", { name: "Asistencia al entrenamiento" })).toBeInTheDocument();
    expect(obtenerEntrenamiento).toHaveBeenCalledWith("1");
    expect(obtenerClase).not.toHaveBeenCalled();
  });

  it("lista los inscriptos con su marca y cuenta presentes y ausentes", async () => {
    renderPage();

    expect(await screen.findByText("López, Juan")).toBeInTheDocument();
    expect(screen.getByText("Ruiz, Eva")).toBeInTheDocument();
    expect(screen.getByLabelText("Marcar a López, Juan como presente")).toBeChecked();
    expect(screen.getByLabelText("Marcar a Ruiz, Eva como presente")).not.toBeChecked();
    expect(screen.getByText("Presentes").parentElement.nextSibling).toHaveTextContent("1");
    expect(screen.getByText("Ausentes").parentElement.nextSibling).toHaveTextContent("1");
  });

  it("alternar una marca cambia el contador sin tocar al resto", async () => {
    renderPage();

    await userEvent.click(await screen.findByLabelText("Marcar a Ruiz, Eva como presente"));

    expect(screen.getByLabelText("Marcar a Ruiz, Eva como presente")).toBeChecked();
    expect(screen.getByLabelText("Marcar a López, Juan como presente")).toBeChecked();
    expect(screen.getByText("Presentes").parentElement.nextSibling).toHaveTextContent("2");
  });

  it("marca a todos presentes y a todos ausentes de una vez", async () => {
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: /Todos presentes/ }));
    expect(screen.getByLabelText("Marcar a Ruiz, Eva como presente")).toBeChecked();

    await userEvent.click(screen.getByRole("button", { name: /Todos ausentes/ }));
    expect(screen.getByLabelText("Marcar a López, Juan como presente")).not.toBeChecked();
    expect(screen.getByText("Presentes").parentElement.nextSibling).toHaveTextContent("0");
  });

  it("guarda la planilla entera de esa fecha y avisa cuántos quedaron presentes", async () => {
    guardarPlanilla.mockResolvedValueOnce([]);

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: /Registrar asistencia/ }));

    await waitFor(() =>
      expect(guardarPlanilla).toHaveBeenCalledWith("1", {
        fecha: expect.any(String),
        marcas: [
          { socioId: 20, presente: true },
          { socioId: 21, presente: false },
        ],
      }),
    );
    expect(await screen.findByText("Se registró la asistencia: 1 de 2 presentes.")).toBeInTheDocument();
  });

  it("tras guardar la planilla con exito, ofrece boton visible para regresar a la actividad", async () => {
    guardarPlanilla.mockResolvedValueOnce([]);

    renderPage("entrenamiento");

    await userEvent.click(await screen.findByRole("button", { name: /Registrar asistencia/ }));

    expect(await screen.findByText("Se registró la asistencia: 1 de 2 presentes.")).toBeInTheDocument();
    const botonVolver = await screen.findByRole("button", { name: /Volver a la actividad/i });
    expect(botonVolver).toBeInTheDocument();

    await userEvent.click(botonVolver);
    expect(await screen.findByText("Volvio a la actividad")).toBeInTheDocument();
  });

  it("al cambiar la fecha pide la planilla de ese día", async () => {
    renderPage();

    await screen.findByText("López, Juan");
    await userEvent.clear(screen.getByLabelText("Fecha de la planilla"));
    await userEvent.type(screen.getByLabelText("Fecha de la planilla"), "2026-11-09");

    await waitFor(() => expect(obtenerPlanilla).toHaveBeenLastCalledWith("1", "2026-11-09"));
  });

  it("sin inscriptos no deja registrar nada", async () => {
    obtenerPlanilla.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No hay alumnos inscriptos para esta fecha.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Registrar asistencia/ })).toBeDisabled();
  });

  it("si falla el guardado, lo muestra y no avisa de éxito", async () => {
    guardarPlanilla.mockRejectedValueOnce({ response: { status: 409, data: { mensaje: "La planilla ya fue cerrada." } } });

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: /Registrar asistencia/ }));

    expect(await screen.findByText("La planilla ya fue cerrada.")).toBeInTheDocument();
    expect(screen.queryByText(/Se registró la asistencia/)).not.toBeInTheDocument();
  });

  it("si falla la carga de la planilla, avisa y deja la lista vacía", async () => {
    obtenerPlanilla.mockRejectedValue({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
    expect(screen.getByText("No hay alumnos inscriptos para esta fecha.")).toBeInTheDocument();
  });

  it("si la fecha no es un día de dictado, muestra el aviso, bloquea la registración y ofrece botón para saltar a la última clase", async () => {
    obtenerEntrenamiento.mockResolvedValueOnce({
      ...actividad,
      fechaInicio: "2026-10-26",
      fechaFin: "2026-12-02",
      dias: ["MONDAY", "WEDNESDAY"],
    });

    renderPage("entrenamiento");

    await screen.findByText("López, Juan");
    await userEvent.clear(screen.getByLabelText("Fecha de la planilla"));
    await userEvent.type(screen.getByLabelText("Fecha de la planilla"), "2026-11-05");

    expect(await screen.findByText("No se puede registrar asistencia en esta fecha")).toBeInTheDocument();
    expect(screen.getByText(/no corresponde a los días de dictado/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Registrar asistencia/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Todos presentes/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Todos ausentes/ })).toBeDisabled();
    expect(screen.getByLabelText("Marcar a López, Juan como presente")).toBeDisabled();

    // El botón ofrece saltar a la última clase dictada (Miércoles 04/11/2026)
    const botonUltimaClase = screen.getByRole("button", { name: /Ir a la última clase/i });
    expect(botonUltimaClase).toBeInTheDocument();
    await userEvent.click(botonUltimaClase);

    expect(screen.queryByText("No se puede registrar asistencia en esta fecha")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Registrar asistencia/ })).toBeEnabled();
  });
});
