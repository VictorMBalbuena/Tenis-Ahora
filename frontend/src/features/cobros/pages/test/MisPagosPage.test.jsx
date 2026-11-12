import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { imprimirCobro, listarMisCobros } from "../../api/cobrosApi";
import { MisPagosPage } from "../MisPagosPage";

vi.mock("../../api/cobrosApi", () => ({
  listarMisCobros: vi.fn(),
  imprimirCobro: vi.fn(),
}));

const unCobro = {
  id: 1,
  reservaId: 12,
  concepto: "SEÑA",
  estado: "PAGADO",
  medioPago: "QR",
  montoFinal: 8000,
  fecha: "2026-10-27T18:30:00",
  recibo: { numeroRecibo: "R-000001" },
};

const pagina = (contenido, extra = {}) => ({ contenido, totalPaginas: 1, totalElementos: contenido.length, ...extra });

describe("MisPagosPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("mientras carga lo indica", () => {
    listarMisCobros.mockReturnValueOnce(new Promise(() => {}));

    render(<MisPagosPage />);

    expect(screen.getByText("Cargando tus pagos...")).toBeInTheDocument();
  });

  it("lista sus pagos con estado, medio y recibo", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([unCobro]));

    render(<MisPagosPage />);

    expect((await screen.findAllByText("Reserva #12")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("Pagado").length).toBeGreaterThan(0);
    expect(screen.getAllByText("R-000001").length).toBeGreaterThan(0);
    expect(listarMisCobros).toHaveBeenCalledWith({ page: 0 });
  });

  it.each([
    ["Pendiente", "PENDIENTE"],
    ["Rechazado", "RECHAZADO"],
    ["Vencido", "VENCIDO"],
    ["Anulado", "ANULADO"],
  ])("muestra un cobro %s con su estado", async (etiqueta, estado) => {
    listarMisCobros.mockResolvedValueOnce(pagina([{ ...unCobro, estado, recibo: null }]));

    render(<MisPagosPage />);

    expect((await screen.findAllByText(etiqueta)).length).toBeGreaterThan(0);
  });

  it("identifica un reembolso con el monto en negativo", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([{ ...unCobro, concepto: "REEMBOLSO" }]));

    render(<MisPagosPage />);

    expect((await screen.findAllByText("Reembolso")).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/^-/).length).toBeGreaterThan(0);
  });

  it("nombra el origen de una inscripción", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([{ ...unCobro, reservaId: null, origen: "Inscripción al torneo Copa Otoño" }]));

    render(<MisPagosPage />);

    expect((await screen.findAllByText("Inscripción al torneo Copa Otoño")).length).toBeGreaterThan(0);
  });

  it("sin pagos, lo avisa", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([]));

    render(<MisPagosPage />);

    expect(await screen.findByText("Todavía no tenés pagos registrados.")).toBeInTheDocument();
  });

  it("si falla la carga, muestra el error y no dice que no hay pagos", async () => {
    listarMisCobros.mockRejectedValueOnce({ response: { status: 500, data: { mensaje: "Falló el servidor." } } });

    render(<MisPagosPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Falló el servidor.");
    expect(screen.queryByText("Todavía no tenés pagos registrados.")).not.toBeInTheDocument();
  });

  it("no ofrece las acciones del personal", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([{ ...unCobro, recibo: null }]));

    render(<MisPagosPage />);

    await screen.findAllByText("Reserva #12");
    expect(screen.queryByLabelText(/Anular/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Emitir/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Ver el detalle/)).not.toBeInTheDocument();
  });

  it("descarga el comprobante", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([unCobro]));
    imprimirCobro.mockResolvedValueOnce(new Blob(["pdf"]));
    const abrir = vi.spyOn(window, "open").mockImplementation(() => null);
    URL.createObjectURL = vi.fn(() => "blob:comprobante");

    render(<MisPagosPage />);
    await userEvent.click((await screen.findAllByLabelText("Imprimir el cobro #1"))[0]);

    await waitFor(() => expect(abrir).toHaveBeenCalledWith("blob:comprobante", "_blank"));
    expect(imprimirCobro).toHaveBeenCalledWith(1);
  });

  it("si falla el comprobante, informa el error y conserva el listado", async () => {
    listarMisCobros.mockResolvedValueOnce(pagina([unCobro]));
    imprimirCobro.mockRejectedValueOnce({ response: undefined });

    render(<MisPagosPage />);
    await userEvent.click((await screen.findAllByLabelText("Imprimir el cobro #1"))[0]);

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(screen.getAllByText("Reserva #12").length).toBeGreaterThan(0);
  });

  it("pagina los resultados", async () => {
    listarMisCobros.mockResolvedValue(pagina([unCobro], { totalPaginas: 2, totalElementos: 25 }));

    render(<MisPagosPage />);
    await userEvent.click(await screen.findByRole("button", { name: "Ir a la página siguiente" }));

    await waitFor(() => expect(listarMisCobros).toHaveBeenLastCalledWith({ page: 1 }));
  });
});
