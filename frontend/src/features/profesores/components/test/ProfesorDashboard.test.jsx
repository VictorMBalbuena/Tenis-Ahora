import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ProfesorDashboard } from "../ProfesorDashboard";
import { listarMisClases } from "@/features/clases/api/clasesApi";

vi.mock("@/features/clases/api/clasesApi", () => ({
  listarMisClases: vi.fn(),
  listarClases: vi.fn(),
}));

const DIAS_EN = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const hoyDia = DIAS_EN[new Date().getDay()];

const CLASE_HOY = {
  id: 20,
  codigo: "C-000020",
  nombre: "Clase Adultos Intermedio",
  cancha: "Cancha 2",
  dias: [hoyDia],
  horaInicio: "10:00:00",
  horaFin: "11:30:00",
  cupoMaximo: 8,
  cupoOcupado: 6,
};

function renderDashboard(user = { id: 3, email: "profe@tenisahora.com", nombre: "Carlos", rol: "PROFESOR" }) {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <ProfesorDashboard user={user} />
    </MemoryRouter>
  );
}

describe("ProfesorDashboard (CU81/CU82)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el saludo personalizado y las métricas de hoy", async () => {
    listarMisClases.mockResolvedValueOnce({
      contenido: [CLASE_HOY],
      totalElementos: 1,
    });

    renderDashboard();

    expect(await screen.findByRole("heading", { name: /Hola, Carlos/i })).toBeInTheDocument();
    expect(screen.getByText("Clases hoy")).toBeInTheDocument();
    expect(screen.getByText("Alumnos hoy")).toBeInTheDocument();
    expect(screen.getAllByText("Mis clases").length).toBeGreaterThan(0);
  });

  it("muestra la clase agendada para hoy con botón para tomar asistencia", async () => {
    listarMisClases.mockResolvedValueOnce({
      contenido: [CLASE_HOY],
      totalElementos: 1,
    });

    renderDashboard();

    expect(await screen.findByRole("heading", { name: "Clase Adultos Intermedio" })).toBeInTheDocument();
    const btnAsistencia = screen.getAllByRole("link", { name: /Tomar asistencia/i })[0];
    expect(btnAsistencia).toHaveAttribute("href", "/clases/20/asistencia");
  });

  it("muestra estado de certificación pendiente ante HTTP 422 o PROFESOR_NO_VALIDADO", async () => {
    listarMisClases.mockRejectedValueOnce({
      response: { status: 422, data: { codigo: "PROFESOR_NO_VALIDADO" } },
    });

    renderDashboard();

    expect(await screen.findByText(/Certificación docente en proceso de validación/i)).toBeInTheDocument();
    expect(screen.queryByText("Clases hoy")).not.toBeInTheDocument();
  });
});
