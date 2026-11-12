import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { PasoResumen } from "../PasoResumen";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi", () => ({
  crearReserva: vi.fn(),
  obtenerReglasDeReserva: vi.fn().mockResolvedValue({ porcentajeSenia: 50 }),
}));

describe("PasoResumen", () => {
  const mockDraft = {
    canchaId: 1,
    canchaNombre: "Cancha 1",
    fecha: "2026-10-25",
    horaInicio: "18:00",
    duracionHoras: 2,
    precioPorHora: 8000,
    modalidad: "SINGLE",
    acompaniantes: [{ nombre: "Carlos", apellido: "Perez", documento: "30111222" }],
    insumos: [{ insumoId: 1, nombre: "Raquetas", cantidad: 2, precioUnitario: 1500 }],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renderiza el desglose con cancha, jugadores e insumos y permite confirmar", async () => {
    const onConfirmar = vi.fn();
    render(<PasoResumen draft={mockDraft} onVolver={vi.fn()} onConfirmar={onConfirmar} />);

    expect(screen.getByText("Cancha 1")).toBeInTheDocument();
    expect(screen.getByText(/Carlos Perez/i)).toBeInTheDocument();
    expect(await screen.findByText(/Seña a pagar \(50%\)/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /confirmar reserva/i }));
    expect(onConfirmar).toHaveBeenCalledWith(mockDraft);
  });

  it("permite volver al paso anterior con onVolver", () => {
    const onVolver = vi.fn();
    render(<PasoResumen draft={mockDraft} onVolver={onVolver} />);

    fireEvent.click(screen.getByRole("button", { name: /volver/i }));
    expect(onVolver).toHaveBeenCalledTimes(1);
  });

  it("muestra desglose financiero correcto con subtotal de cancha, insumos y seña 50%", async () => {
    render(<PasoResumen draft={mockDraft} onVolver={vi.fn()} />);

    // Cancha: 2h * 8000 = 16000. Insumos: 2 * 1500 = 3000. Total = 19000. Seña (50%) = 9500.
    expect(screen.getByText(/19\.000/)).toBeInTheDocument();
    expect(await screen.findByText(/9\.500/)).toBeInTheDocument();
  });

  it("muestra mensaje amigable cuando no hay insumos seleccionados", () => {
    const draftSinInsumos = {
      ...mockDraft,
      insumos: [],
    };
    render(<PasoResumen draft={draftSinInsumos} onVolver={vi.fn()} />);

    expect(screen.getByText(/sin insumos seleccionados/i)).toBeInTheDocument();
  });

  it("calcula y muestra horario respetando horaFin explícita o calculada", () => {
    const draftConHoraFin = {
      ...mockDraft,
      horaInicio: "10:00",
      horaFin: "11:30",
      duracionHoras: 1.5,
    };
    render(<PasoResumen draft={draftConHoraFin} onVolver={vi.fn()} />);

    expect(screen.getByText("10:00 - 11:30")).toBeInTheDocument();
  });

  it("maneja modalidad Dobles y lista múltiples acompañantes", () => {
    const draftDobles = {
      ...mockDraft,
      modalidad: "DOBLES",
      acompaniantes: [
        { nombre: "Carlos", apellido: "Perez", documento: "30111222" },
        { nombre: "Juan", apellido: "Gomez", documento: "35333444" },
        { nombre: "Lucia", apellido: "Diaz", documento: "40555666" },
      ],
    };
    render(<PasoResumen draft={draftDobles} onVolver={vi.fn()} />);

    expect(screen.getByText(/Dobles/i)).toBeInTheDocument();
    expect(screen.getByText(/Carlos Perez/i)).toBeInTheDocument();
    expect(screen.getByText(/Juan Gomez/i)).toBeInTheDocument();
    expect(screen.getByText(/Lucia Diaz/i)).toBeInTheDocument();
  });

  it("si onConfirmar no está definido, llama a crearReserva, deshabilita botón durante envío y ejecuta onConfirmarExitoso", async () => {
    let resolverApi;
    reservasApi.crearReserva.mockReturnValueOnce(
      new Promise((res) => {
        resolverApi = res;
      })
    );
    const onConfirmarExitoso = vi.fn();

    render(
      <PasoResumen
        draft={mockDraft}
        onVolver={vi.fn()}
        onConfirmarExitoso={onConfirmarExitoso}
      />
    );

    const btnConfirmar = screen.getByRole("button", { name: /confirmar reserva/i });
    fireEvent.click(btnConfirmar);

    expect(screen.getByText(/confirmando reserva\.\.\./i)).toBeInTheDocument();
    expect(btnConfirmar).toBeDisabled();

    const mockRespuesta = { id: 101, numero: "R-000101", estado: "PENDIENTE_PAGO" };
    resolverApi(mockRespuesta);

    await waitFor(() => {
      expect(onConfirmarExitoso).toHaveBeenCalledWith(mockRespuesta);
    });
  });

  it("muestra mensaje de error si falla la creación directa en el API", async () => {
    reservasApi.crearReserva.mockRejectedValueOnce(
      new Error("No hay disponibilidad en el horario seleccionado.")
    );

    render(<PasoResumen draft={mockDraft} onVolver={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: /confirmar reserva/i }));

    await waitFor(() => {
      expect(
        screen.getByText("No hay disponibilidad en el horario seleccionado.")
      ).toBeInTheDocument();
    });
  });

  it("extrae el mensaje en español del ErrorResponse en caso de error 400", async () => {
    reservasApi.crearReserva.mockRejectedValueOnce({
      name: "AxiosError",
      message: "Request failed with status code 400",
      response: {
        status: 400,
        data: {
          codigo: "RESERVA_TITULAR_INVALIDO",
          mensaje: "Indicá el socio titular de la reserva",
        },
      },
    });

    render(<PasoResumen draft={mockDraft} onVolver={vi.fn()} />);

    fireEvent.click(screen.getByRole("button", { name: /confirmar reserva/i }));

    await waitFor(() => {
      expect(
        screen.getByText("Indicá el socio titular de la reserva")
      ).toBeInTheDocument();
      expect(
        screen.queryByText("Request failed with status code 400")
      ).not.toBeInTheDocument();
    });
  });

  it("envía titularId en el payload si está definido en el draft (reserva de personal)", async () => {
    reservasApi.crearReserva.mockResolvedValueOnce({ id: 999 });
    const draftConTitular = {
      ...mockDraft,
      titularId: 3,
      titularNombre: "Socio Prueba",
    };

    render(<PasoResumen draft={draftConTitular} onVolver={vi.fn()} />);

    expect(screen.getByText(/Titular: Socio Prueba/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /confirmar reserva/i }));

    await waitFor(() => {
      expect(reservasApi.crearReserva).toHaveBeenCalledWith(
        expect.objectContaining({ titularId: 3 })
      );
    });
  });

  it("renderiza de forma segura si draft está vacío", () => {
    render(<PasoResumen draft={{}} onVolver={vi.fn()} />);
    expect(screen.getByText("Resumen de la Reserva")).toBeInTheDocument();
    expect(screen.getByText(/sin insumos seleccionados/i)).toBeInTheDocument();
  });
});
