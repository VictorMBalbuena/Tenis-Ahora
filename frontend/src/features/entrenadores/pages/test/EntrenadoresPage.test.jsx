import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listarEntrenadores } from "../../api/entrenadoresApi";
import { EntrenadoresPage } from "../EntrenadoresPage";

vi.mock("../../api/entrenadoresApi", () => ({
  listarEntrenadores: vi.fn(),
  imprimirEntrenador: vi.fn(),
  eliminarEntrenador: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenadores"]}>
      <EntrenadoresPage />
    </MemoryRouter>
  );
}

describe("EntrenadoresPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lista los entrenadores que trae el servidor", async () => {
    listarEntrenadores.mockResolvedValueOnce({
      contenido: [{ id: 1, nombre: "Lucas", apellido: "Pérez", email: "lucas@correo.com", dni: "31123456", activo: true, estadoValidacion: "VALIDADO" }],
      totalPaginas: 1,
      totalElementos: 1,
    });

    renderPage();

    expect(await screen.findAllByText("Lucas Pérez")).not.toHaveLength(0);
  });

  it("al elegir un estado en el filtro, pide al backend con ese estado", async () => {
    listarEntrenadores.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await waitFor(() => expect(listarEntrenadores).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByText("Todas las certificaciones"));
    await userEvent.click(await screen.findByText("Validada"));

    await waitFor(() =>
      expect(listarEntrenadores).toHaveBeenLastCalledWith({ busqueda: "", especialidad: "", estado: "VALIDADO", activo: "", page: 0, size: 20 })
    );
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarEntrenadores.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });
});
