import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import App from "../App";
import {
  ReservasPage,
  ReservaFormPage,
  ReservaDetallePage,
} from "@/features/reservas";
import { useAuthStore } from "@/store/authStore";

// Mock restaurarSesion para evitar efectos de red en montaje de App
vi.mock("@/config/sesion", () => ({
  restaurarSesion: vi.fn(),
}));

// Mock de servicios de la feature reservas
vi.mock("@/features/reservas/api/reservasApi", () => ({
  obtenerMisReservas: vi.fn().mockResolvedValue([]),
  buscarReservas: vi.fn().mockResolvedValue({ contenido: [] }),
  obtenerReserva: vi.fn().mockResolvedValue({ id: 123, codigo: "RES-123" }),
  consultarDisponibilidad: vi.fn().mockResolvedValue([]),
  consultarInsumosDisponibles: vi.fn().mockResolvedValue([]),
  crearReserva: vi.fn().mockResolvedValue({}),
  previsualizarCancelacion: vi.fn().mockResolvedValue({}),
  confirmarCancelacion: vi.fn().mockResolvedValue({}),
  modificarReserva: vi.fn().mockResolvedValue({}),
  obtenerReglasDeReserva: vi.fn().mockResolvedValue(null),
}));

describe("Rutas de la Feature Reservas (FRONT 06 / SCRUM-34 Refactor)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("exporta ReservasPage, ReservaFormPage y ReservaDetallePage desde el punto de entrada de la feature", () => {
    expect(ReservasPage).toBeDefined();
    expect(typeof ReservasPage).toBe("function");
    expect(ReservaFormPage).toBeDefined();
    expect(typeof ReservaFormPage).toBe("function");
    expect(ReservaDetallePage).toBeDefined();
    expect(typeof ReservaDetallePage).toBe("function");
  });

  it("la ruta /reservas resuelve ReservasPage (Mis reservas) para un socio sin placeholder", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas");
    render(<App />);

    expect(await screen.findByRole("heading", { name: /mis reservas/i })).toBeInTheDocument();
    expect(screen.queryByText("SCRUM-34")).not.toBeInTheDocument();
  });

  it("la ruta /reservas resuelve GestionReservasView para un empleado sin placeholder", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 2, email: "empleado@tenisahora.com", rol: "EMPLEADO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas");
    render(<App />);

    expect(await screen.findByRole("heading", { name: /gestión de reservas/i })).toBeInTheDocument();
    expect(screen.queryByText("SCRUM-34")).not.toBeInTheDocument();
  });

  it("permite navegar a la página de detalle /reservas/123", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas/123");
    render(<App />);

    expect(await screen.findByTestId("reserva-detalle-page")).toBeInTheDocument();
  });

  it("/reservas/nuevo abre el wizard de nueva reserva", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas/nuevo");
    render(<App />);

    expect(await screen.findByTestId("nueva-reserva-page")).toBeInTheDocument();
  });

  it("un empleado o administrador puede navegar a la página de formulario edición /reservas/editar/123", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 2, email: "admin@tenisahora.com", rol: "ADMINISTRADOR" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas/editar/123");
    render(<App />);

    expect(await screen.findByTestId("reserva-form-page")).toBeInTheDocument();
  });

  it("un socio NO puede acceder a /reservas/editar/123 y es redirigido", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas/editar/123");
    render(<App />);

    expect(screen.queryByTestId("reserva-form-page")).not.toBeInTheDocument();
  });

  it("la ruta /reservas/nueva redirige a /reservas/nuevo", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    window.history.pushState({}, "", "/reservas/nueva");
    render(<App />);

    expect(await screen.findByTestId("nueva-reserva-page")).toBeInTheDocument();
  });
});
