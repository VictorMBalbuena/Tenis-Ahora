import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { PasoInsumos } from "../PasoInsumos";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi", () => ({
  consultarInsumosDisponibles: vi.fn(),
}));

describe("PasoInsumos", () => {
  const mockInsumos = [
    { insumoId: 1, nombre: "Raquetas", cantidadAsignable: 2 },
    { insumoId: 2, nombre: "Pelotas", cantidadAsignable: 0 },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("muestra alerta de sin stock y deshabilita incremento si cantidadAsignable es 0", () => {
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    expect(screen.getByText(/sin stock disponible/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/aumentar pelotas/i)).toBeDisabled();
  });

  it("permite sumar unidades respetando el tope asignable", () => {
    const onSiguiente = vi.fn();
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );

    const btnIncrementar = screen.getByLabelText(/aumentar raquetas/i);
    fireEvent.click(btnIncrementar);
    fireEvent.click(btnIncrementar);
    expect(btnIncrementar).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /continuar/i }));
    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({
        insumos: [
          expect.objectContaining({
            insumoId: 1,
            cantidad: 2,
          }),
        ],
      })
    );
  });

  it("permite disminuir unidades y deshabilita el decremento en 0", () => {
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    const btnDisminuir = screen.getByLabelText(/disminuir raquetas/i);
    const btnAumentar = screen.getByLabelText(/aumentar raquetas/i);

    expect(btnDisminuir).toBeDisabled();

    fireEvent.click(btnAumentar);
    expect(btnDisminuir).not.toBeDisabled();

    fireEvent.click(btnDisminuir);
    expect(btnDisminuir).toBeDisabled();
  });

  it("permite continuar sin insumos (insumos es opcional)", () => {
    const onSiguiente = vi.fn();
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /continuar/i }));
    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({
        insumos: [],
      })
    );
  });

  it("ejecuta onVolver al hacer clic en el botón volver", () => {
    const onVolver = vi.fn();
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [] }}
        onVolver={onVolver}
        onSiguiente={vi.fn()}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /volver/i }));
    expect(onVolver).toHaveBeenCalledTimes(1);
  });

  it("inicializa las cantidades a partir de valor.insumos", () => {
    const onSiguiente = vi.fn();
    render(
      <PasoInsumos
        insumos={mockInsumos}
        valor={{ insumos: [{ insumoId: 1, nombre: "Raquetas", cantidad: 1 }] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );

    const btnDisminuir = screen.getByLabelText(/disminuir raquetas/i);
    expect(btnDisminuir).not.toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /continuar/i }));
    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({
        insumos: [
          expect.objectContaining({
            insumoId: 1,
            cantidad: 1,
          }),
        ],
      })
    );
  });

  it("consulta los insumos disponibles por API si no se pasa la prop insumos", async () => {
    reservasApi.consultarInsumosDisponibles.mockResolvedValueOnce(mockInsumos);

    render(
      <PasoInsumos
        turno={{ fecha: "2026-10-25", horaInicio: "18:00", duracionHoras: 2 }}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(reservasApi.consultarInsumosDisponibles).toHaveBeenCalledWith({
        fecha: "2026-10-25",
        horaInicio: "18:00",
        duracionHoras: 2,
      });
      expect(screen.getByText("Raquetas")).toBeInTheDocument();
      expect(screen.getByText(/sin stock disponible/i)).toBeInTheDocument();
    });
  });

  it("muestra el precio unitario del insumo cuando está provisto", () => {
    const insumosConPrecio = [
      { insumoId: 1, nombre: "Tubo de pelotas", cantidadAsignable: 5, precioUnitario: 3500 },
    ];

    render(
      <PasoInsumos
        insumos={insumosConPrecio}
        valor={{ insumos: [] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    expect(screen.getByText(/3\.500/)).toBeInTheDocument();
  });
});
