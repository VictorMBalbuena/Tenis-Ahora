import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EntrenadorDashboard } from "../EntrenadorDashboard";
import { listarMisEntrenamientos } from "@/features/entrenamientos/api/entrenamientosApi";

vi.mock("@/features/entrenamientos/api/entrenamientosApi", () => ({
  listarMisEntrenamientos: vi.fn(),
  listarEntrenamientos: vi.fn(),
}));

const DIAS_EN = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];
const hoyDia = DIAS_EN[new Date().getDay()];

const ENTRENAMIENTO_HOY = {
  id: 10,
  codigo: "A-000010",
  nombre: "Físico Competitivo",
  cancha: "Cancha 3",
  dias: [hoyDia],
  horaInicio: "08:00:00",
  horaFin: "09:30:00",
  cupoMaximo: 12,
  cupoOcupado: 9,
};

const ENTRENAMIENTO_OTRO_DIA = {
  id: 11,
  codigo: "A-000011",
  nombre: "Pretemporada",
  cancha: "Cancha 1",
  dias: [hoyDia === "SUNDAY" ? "MONDAY" : "SUNDAY"],
  horaInicio: "18:00:00",
  horaFin: "19:30:00",
  cupoMaximo: 15,
  cupoOcupado: 11,
};

function renderDashboard(user = { id: 5, email: "entrenador@tenisahora.com", nombre: "Ezequiel", rol: "ENTRENADOR" }) {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <EntrenadorDashboard user={user} />
    </MemoryRouter>
  );
}

describe("EntrenadorDashboard (Inicio del Entrenador - CU84)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el saludo personalizado y la fecha actual", async () => {
    listarMisEntrenamientos.mockResolvedValueOnce({
      contenido: [ENTRENAMIENTO_HOY],
      totalElementos: 1,
    });

    renderDashboard();

    expect(screen.getByText(/Hola, Ezequiel/i)).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: /Hola, Ezequiel/i })).toBeInTheDocument();
  });

  it("muestra las 3 métricas indispensables (clases hoy, alumnos hoy, mis entrenamientos)", async () => {
    listarMisEntrenamientos.mockResolvedValueOnce({
      contenido: [ENTRENAMIENTO_HOY, ENTRENAMIENTO_OTRO_DIA],
      totalElementos: 2,
    });

    renderDashboard();

    expect(await screen.findByText("Clases hoy")).toBeInTheDocument();
    expect(screen.getByText("Alumnos hoy")).toBeInTheDocument();
    expect(screen.getAllByText("Mis entrenamientos").length).toBeGreaterThan(0);

    // 1 clase hoy
    expect(screen.getByText("1")).toBeInTheDocument();
    // 9 alumnos hoy
    expect(screen.getByText("9")).toBeInTheDocument();
    // 2 entrenamientos total
    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("muestra la sesión agendada para hoy con separadores de guión medio y botón h-10 para tomar asistencia", async () => {
    listarMisEntrenamientos.mockResolvedValueOnce({
      contenido: [ENTRENAMIENTO_HOY],
      totalElementos: 1,
    });

    renderDashboard();

    await waitFor(() => {
      expect(screen.getAllByText("Físico Competitivo").length).toBeGreaterThan(0);
    });
    expect(await screen.findByText(/Cancha 3 - 08:00 a 09:30 - 9 alumnos/i)).toBeInTheDocument();

    const enlacesAsistencia = screen.getAllByRole("link", { name: /asistencia/i });
    expect(enlacesAsistencia.some((link) => link.getAttribute("href") === "/entrenamientos/10/asistencia")).toBe(true);
    expect(enlacesAsistencia[0].className).toContain("h-10");
  });

  it("muestra aviso conciso cuando hoy no hay clases programadas", async () => {
    listarMisEntrenamientos.mockResolvedValueOnce({
      contenido: [ENTRENAMIENTO_OTRO_DIA],
      totalElementos: 1,
    });

    renderDashboard();

    expect(
      await screen.findByText(/Hoy no tenés clases programadas/i)
    ).toBeInTheDocument();
  });

  it("muestra empty state dedicado cuando la certificación está pendiente de validación y oculta las métricas", async () => {
    const error422 = {
      response: {
        status: 422,
        data: { codigo: "INSTRUCTOR_NO_VALIDADO" },
      },
    };
    listarMisEntrenamientos.mockRejectedValueOnce(error422);

    renderDashboard();

    expect(
      await screen.findByText(/Certificación deportiva en proceso de validación/i)
    ).toBeInTheDocument();
    expect(screen.queryByText("Clases hoy")).not.toBeInTheDocument();
    expect(screen.queryByText("Hoy no tenés clases programadas.")).not.toBeInTheDocument();
  });

  it("ofrece accesos directos necesarios (Mis entrenamientos, Perfil) y no muestra canchas ni usuarios", async () => {
    listarMisEntrenamientos.mockResolvedValueOnce({
      contenido: [],
      totalElementos: 0,
    });

    renderDashboard();

    expect(
      await screen.findByRole("link", { name: /mis entrenamientos/i })
    ).toHaveAttribute("href", "/entrenamientos");
    expect(
      screen.getByRole("link", { name: /perfil/i })
    ).toHaveAttribute("href", "/perfil");

    // NO debe mostrar canchas ni usuarios
    expect(
      screen.queryByRole("link", { name: /control de canchas/i })
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /directorio de usuarios/i })
    ).not.toBeInTheDocument();
  });
});
