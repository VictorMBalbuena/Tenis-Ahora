import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import CanchasPage from "../CanchasPage";

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn(), delete: vi.fn() },
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/canchas"]}>
      <CanchasPage />
    </MemoryRouter>
  );
}

const paginaVacia = { contenido: [], totalPaginas: 0, totalElementos: 0 };

function mockConsultasCanchas(respuestaCanchas = paginaVacia) {
  axiosClient.get.mockImplementation((url) =>
    Promise.resolve({ data: url === "/tipos-cancha" ? [] : respuestaCanchas })
  );
}

describe("CanchasPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lista las canchas que trae el servidor", async () => {
    mockConsultasCanchas({
      contenido: [{ id: 1, identificador: "Cancha 1", tipoSuperficie: "PASTO", activa: true }],
      totalPaginas: 1,
      totalElementos: 1,
    });

    renderPage();

    expect(await screen.findAllByText("Cancha 1")).not.toHaveLength(0);
  });

  it("al elegir un estado en el filtro, pide al backend con ese estado", async () => {
    mockConsultasCanchas();

    renderPage();
    await waitFor(() =>
      expect(axiosClient.get).toHaveBeenCalledWith("/canchas", {
        params: { page: 0, size: 20 },
      })
    );

    await userEvent.click(screen.getByText("Todos los estados"));
    await userEvent.click(await screen.findByText("Mantenimiento"));

    await waitFor(() =>
      expect(axiosClient.get).toHaveBeenLastCalledWith("/canchas", {
        params: { page: 0, size: 20, estado: "MANTENIMIENTO" },
      })
    );
  });

  it("si falla la carga, muestra el estado de error con reintentar", async () => {
    axiosClient.get.mockImplementation((url) =>
      url === "/tipos-cancha"
        ? Promise.resolve({ data: [] })
        : Promise.reject(new Error("500"))
    );

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reintentar" })).toBeInTheDocument();
  });
});
