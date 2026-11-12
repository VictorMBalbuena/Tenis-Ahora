import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { listarUsuarios } from "@/features/usuarios/api/usuariosApi";
import { desinscribirAlumno, listarInscriptos, obtenerClase } from "../../api/clasesApi";
import ClaseDetallePage from "../ClaseDetallePage";

vi.mock("../../api/clasesApi", () => ({
  obtenerClase: vi.fn(),
  listarInscriptos: vi.fn(),
  desinscribirAlumno: vi.fn(),
  inscribirAlumno: vi.fn(),
  imprimirClase: vi.fn(),
  eliminarClase: vi.fn(),
}));

vi.mock("@/features/usuarios/api/usuariosApi", () => ({
  listarUsuarios: vi.fn(),
}));

const unaClase = {
  id: 1,
  nombre: "Tenis inicial",
  estado: "PROGRAMADA",
  fechaInicio: "2099-11-02",
  fechaFin: "2100-03-02",
  dias: ["MONDAY"],
  horaInicio: "18:00:00",
  horaFin: "19:30:00",
  nivel: "INTERMEDIO",
  modalidad: "GRUPAL",
  cupoMaximo: 12,
  cupoOcupado: 1,
  cupoDisponible: 11,
  cancha: "Cancha 3",
  profesor: "Ana Gómez",
};

const unaInscripcion = {
  socioId: 20,
  socio: "Juan López",
  dni: "30123456",
  inscripcionId: 50,
  estado: "CONFIRMADA",
  fechaInscripcion: "2026-10-20",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/clases/1"]}>
      <Routes>
        <Route path="/clases/:id" element={<ClaseDetallePage />} />
        <Route path="/clases" element={<p>Listado de clases</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ClaseDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listarUsuarios.mockResolvedValue({ contenido: [] });
  });

  it("muestra la clase con su profesor, su cancha y sus alumnos (CU34)", async () => {
    obtenerClase.mockResolvedValueOnce(unaClase);
    listarInscriptos.mockResolvedValueOnce([unaInscripcion]);

    renderPage();

    expect(await screen.findByRole("heading", { name: "Tenis inicial" })).toBeInTheDocument();
    expect(screen.getAllByText(/Ana Gómez/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Cancha 3/).length).toBeGreaterThan(0);
    expect(screen.getByText("Juan López")).toBeInTheDocument();
  });

  it("da de baja una inscripción y avisa que se liberó el lugar (CU81)", async () => {
    obtenerClase.mockResolvedValue(unaClase);
    listarInscriptos.mockResolvedValue([unaInscripcion]);
    desinscribirAlumno.mockResolvedValueOnce(undefined);

    renderPage();

    await userEvent.click(await screen.findByLabelText("Dar de baja a Juan López"));

    await waitFor(() => expect(desinscribirAlumno).toHaveBeenCalledWith("1", 20));
    expect(await screen.findByText("Se liberó el lugar del alumno.")).toBeInTheDocument();
  });

  it("con el cupo completo no deja abrir la inscripción", async () => {
    obtenerClase.mockResolvedValueOnce({ ...unaClase, cupoMaximo: 10, cupoOcupado: 10, cupoDisponible: 0 });
    listarInscriptos.mockResolvedValueOnce([unaInscripcion]);

    renderPage();

    expect(await screen.findByRole("button", { name: /Inscribir alumno/ })).toBeDisabled();
  });

  it("si el listado de inscriptos falla, el detalle se muestra igual", async () => {
    obtenerClase.mockResolvedValueOnce(unaClase);
    listarInscriptos.mockRejectedValueOnce({ response: { status: 500 } });

    renderPage();

    expect(await screen.findByRole("heading", { name: "Tenis inicial" })).toBeInTheDocument();
    expect(screen.getByText("Todavía no hay alumnos inscriptos.")).toBeInTheDocument();
  });

  it("si no existe la clase, muestra el error y el botón de volver", async () => {
    obtenerClase.mockRejectedValueOnce({ response: { status: 404, data: { mensaje: "No existe la clase." } } });
    listarInscriptos.mockResolvedValueOnce([]);

    renderPage();

    expect(await screen.findByText("No existe la clase.")).toBeInTheDocument();
  });

  it("el profesor no ve la fila 'Profesor' en la ficha de detalle", async () => {
    useAuthStore.setState({ user: { rol: "PROFESOR", id: 10 } });
    obtenerClase.mockResolvedValueOnce(unaClase);
    listarInscriptos.mockResolvedValueOnce([]);
    renderPage();
    expect(await screen.findByText("Tenis inicial")).toBeInTheDocument();
    expect(screen.queryByText("Profesor")).not.toBeInTheDocument();
  });
});
