import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { eliminarClase, listarClases } from "../../api/clasesApi";
import { ClasesPage } from "../ClasesPage";

vi.mock("../../api/clasesApi", () => ({
  listarClases: vi.fn(),
  imprimirClase: vi.fn(),
  eliminarClase: vi.fn(),
}));

const unaClase = {
  id: 1,
  nombre: "Tenis inicial",
  estado: "PROGRAMADA",
  fechaInicio: "2026-11-02",
  fechaFin: "2027-03-02",
  dias: ["MONDAY"],
  horaInicio: "18:00:00",
  horaFin: "19:30:00",
  nivel: "INTERMEDIO",
  modalidad: "GRUPAL",
  cupoMaximo: 12,
  cupoOcupado: 4,
  cupoDisponible: 8,
  cancha: "Cancha 3",
  profesor: "Ana Gómez",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/clases"]}>
      <ClasesPage />
    </MemoryRouter>,
  );
}

describe("ClasesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" } });
  });

  it("lista las clases que trae el servidor con su profesor y su cancha", async () => {
    listarClases.mockResolvedValueOnce({ contenido: [unaClase], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findAllByText("Ana Gómez")).not.toHaveLength(0);
    expect(screen.getAllByText("Cancha 3").length).toBeGreaterThan(0);
    expect(screen.getAllByText(/18:00 a 19:30/).length).toBeGreaterThan(0);
  });

  it("muestra los lugares que quedan y avisa cuando el cupo está completo", async () => {
    listarClases.mockResolvedValueOnce({
      contenido: [unaClase, { ...unaClase, id: 2, cupoMaximo: 10, cupoOcupado: 10, cupoDisponible: 0 }],
      totalPaginas: 1,
      totalElementos: 2,
    });

    renderPage();

    expect(await screen.findAllByText(/8 lugares/)).not.toHaveLength(0);
    expect(screen.getAllByText("Cupo completo").length).toBeGreaterThan(0);
  });

  it("al elegir un nivel, pide al backend con ese filtro", async () => {
    listarClases.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await waitFor(() => expect(listarClases).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByText("Todos los niveles"));
    await userEvent.click(await screen.findByText("Avanzado"));

    await waitFor(() =>
      expect(listarClases).toHaveBeenLastCalledWith(expect.objectContaining({ nivel: "AVANZADO", page: 0 })),
    );
  });

  it("al profesor le muestra solo sus clases y no le ofrece programar (CU82)", async () => {
    useAuthStore.setState({ user: { id: 8, email: "ana@tenisahora.com", rol: "PROFESOR" } });
    listarClases.mockResolvedValue({ contenido: [unaClase], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findByRole("heading", { name: "Mis clases" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Nueva clase/ })).not.toBeInTheDocument();
    await waitFor(() => expect(listarClases).toHaveBeenLastCalledWith(expect.objectContaining({ profesorId: "8" })));
  });

  it("al empleado sí le ofrece programar una clase nueva", async () => {
    listarClases.mockResolvedValue({ contenido: [unaClase], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findByRole("button", { name: /Nueva clase/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Clases" })).toBeInTheDocument();
  });

  it("al eliminar una clase avisa y recarga el listado", async () => {
    listarClases.mockResolvedValue({ contenido: [unaClase], totalPaginas: 1, totalElementos: 1 });
    eliminarClase.mockResolvedValueOnce(undefined);

    renderPage();

    await userEvent.click((await screen.findAllByLabelText("Eliminar Tenis inicial"))[0]);
    await userEvent.click(screen.getByRole("button", { name: "Eliminar clase" }));

    await waitFor(() => expect(eliminarClase).toHaveBeenCalledWith(1));
    expect(await screen.findByText(/Se eliminó la clase/)).toBeInTheDocument();
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarClases.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });

  it("sin clases y sin filtros, ofrece programar la primera", async () => {
    listarClases.mockResolvedValueOnce({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();

    expect(await screen.findByText("Todavía no hay clases programadas.")).toBeInTheDocument();
    expect(screen.getByText("Programar la primera")).toBeInTheDocument();
  });

  it("al profesor sin clases le habla de sus clases, no de programar", async () => {
    useAuthStore.setState({ user: { id: 8, email: "ana@tenisahora.com", rol: "PROFESOR" } });
    listarClases.mockResolvedValueOnce({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();

    expect(await screen.findByText("Todavía no tenés clases asignadas.")).toBeInTheDocument();
    expect(screen.queryByText("Programar la primera")).not.toBeInTheDocument();
  });

  it("muestra CertificacionPendienteState si la certificación está pendiente", async () => {
    useAuthStore.setState({ user: { rol: "PROFESOR", id: 10 } });
    listarClases.mockRejectedValueOnce({
      response: { status: 422, data: { codigo: "PROFESOR_NO_VALIDADO" } },
    });
    renderPage();
    expect(await screen.findByText(/Certificación docente en proceso de validación/i)).toBeInTheDocument();
  });
});
