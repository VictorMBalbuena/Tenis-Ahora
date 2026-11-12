import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { TorneoDetallePage } from "../TorneoDetallePage";
import * as torneosApi from "../../api/torneosApi";
import { useAuthStore } from "@/store/authStore";

vi.mock("../../api/torneosApi", () => ({
  obtenerTorneo: vi.fn(),
  listarInscripciones: vi.fn(),
  generarFixture: vi.fn(),
  descargarPdfResultados: vi.fn(),
}));

const torneoMock = {
  id: 1,
  nombre: "Torneo Nacional Seniors",
  categoria: "MASCULINO",
  modalidad: "SINGLE",
  tipoSuperficie: "LADRILLO",
  estado: "PLANIFICADO",
  fechaInicio: "2026-11-15",
  fechaFin: "2026-11-20",
  costoInscripcion: 6000,
  canchaId: 1,
};

const inscripcionesMock = [
  { id: 101, jugador1Id: 5, nombreJugador1: "Carlos Alcaraz", parejaId: null },
  { id: 102, jugador1Id: 6, nombreJugador1: "Jannik Sinner", parejaId: null },
];

describe("TorneoDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    torneosApi.obtenerTorneo.mockResolvedValue(torneoMock);
    torneosApi.listarInscripciones.mockResolvedValue(inscripcionesMock);
    useAuthStore.setState({ user: { email: "admin@tenis.com", rol: "ADMINISTRADOR" } });
  });

  it("renderiza datos técnicos y lista de inscriptos", async () => {
    render(
      <MemoryRouter initialEntries={["/torneos/1"]}>
        <Routes>
          <Route path="/torneos/:id" element={<TorneoDetallePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("Torneo Nacional Seniors")).toBeInTheDocument();
    expect(screen.getByText("Carlos Alcaraz")).toBeInTheDocument();
    expect(screen.getByText("Jannik Sinner")).toBeInTheDocument();
    expect(screen.getByText(/inscribirme|inscribir/i)).toBeInTheDocument();
  });

  it("permite a un administrador generar el fixture", async () => {
    torneosApi.generarFixture.mockResolvedValueOnce([{ id: 1 }]);

    render(
      <MemoryRouter initialEntries={["/torneos/1"]}>
        <Routes>
          <Route path="/torneos/:id" element={<TorneoDetallePage />} />
        </Routes>
      </MemoryRouter>
    );

    const botonGenerar = await screen.findByRole("button", { name: /generar cuadro|generar fixture/i });
    fireEvent.click(botonGenerar);

    await waitFor(() => {
      expect(torneosApi.generarFixture).toHaveBeenCalledWith("1");
    });
  });

  it("renderiza identificadorCancha amigable y nombres de inscripciones según DTO backend", async () => {
    torneosApi.obtenerTorneo.mockResolvedValueOnce({
      ...torneoMock,
      canchaId: 1,
      identificadorCancha: "Cancha Central",
    });
    torneosApi.listarInscripciones.mockResolvedValueOnce([
      {
        id: 201,
        jugador1Id: 10,
        jugador1Nombre: "Diego Schwartzman",
        parejaId: 11,
        parejaNombre: "Horacio Zeballos",
      },
    ]);

    render(
      <MemoryRouter initialEntries={["/torneos/1"]}>
        <Routes>
          <Route path="/torneos/:id" element={<TorneoDetallePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("Cancha Central")).toBeInTheDocument();
    expect(screen.getByText(/Diego Schwartzman & Horacio Zeballos/i)).toBeInTheDocument();
  });
});
