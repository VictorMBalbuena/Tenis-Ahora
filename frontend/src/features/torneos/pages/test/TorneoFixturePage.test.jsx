import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { TorneoFixturePage } from "../TorneoFixturePage";
import * as torneosApi from "../../api/torneosApi";
import { useAuthStore } from "@/store/authStore";

vi.mock("../../api/torneosApi", () => ({
  obtenerTorneo: vi.fn(),
  obtenerPartidos: vi.fn(),
  descargarPdfResultados: vi.fn(),
  cargarResultado: vi.fn(),
}));

const torneoMock = {
  id: 1,
  nombre: "Torneo Abierto Primavera",
  categoria: "MASCULINO",
  modalidad: "SINGLE",
  tipoSuperficie: "LADRILLO",
  estado: "EN_CURSO",
};

const partidosMock = [
  {
    id: 10,
    torneoId: 1,
    ronda: 1,
    orden: 1,
    participante1: { id: 1, nombre: "Joaquín Ferreyra" },
    participante2: { id: 2, nombre: "Gastón Luna" },
    sets: "6-3 6-4",
    ganadorId: 1,
  },
  {
    id: 11,
    torneoId: 1,
    ronda: 1,
    orden: 2,
    participante1: { id: 3, nombre: "Carlos Gómez" },
    participante2: { id: 4, nombre: "Lucas Paz" },
    sets: null,
    ganadorId: null,
  },
  {
    id: 12,
    torneoId: 1,
    ronda: 2,
    orden: 1,
    participante1: { id: 1, nombre: "Joaquín Ferreyra" },
    participante2: null,
    sets: null,
    ganadorId: null,
  },
];

describe("TorneoFixturePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    torneosApi.obtenerTorneo.mockResolvedValue(torneoMock);
    torneosApi.obtenerPartidos.mockResolvedValue(partidosMock);
    useAuthStore.setState({ user: { email: "admin@tenis.com", rol: "ADMINISTRADOR" } });
  });

  it("renderiza cuadro, pestañas y partidos agrupados", async () => {
    render(
      <MemoryRouter initialEntries={["/torneos/1/fixture"]}>
        <Routes>
          <Route path="/torneos/:id/fixture" element={<TorneoFixturePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("Torneo Abierto Primavera")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /cuadro/i })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /resultados/i })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /reglamento/i })).toBeInTheDocument();

    expect(screen.getAllByText("Joaquín Ferreyra").length).toBe(2);
    expect(screen.getByText("Gastón Luna")).toBeInTheDocument();
    expect(screen.getByText("Semifinal")).toBeInTheDocument();
    expect(screen.getByText("Final")).toBeInTheDocument();
  });

  it("permite descargar el PDF y revoca la URL del blob para liberar memoria (CU50)", async () => {
    const fakeBlob = new Blob(["fake-pdf"], { type: "application/pdf" });
    torneosApi.descargarPdfResultados.mockResolvedValueOnce(fakeBlob);

    const createObjectURLSpy = vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:http://localhost/fake");
    const revokeObjectURLSpy = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});

    render(
      <MemoryRouter initialEntries={["/torneos/1/fixture"]}>
        <Routes>
          <Route path="/torneos/:id/fixture" element={<TorneoFixturePage />} />
        </Routes>
      </MemoryRouter>
    );

    const botonDescargar = await screen.findByRole("button", { name: /imprimir|descargar pdf|resultados/i });
    fireEvent.click(botonDescargar);

    await waitFor(() => {
      expect(torneosApi.descargarPdfResultados).toHaveBeenCalledWith("1");
      expect(createObjectURLSpy).toHaveBeenCalledWith(fakeBlob);
      expect(revokeObjectURLSpy).toHaveBeenCalledWith("blob:http://localhost/fake");
    });

    createObjectURLSpy.mockRestore();
    revokeObjectURLSpy.mockRestore();
  });
});
