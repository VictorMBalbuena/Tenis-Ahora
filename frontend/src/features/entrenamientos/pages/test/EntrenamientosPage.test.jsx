import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { eliminarEntrenamiento, listarEntrenamientos } from "../../api/entrenamientosApi";
import { EntrenamientosPage } from "../EntrenamientosPage";

vi.mock("../../api/entrenamientosApi", () => ({
  listarEntrenamientos: vi.fn(),
  imprimirEntrenamiento: vi.fn(),
  eliminarEntrenamiento: vi.fn(),
}));

const unEntrenamiento = {
  id: 1,
  nombre: "Entrenamiento funcional",
  estado: "PROGRAMADA",
  fechaInicio: "2026-11-02",
  fechaFin: "2027-03-02",
  dias: ["MONDAY"],
  horaInicio: "08:00:00",
  horaFin: "09:30:00",
  modalidad: "GRUPAL",
  objetivo: "Preparación física de pretemporada",
  cupoMaximo: 8,
  cupoOcupado: 3,
  cupoDisponible: 5,
  cancha: "Cancha 3",
  entrenador: "Luis Paz",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenamientos"]}>
      <EntrenamientosPage />
    </MemoryRouter>,
  );
}

describe("EntrenamientosPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" } });
  });

  it("lista los entrenamientos con su entrenador, su objetivo y su cancha", async () => {
    listarEntrenamientos.mockResolvedValueOnce({ contenido: [unEntrenamiento], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findAllByText("Luis Paz")).not.toHaveLength(0);
    expect(screen.getAllByText(/Preparación física de pretemporada/).length).toBeGreaterThan(0);
    expect(screen.getAllByText("Cancha 3").length).toBeGreaterThan(0);
  });

  it("muestra los lugares libres y avisa cuando el cupo está completo", async () => {
    listarEntrenamientos.mockResolvedValueOnce({
      contenido: [unEntrenamiento, { ...unEntrenamiento, id: 2, cupoMaximo: 6, cupoOcupado: 6, cupoDisponible: 0 }],
      totalPaginas: 1,
      totalElementos: 2,
    });

    renderPage();

    expect(await screen.findAllByText(/5 lugares/)).not.toHaveLength(0);
    expect(screen.getAllByText("Cupo completo").length).toBeGreaterThan(0);
  });

  it("al elegir un estado, pide al backend con ese filtro", async () => {
    listarEntrenamientos.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await waitFor(() => expect(listarEntrenamientos).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByText("Todos los estados"));
    await userEvent.click(await screen.findByText("Programado"));

    await waitFor(() =>
      expect(listarEntrenamientos).toHaveBeenLastCalledWith(expect.objectContaining({ estado: "PROGRAMADA" })),
    );
  });

  it("en la vista movil la tarjeta permite ir a ver el detalle y tomar asistencia directamente", async () => {
    listarEntrenamientos.mockResolvedValueOnce({ contenido: [unEntrenamiento], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    const botonAsistencia = await screen.findByRole("button", { name: /Registrar asistencia de/i });
    expect(botonAsistencia).toBeInTheDocument();

    const tarjetas = screen.getAllByRole("button", { name: /Ver detalle de/i });
    expect(tarjetas.length).toBeGreaterThanOrEqual(1);
  });

  it("al entrenador le muestra solo los suyos y no le ofrece programar (CU84)", async () => {
    useAuthStore.setState({ user: { id: 9, email: "luis@tenisahora.com", rol: "ENTRENADOR" } });
    listarEntrenamientos.mockResolvedValue({ contenido: [unEntrenamiento], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findByRole("heading", { name: "Mis entrenamientos" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nuevo entrenamiento/ })).not.toBeInTheDocument();
    await waitFor(() => expect(listarEntrenamientos).toHaveBeenLastCalledWith(expect.objectContaining({ entrenadorId: "9", size: 50 })));
  });

  it("al empleado sí le ofrece programar uno nuevo", async () => {
    listarEntrenamientos.mockResolvedValue({ contenido: [unEntrenamiento], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findByRole("button", { name: /Nuevo entrenamiento/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Entrenamientos" })).toBeInTheDocument();
  });

  it("al eliminar uno avisa y recarga el listado", async () => {
    listarEntrenamientos.mockResolvedValue({ contenido: [unEntrenamiento], totalPaginas: 1, totalElementos: 1 });
    eliminarEntrenamiento.mockResolvedValueOnce(undefined);

    renderPage();

    await userEvent.click((await screen.findAllByLabelText("Eliminar Entrenamiento funcional"))[0]);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar entrenamiento" }));

    await waitFor(() => expect(eliminarEntrenamiento).toHaveBeenCalledWith(1));
    expect(await screen.findByText(/Se eliminó el entrenamiento/)).toBeInTheDocument();
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarEntrenamientos.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });

  it("al entrenador sin entrenamientos no le ofrece programar", async () => {
    useAuthStore.setState({ user: { id: 9, email: "luis@tenisahora.com", rol: "ENTRENADOR" } });
    listarEntrenamientos.mockResolvedValueOnce({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();

    expect(await screen.findByText("Todavía no tenés entrenamientos asignados.")).toBeInTheDocument();
    expect(screen.queryByText("Programar el primero")).not.toBeInTheDocument();
  });

  it("al entrenador con certificacion pendiente muestra el empty state amigable y oculta metricas y error", async () => {
    useAuthStore.setState({ user: { id: 9, email: "luis@tenisahora.com", rol: "ENTRENADOR" } });
    const error422 = {
      response: {
        status: 422,
        data: { codigo: "INSTRUCTOR_NO_VALIDADO", mensaje: "No tenés la certificación validada" },
      },
    };
    listarEntrenamientos.mockRejectedValueOnce(error422);

    renderPage();

    expect(await screen.findByText(/Certificación deportiva en proceso de validación/i)).toBeInTheDocument();
    expect(screen.queryByText(/No tenés la certificación validada/i)).not.toBeInTheDocument();
    expect(screen.queryByText("Alumnos inscriptos")).not.toBeInTheDocument();
  });
});
