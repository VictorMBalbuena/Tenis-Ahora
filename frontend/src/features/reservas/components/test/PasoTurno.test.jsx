import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { PasoTurno } from "../PasoTurno";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi", () => ({
  consultarDisponibilidad: vi.fn(),
  obtenerReglasDeReserva: vi.fn().mockResolvedValue({ antelacionMaximaDias: 30, duracionMaximaHoras: 2 }),
}));

describe("PasoTurno", () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date("2026-10-20T12:00:00Z"));
    reservasApi.consultarDisponibilidad.mockResolvedValue([]);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const mockCanchas = [
    {
      canchaId: 1,
      identificador: "Cancha 1",
      tipoSuperficie: "LADRILLO",
      precioPorHora: 8000,
      franjas: [
        { desde: "18:00:00", hasta: "19:00:00", estado: "LIBRE" },
        { desde: "19:00:00", hasta: "20:00:00", estado: "OCUPADA" },
      ],
    },
  ];

  it("renderiza canchas y permite seleccionar un slot libre", async () => {
    const onSiguiente = vi.fn();
    render(
      <PasoTurno
        canchasDisponibles={mockCanchas}
        valor={{ fecha: "2026-10-25", duracionHoras: 1 }}
        onSiguiente={onSiguiente}
      />
    );

    const slotLibre = screen.getByRole("button", { name: /18:00/i });
    expect(slotLibre).toBeInTheDocument();
    fireEvent.click(slotLibre);

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);
    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({ canchaId: 1, horaInicio: "18:00" })
    );
  });

  it("advierte si se selecciona una fecha fuera de la antelación que informa el servidor", async () => {
    render(<PasoTurno valor={{ fecha: "2099-01-01" }} onSiguiente={vi.fn()} />);
    expect(await screen.findByText(/más de 30 días de anticipación/i)).toBeInTheDocument();
  });

  it("deshabilita el botón continuar si no hay slot seleccionado", () => {
    render(
      <PasoTurno
        canchasDisponibles={mockCanchas}
        valor={{ fecha: "2026-10-25", duracionHoras: 1 }}
        onSiguiente={vi.fn()}
      />
    );

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    expect(btnContinuar).toBeDisabled();
  });

  it("no permite interactuar con slots ocupados o bloqueados", () => {
    const mockConBloqueada = [
      {
        canchaId: 1,
        identificador: "Cancha 1",
        tipoSuperficie: "LADRILLO",
        precioPorHora: 8000,
        franjas: [
          { desde: "19:00:00", hasta: "20:00:00", estado: "OCUPADA" },
          { desde: "20:00:00", hasta: "21:00:00", estado: "BLOQUEADA" },
        ],
      },
    ];

    render(
      <PasoTurno
        canchasDisponibles={mockConBloqueada}
        valor={{ fecha: "2026-10-25" }}
        onSiguiente={vi.fn()}
      />
    );

    const slotOcupado = screen.getByRole("button", { name: /19:00/i });
    expect(slotOcupado).toBeDisabled();

    const slotBloqueado = screen.getByRole("button", { name: /20:00/i });
    expect(slotBloqueado).toBeDisabled();
  });

  it("permite cambiar la duración del turno entre 1 y 2 horas", async () => {
    const onSiguiente = vi.fn();
    const canchasConDosHorasLibres = [
      {
        ...mockCanchas[0],
        franjas: [
          { desde: "18:00:00", hasta: "19:00:00", estado: "LIBRE" },
          { desde: "19:00:00", hasta: "20:00:00", estado: "LIBRE" },
        ],
      },
    ];
    render(
      <PasoTurno
        canchasDisponibles={canchasConDosHorasLibres}
        valor={{ fecha: "2026-10-25", duracionHoras: 1 }}
        onSiguiente={onSiguiente}
      />
    );

    const btn2Horas = await screen.findByRole("button", { name: /2 horas/i });
    fireEvent.click(btn2Horas);

    const slotLibre = screen.getByRole("button", { name: /18:00/i });
    fireEvent.click(slotLibre);

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);

    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({ duracionHoras: 2 })
    );
  });

  it("consulta la disponibilidad por API si no se provee canchasDisponibles", async () => {
    reservasApi.consultarDisponibilidad.mockResolvedValueOnce({
      canchas: mockCanchas,
    });

    render(
      <PasoTurno
        valor={{ fecha: "2026-10-28", duracionHoras: 1 }}
        onSiguiente={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(reservasApi.consultarDisponibilidad).toHaveBeenCalledWith({
        fecha: "2026-10-28",
      });
      expect(screen.getByRole("button", { name: /18:00/i })).toBeInTheDocument();
    });
  });

  it("permite ver una fecha lejana con la advertencia, pero no elegir un turno", async () => {
    render(
      <PasoTurno
        canchasDisponibles={mockCanchas}
        valor={{ fecha: "2099-01-01", duracionHoras: 1 }}
        onSiguiente={vi.fn()}
      />
    );

    expect(await screen.findByText(/más de 30 días de anticipación/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /18:00/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /continuar/i })).toBeDisabled();
  });

  it("utiliza la fecha local como mínimo y no limita el máximo", () => {
    const localEvening = new Date(2026, 9, 20, 22, 30, 0); // 20 de octubre a las 22:30 hora local
    vi.setSystemTime(localEvening);

    render(<PasoTurno valor={{}} onSiguiente={vi.fn()} />);

    const inputFecha = screen.getByLabelText(/fecha de reserva/i);
    expect(inputFecha).toHaveAttribute("data-min", "2026-10-20");
    expect(inputFecha).not.toHaveAttribute("data-max");
  });
});
