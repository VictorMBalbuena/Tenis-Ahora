import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProtectedRoute from "../ProtectedRoute";
import {
  TorneoDetallePage,
  TorneoFixturePage,
  TorneoFormPage,
  TorneosPage,
} from "@/features/torneos";
import { useAuthStore } from "@/store/authStore";

vi.mock("@/features/torneos/api/torneosApi", () => ({
  listarTorneos: vi.fn(() => Promise.resolve([])),
  obtenerTorneo: vi.fn(() => Promise.resolve({ id: 1, nombre: "Torneo Test", estado: "EN_CURSO" })),
  obtenerPartidos: vi.fn(() => Promise.resolve([])),
  listarInscripciones: vi.fn(() => Promise.resolve([])),
}));

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { contenido: [] } })),
  },
}));

function renderConRutas(rutaInicial) {
  return render(
    <MemoryRouter initialEntries={[rutaInicial]}>
      <Routes>
        <Route path="/login" element={<p>Login</p>} />
        <Route path="/dashboard" element={<p>Dashboard</p>} />

        {/* Rutas generales de torneos */}
        <Route element={<ProtectedRoute />}>
          <Route path="/torneos" element={<TorneosPage />} />
          <Route path="/torneos/:id" element={<TorneoDetallePage />} />
          <Route path="/torneos/:id/fixture" element={<TorneoFixturePage />} />
        </Route>

        {/* Rutas restringidas a empleados y admin */}
        <Route element={<ProtectedRoute allowedRoles={["EMPLEADO", "ADMINISTRADOR"]} />}>
          <Route path="/torneos/nuevo" element={<TorneoFormPage />} />
          <Route path="/torneos/editar/:id" element={<TorneoFormPage />} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe("TorneosRoutes", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("permite a cualquier usuario autenticado ver /torneos", async () => {
    useAuthStore.setState({
      token: "access-token",
      inicializado: true,
      user: { email: "socio@tenis.com", rol: "SOCIO" },
    });

    renderConRutas("/torneos");

    expect(await screen.findByRole("heading", { name: "Torneos y ligas" })).toBeInTheDocument();
  });

  it("bloquea a usuarios con rol SOCIO de acceder a /torneos/nuevo y redirige a /dashboard", () => {
    useAuthStore.setState({
      token: "access-token",
      inicializado: true,
      user: { email: "socio@tenis.com", rol: "SOCIO" },
    });

    renderConRutas("/torneos/nuevo");

    expect(screen.getByText("Dashboard")).toBeInTheDocument();
    expect(screen.queryByLabelText(/nombre del torneo/i)).not.toBeInTheDocument();
  });

  it("permite a ADMINISTRADOR acceder a /torneos/nuevo", async () => {
    useAuthStore.setState({
      token: "access-token",
      inicializado: true,
      user: { email: "admin@tenis.com", rol: "ADMINISTRADOR" },
    });

    renderConRutas("/torneos/nuevo");

    expect(await screen.findByLabelText(/nombre del torneo/i)).toBeInTheDocument();
  });
});
