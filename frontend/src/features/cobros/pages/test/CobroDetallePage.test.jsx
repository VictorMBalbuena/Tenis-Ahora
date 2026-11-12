import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { anularCobro, emitirRecibo, obtenerCobro } from "../../api/cobrosApi";
import CobroDetallePage from "../CobroDetallePage";

vi.mock("../../api/cobrosApi", () => ({
  obtenerCobro: vi.fn(),
  imprimirCobro: vi.fn(),
  anularCobro: vi.fn(),
  emitirRecibo: vi.fn(),
}));

const unCobro = {
  id: 1,
  concepto: "SALDO",
  estado: "PAGADO",
  medioPago: "QR",
  montoBase: 20000,
  montoFinal: 18000,
  fecha: "2026-10-27T18:30:00",
  reserva: { id: 12, cancha: { identificador: "Cancha 3" }, fecha: "2026-11-02", horaInicio: "18:00:00" },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/cobros/1"]}>
      <Routes>
        <Route path="/cobros/:id" element={<CobroDetallePage />} />
        <Route path="/cobros" element={<p>Listado de cobros</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("CobroDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { rol: "ADMINISTRADOR" } });
  });

  it("muestra el desglose con el monto original, el descuento y el total", async () => {
    obtenerCobro.mockResolvedValueOnce(unCobro);

    renderPage();

    expect(await screen.findByText("Monto original")).toBeInTheDocument();
    expect(screen.getByText("$ 20.000,00")).toBeInTheDocument();
    expect(screen.getByText("- $ 2.000,00")).toBeInTheDocument();
    expect(screen.getAllByText("$ 18.000,00").length).toBeGreaterThan(0);
  });

  it("si el cobro no tiene recibo, ofrece emitirlo", async () => {
    obtenerCobro.mockResolvedValueOnce(unCobro);
    emitirRecibo.mockResolvedValueOnce({ id: 5, numeroRecibo: "REC-0000005", fechaEmision: "2026-10-27T18:40:00" });

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: /Emitir recibo/ }));

    await waitFor(() => expect(emitirRecibo).toHaveBeenCalledWith("1"));
    expect(await screen.findByText(/Se emitió el recibo REC-0000005/)).toBeInTheDocument();
  });

  it("anular exige el motivo", async () => {
    obtenerCobro.mockResolvedValueOnce(unCobro);

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Anular" }));
    await userEvent.click(screen.getByRole("button", { name: "Anular cobro" }));

    expect(await screen.findByText("Indicá el motivo.")).toBeInTheDocument();
    expect(anularCobro).not.toHaveBeenCalled();
  });

  it("al anular manda el motivo y muestra el cobro anulado", async () => {
    obtenerCobro.mockResolvedValueOnce(unCobro).mockResolvedValueOnce({
      ...unCobro,
      estado: "ANULADO",
      motivoAnulacion: "Reserva equivocada",
      fechaAnulacion: "2026-10-28T10:00:00",
    });
    anularCobro.mockResolvedValueOnce({});

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "Anular" }));
    await userEvent.type(screen.getByLabelText("Motivo"), "Reserva equivocada");
    await userEvent.click(screen.getByRole("button", { name: "Anular cobro" }));

    await waitFor(() => expect(anularCobro).toHaveBeenCalledWith(1, "Reserva equivocada"));
    expect(await screen.findByText("Reserva equivocada")).toBeInTheDocument();
    expect(screen.getByText("Anulado")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Anular" })).not.toBeInTheDocument();
  });

  it("el empleado no ve la opción de anular", async () => {
    useAuthStore.setState({ user: { rol: "EMPLEADO" } });
    obtenerCobro.mockResolvedValueOnce(unCobro);

    renderPage();

    await screen.findByText("Datos del cobro");
    expect(screen.queryByRole("button", { name: "Anular" })).not.toBeInTheDocument();
  });

  it("un cobro rechazado no ofrece emitir recibo", async () => {
    obtenerCobro.mockResolvedValueOnce({ ...unCobro, estado: "RECHAZADO" });

    renderPage();

    await screen.findByText("Datos del cobro");
    expect(screen.queryByRole("button", { name: /Emitir recibo/ })).not.toBeInTheDocument();
  });

  it("si no existe el cobro, ofrece volver al listado", async () => {
    obtenerCobro.mockRejectedValueOnce({ response: { status: 404 } });

    renderPage();

    expect(await screen.findByText("No se encontraron los datos del cobro.")).toBeInTheDocument();
  });
});
