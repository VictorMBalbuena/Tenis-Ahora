import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listarProfesores } from "../../api/profesoresApi";
import { ProfesoresPage } from "../ProfesoresPage";

vi.mock("../../api/profesoresApi", () => ({
  listarProfesores: vi.fn(),
  imprimirProfesor: vi.fn(),
  eliminarProfesor: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/profesores"]}>
      <ProfesoresPage />
    </MemoryRouter>
  );
}

describe("ProfesoresPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lista los profesores que trae el servidor", async () => {
    listarProfesores.mockResolvedValueOnce({
      contenido: [{ id: 1, nombre: "Bruno", apellido: "Tealdi", email: "bruno@correo.com", dni: "44966422", activo: true, estadoValidacion: "VALIDADO" }],
      totalPaginas: 1,
      totalElementos: 1,
    });

    renderPage();

    expect(await screen.findAllByText("Bruno Tealdi")).not.toHaveLength(0);
  });

  it("al elegir un estado en el filtro, pide al backend con ese estado", async () => {
    listarProfesores.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await waitFor(() => expect(listarProfesores).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByText("Todas las certificaciones"));
    await userEvent.click(await screen.findByText("Validada"));

    await waitFor(() =>
      expect(listarProfesores).toHaveBeenLastCalledWith({ busqueda: "", estado: "VALIDADO", activo: "", page: 0, size: 20 })
    );
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarProfesores.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });
});
