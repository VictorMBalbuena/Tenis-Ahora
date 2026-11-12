import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { registrarMovimiento } from "../../api/insumosApi";
import { MovimientoStockDialog } from "../MovimientoStockDialog";

vi.mock("../../api/insumosApi", () => ({
  registrarMovimiento: vi.fn(),
}));

describe("MovimientoStockDialog", () => {
  const insumo = { id: 1, nombre: "Raquetas", cantidadDisponible: 10 };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("no renderiza nada sin insumo", () => {
    const { container } = render(<MovimientoStockDialog abierto insumo={null} onCerrar={vi.fn()} onRegistrado={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("por defecto reponer, con cantidad 1 muestra el resultante", () => {
    render(<MovimientoStockDialog abierto insumo={insumo} onCerrar={vi.fn()} onRegistrado={vi.fn()} />);
    expect(screen.getByText("El stock disponible queda en 11.")).toBeInTheDocument();
  });

  it("al elegir Descontar más de lo disponible, avisa y deshabilita Registrar", async () => {
    render(<MovimientoStockDialog abierto insumo={insumo} onCerrar={vi.fn()} onRegistrado={vi.fn()} />);

    await userEvent.click(screen.getByRole("radio", { name: /Descontar/ }));
    await userEvent.clear(screen.getByLabelText("Cantidad"));
    await userEvent.type(screen.getByLabelText("Cantidad"), "20");

    expect(screen.getByText("No se puede: solo hay 10 disponibles.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Registrar movimiento" })).toBeDisabled();
  });

  it("registra el movimiento y avisa con el mensaje según el tipo", async () => {
    registrarMovimiento.mockResolvedValueOnce({});
    const onRegistrado = vi.fn();

    render(<MovimientoStockDialog abierto insumo={insumo} onCerrar={vi.fn()} onRegistrado={onRegistrado} />);
    await userEvent.click(screen.getByRole("button", { name: "Registrar movimiento" }));

    expect(registrarMovimiento).toHaveBeenCalledWith(1, { tipo: "REPOSICION", cantidad: 1, motivo: "" });
    expect(onRegistrado).toHaveBeenCalledWith("Se repusieron 1 unidades de Raquetas.");
  });

  it("si falla el registro, muestra el error y no avisa como registrado", async () => {
    registrarMovimiento.mockRejectedValueOnce(new Error("500"));
    const onRegistrado = vi.fn();

    render(<MovimientoStockDialog abierto insumo={insumo} onCerrar={vi.fn()} onRegistrado={onRegistrado} />);
    await userEvent.click(screen.getByRole("button", { name: "Registrar movimiento" }));

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(onRegistrado).not.toHaveBeenCalled();
  });
});
