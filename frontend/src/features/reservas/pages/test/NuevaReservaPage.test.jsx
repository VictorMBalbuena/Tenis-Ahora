import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { NuevaReservaPage } from "../NuevaReservaPage";
import * as reservasApi from "../../api/reservasApi";

const mockedNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

vi.mock("../../api/reservasApi", () => ({
  consultarDisponibilidad: vi.fn(),
  consultarInsumosDisponibles: vi.fn(),
  crearReserva: vi.fn(),
  obtenerReglasDeReserva: vi.fn().mockResolvedValue(null),
  buscarJugadorPorDni: vi.fn(),
}));

describe("NuevaReservaPage", () => {
  const mockDraftCompleto = {
    canchaId: 1,
    canchaNombre: "Cancha Central",
    fecha: "2026-10-25",
    horaInicio: "18:00",
    duracionHoras: 2,
    precioPorHora: 8000,
    modalidad: "SINGLE",
    acompaniantes: [{ nombre: "Juan", apellido: "Perez", documento: "35123456" }],
    insumos: [{ insumoId: 1, nombre: "Tubo de pelotas", cantidad: 1, precioUnitario: 3000 }],
  };

  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date("2026-10-20T12:00:00Z"));
    vi.clearAllMocks();
    reservasApi.consultarDisponibilidad.mockResolvedValue([]);
    reservasApi.consultarInsumosDisponibles.mockResolvedValue([]);
    reservasApi.crearReserva.mockResolvedValue({});
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("renderiza el paso 1 por defecto con el stepper", () => {
    render(
      <MemoryRouter>
        <NuevaReservaPage />
      </MemoryRouter>
    );

    expect(screen.getByRole("button", { name: /Turno$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Jugadores$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Insumos$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Resumen$/i })).toBeInTheDocument();
  });

  it("el botón volver del encabezado redirige a /reservas", () => {
    render(
      <MemoryRouter>
        <NuevaReservaPage />
      </MemoryRouter>
    );

    const btnVolver = screen.getByRole("button", { name: /volver/i });
    fireEvent.click(btnVolver);

    expect(mockedNavigate).toHaveBeenCalledWith("/reservas");
  });

  it("muestra el paso 1 activo con aria-current='step' y los siguientes pendientes deshabilitados", () => {
    render(
      <MemoryRouter>
        <NuevaReservaPage />
      </MemoryRouter>
    );

    const paso1Btn = screen.getByRole("button", { name: /Turno$/i });
    expect(paso1Btn).toHaveAttribute("aria-current", "step");
    expect(paso1Btn).toBeDisabled();

    const paso2Btn = screen.getByRole("button", { name: /Jugadores$/i });
    expect(paso2Btn).toBeDisabled();

    const paso3Btn = screen.getByRole("button", { name: /Insumos$/i });
    expect(paso3Btn).toBeDisabled();

    const paso4Btn = screen.getByRole("button", { name: /Resumen$/i });
    expect(paso4Btn).toBeDisabled();
  });

  it("avanza de paso 1 a paso 2 al seleccionar turno y presionar continuar", async () => {
    const mockCanchas = [
      {
        canchaId: 1,
        identificador: "Cancha 1",
        tipoSuperficie: "LADRILLO",
        precioPorHora: 8000,
        franjas: [{ desde: "18:00:00", hasta: "19:00:00", estado: "LIBRE" }],
      },
    ];
    reservasApi.consultarDisponibilidad.mockResolvedValue(mockCanchas);

    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialDraft={{
            fecha: "2026-10-28",
            duracionHoras: 1,
          }}
        />
      </MemoryRouter>
    );

    // Esperar a que se rendericen los slots
    const slotBtn = await screen.findByRole("button", { name: /18:00/i });
    fireEvent.click(slotBtn);

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    expect(btnContinuar).toBeEnabled();
    fireEvent.click(btnContinuar);

    // Ahora estamos en paso 2
    const paso2Btn = screen.getByRole("button", { name: /Jugadores$/i });
    expect(paso2Btn).toHaveAttribute("aria-current", "step");
    expect(screen.getByRole("heading", { name: "Cantidad de jugadores" })).toBeInTheDocument();
  });

  it("renderiza PasoJugadores cuando initialPaso es 2 y permite retroceder a paso 1 con botón volver", () => {
    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialPaso={2}
          initialDraft={{
            fecha: "2026-10-25",
            canchaId: 1,
            canchaNombre: "Cancha 1",
            horaInicio: "18:00",
            duracionHoras: 1,
            precioPorHora: 8000,
          }}
        />
      </MemoryRouter>
    );

    // Debe estar activo el paso 2
    const paso2Btn = screen.getByRole("button", { name: /Jugadores$/i });
    expect(paso2Btn).toHaveAttribute("aria-current", "step");

    // Paso 1 completado
    const paso1Btn = screen.getByRole("button", { name: /Turno$/i });
    expect(paso1Btn).not.toBeDisabled();

    // Renderiza el contenido de PasoJugadores
    expect(screen.getByRole("heading", { name: "Cantidad de jugadores" })).toBeInTheDocument();

    // Click en botón volver dentro del paso (no el del encabezado)
    const botonesVolver = screen.getAllByRole("button", { name: /volver/i });
    // El segundo botón volver pertenece al footer del paso actual
    fireEvent.click(botonesVolver[1]);

    // Debe volver a paso 1
    const paso1Activo = screen.getByRole("button", { name: /Turno$/i });
    expect(paso1Activo).toHaveAttribute("aria-current", "step");
  });

  it("permite saltar a un paso anterior completado haciendo clic en el stepper", () => {
    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialPaso={3}
          initialDraft={mockDraftCompleto}
        />
      </MemoryRouter>
    );

    // Paso 3 activo
    expect(screen.getByRole("button", { name: /Insumos$/i })).toHaveAttribute("aria-current", "step");

    // Clic en paso 1 en el stepper
    const paso1Btn = screen.getByRole("button", { name: /Turno$/i });
    fireEvent.click(paso1Btn);

    // Ahora paso 1 está activo
    expect(screen.getByRole("button", { name: /Turno$/i })).toHaveAttribute("aria-current", "step");
  });

  it("renderiza PasoInsumos en paso 3 y avanza a paso 4", () => {
    reservasApi.consultarInsumosDisponibles.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialPaso={3}
          initialDraft={mockDraftCompleto}
        />
      </MemoryRouter>
    );

    expect(screen.getByRole("heading", { name: "Insumos a incorporar" })).toBeInTheDocument();

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);

    // Ahora estamos en paso 4 (Resumen)
    const paso4Btn = screen.getByRole("button", { name: /Resumen$/i });
    expect(paso4Btn).toHaveAttribute("aria-current", "step");
    expect(screen.getByText(/Resumen de la Reserva/i)).toBeInTheDocument();
  });

  it("en paso 4 confirma la reserva y despliega el modal de éxito con redirección a /reservas", async () => {
    const respuestaApi = {
      id: 55,
      numero: "R-000055",
      estado: "PENDIENTE_PAGO",
      montoSenia: 9500,
      total: 19000,
    };
    reservasApi.crearReserva.mockResolvedValue(respuestaApi);

    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialPaso={4}
          initialDraft={mockDraftCompleto}
        />
      </MemoryRouter>
    );

    expect(screen.getByText("Cancha Central")).toBeInTheDocument();
    expect(screen.getByText(/Juan Perez/i)).toBeInTheDocument();

    // Confirmar
    const btnConfirmar = screen.getByRole("button", { name: /confirmar reserva/i });
    fireEvent.click(btnConfirmar);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /turno retenido/i })).toBeInTheDocument();
      expect(screen.getByText("R-000055")).toBeInTheDocument();
      expect(screen.getByText(/Falta pagar la seña/i)).toBeInTheDocument();
    });

    // Clic en "Mis Reservas" debe navegar a /reservas
    const btnMisReservas = screen.getByRole("button", { name: /mis reservas/i });
    fireEvent.click(btnMisReservas);

    expect(mockedNavigate).toHaveBeenCalledWith("/reservas");
  });

  it("en modal de éxito permite pagar seña redirigiendo a la pantalla de cobro de la reserva", async () => {
    const respuestaApi = {
      id: 56,
      numero: "R-000056",
      estado: "PENDIENTE_PAGO",
      montoSenia: 9500,
      total: 19000,
    };
    reservasApi.crearReserva.mockResolvedValue(respuestaApi);

    render(
      <MemoryRouter>
        <NuevaReservaPage
          initialPaso={4}
          initialDraft={mockDraftCompleto}
        />
      </MemoryRouter>
    );

    const btnConfirmar = screen.getByRole("button", { name: /confirmar reserva/i });
    fireEvent.click(btnConfirmar);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /turno retenido/i })).toBeInTheDocument();
    });

    const btnPagar = screen.getByRole("button", { name: /pagar seña/i });
    fireEvent.click(btnPagar);

    expect(mockedNavigate).toHaveBeenCalledWith("/reservas/56/cobro");
  });
});
