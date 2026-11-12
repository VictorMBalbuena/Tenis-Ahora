import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import DashboardPage from "../pages/DashboardPage";
import { useAuthStore } from "@/store/authStore";

// Las canchas salen del backend: ya no hay lista escrita a mano en la pantalla.
vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn() },
}));

const CANCHAS = [
  { id: 1, identificador: "Cancha 1", tipoSuperficie: "LADRILLO", activa: true },
  { id: 2, identificador: "Cancha 2", tipoSuperficie: "CEMENTO", activa: true },
];

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <DashboardPage />
    </MemoryRouter>
  );
}

describe("DashboardPage (Linear-clean)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    axiosClient.get.mockResolvedValue({ data: { contenido: CANCHAS, totalElementos: CANCHAS.length } });
  });

  it("muestra el encabezado con logo institucional, saludo de bienvenida, fecha y estado de club abierto", async () => {
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "admin@tenisahora.com", rol: "ADMINISTRADOR" },
    });

    renderDashboard();

    expect(screen.getByText(/Tenis Ahora Club & Sistema Deportivo/i)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText(/Club Abierto • 2 canchas operativas/i)).toBeInTheDocument());
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(/Admin/i);
  });

  it("renderiza las 3 métricas esenciales del club (canchas disponibles, turnos del día y socios)", () => {
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "socio@tenisahora.com", rol: "ADMINISTRADOR" },
    });

    renderDashboard();

    expect(screen.getByText(/Canchas Disponibles/i)).toBeInTheDocument();
    expect(screen.getByText(/Turnos del Día/i)).toBeInTheDocument();
    expect(screen.getByText(/Socios Activos/i)).toBeInTheDocument();
  });

  it("ofrece accesos directos discretos a reservas, canchas y usuarios con accesibilidad", () => {
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" },
    });

    renderDashboard();

    expect(screen.getByRole("link", { name: /ir a gestión de reservas/i })).toHaveAttribute("href", "/reservas");
    expect(screen.getByRole("link", { name: /ir a control de canchas/i })).toHaveAttribute("href", "/canchas");
    expect(screen.getByRole("link", { name: /ir a directorio de usuarios/i })).toHaveAttribute("href", "/usuarios");
  });

  it("al socio le muestra sus reservas y sus accesos, sin datos del club", async () => {
    const PROXIMAS = [
      { id: 1, estado: "CONFIRMADA", fecha: "2026-12-01", horaInicio: "10:00", horaFin: "11:00", cancha: "Cancha 1" },
      { id: 2, estado: "PENDIENTE_PAGO", fecha: "2026-12-02", horaInicio: "12:00", horaFin: "13:00", cancha: "Cancha 2", venceEn: "2099-01-01T18:30:00" },
      { id: 3, estado: "CANCELADA", fecha: "2026-12-03", horaInicio: "14:00", horaFin: "15:00", cancha: "Cancha 3" },
    ];
    const CANCELADAS = [
      { id: 4, estado: "CANCELADA", fecha: "2026-10-01", horaInicio: "10:00", horaFin: "11:00", cancha: "Cancha 4", montoPenalidad: 0, montoReembolso: 0 },
      { id: 5, estado: "CANCELADA", fecha: "2026-10-02", horaInicio: "10:00", horaFin: "11:00", cancha: "Cancha 5", montoPenalidad: 8000, montoReembolso: 0 },
    ];
    axiosClient.get.mockImplementation((url, config) => {
      if (url.includes("mis-inscripciones")) return Promise.resolve({ data: [7] });
      if (config?.params?.estado === "CANCELADA") return Promise.resolve({ data: { contenido: CANCELADAS, totalElementos: 2 } });
      if (config?.params?.estado) return Promise.resolve({ data: { contenido: [], totalElementos: 1 } });
      return Promise.resolve({ data: { contenido: PROXIMAS, totalElementos: 3 } });
    });
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "socio@tenisahora.com", rol: "SOCIO", nombre: "Mica" },
    });

    renderDashboard();

    expect(screen.getByRole("heading", { name: "Hola, Mica" })).toBeInTheDocument();
    expect(await screen.findByRole("timer")).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Pagar seña" })[0]).toHaveAttribute("href", "/reservas/2/cobro");
    expect(screen.queryByText(/Cancha 3/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Socios Activos/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Estado de Canchas/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /usuarios/i })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /Canceladas/ }));

    expect(screen.getByText("Seña perdida")).toBeInTheDocument();
    expect(screen.getAllByText("Sin cargo").length).toBeGreaterThan(1);
  });

  it("muestra las canchas que devuelve el backend", async () => {
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" },
    });

    renderDashboard();

    expect(await screen.findByText("Cancha Cancha 1")).toBeInTheDocument();
    expect(screen.getByText("Cancha Cancha 2")).toBeInTheDocument();
    expect(screen.getByText("LADRILLO")).toBeInTheDocument();
    expect(screen.getByText("CEMENTO")).toBeInTheDocument();
  });

  it("si no se pueden cargar las canchas lo dice, en vez de inventar un listado", async () => {
    axiosClient.get.mockRejectedValue({ response: { status: 403 } });
    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" },
    });

    renderDashboard();

    expect(await screen.findByText("No se pudo cargar el estado de las canchas.")).toBeInTheDocument();
    expect(screen.queryByText("Cancha Central")).not.toBeInTheDocument();
  });

  it("al Entrenador le muestra su propio Inicio con métricas de entrenamientos y no el panel genérico", async () => {
    axiosClient.get.mockResolvedValue({
      data: {
        contenido: [
          {
            id: 1,
            nombre: "Entrenamiento Funcional",
            cancha: "Cancha 1",
            dias: ["MONDAY"],
            horaInicio: "10:00:00",
            horaFin: "11:30:00",
            cupoMaximo: 10,
            cupoOcupado: 6,
          },
        ],
        totalElementos: 1,
      },
    });

    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { id: 8, email: "entrenador@tenisahora.com", nombre: "Federico", rol: "ENTRENADOR" },
    });

    renderDashboard();

    expect(await screen.findByText(/Hola, Federico/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Mis entrenamientos/i).length).toBeGreaterThan(0);
    expect(screen.queryByText("Canchas Disponibles")).not.toBeInTheDocument();
  });

  it("al Profesor le muestra su propio Inicio con métricas de clases y no el panel genérico", async () => {
    axiosClient.get.mockResolvedValue({
      data: {
        contenido: [
          {
            id: 2,
            nombre: "Clase Adultos Avanzado",
            cancha: "Cancha 2",
            dias: ["MONDAY"],
            horaInicio: "10:00:00",
            horaFin: "11:30:00",
            cupoMaximo: 8,
            cupoOcupado: 5,
          },
        ],
        totalElementos: 1,
      },
    });

    useAuthStore.setState({
      token: "access",
      inicializado: true,
      user: { id: 9, email: "profesor@tenisahora.com", nombre: "Carlos", rol: "PROFESOR" },
    });

    renderDashboard();

    expect(await screen.findByText(/Hola, Carlos/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Mis clases/i).length).toBeGreaterThan(0);
    expect(screen.queryByText("Canchas Disponibles")).not.toBeInTheDocument();
  });
});
