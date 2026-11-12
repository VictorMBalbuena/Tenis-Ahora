import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { emitirRecibo, imprimirCobro, listarCobros } from "../../api/cobrosApi";
import { CobrosPage } from "../CobrosPage";

vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange }) => <input aria-label={name} value={value} onChange={onChange} />,
}));

vi.mock("../../api/cobrosApi", () => ({
  listarCobros: vi.fn(),
  imprimirCobro: vi.fn(),
  anularCobro: vi.fn(),
  emitirRecibo: vi.fn(),
}));

const unCobro = {
  id: 1,
  concepto: "SALDO",
  estado: "PAGADO",
  medioPago: "QR",
  montoFinal: 18000,
  fecha: "2026-10-27T18:30:00",
  reserva: { id: 12, cancha: { identificador: "Cancha 3" }, fecha: "2026-11-02", horaInicio: "18:00:00" },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/cobros"]}>
      <CobrosPage />
    </MemoryRouter>,
  );
}

describe("CobrosPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { rol: "ADMINISTRADOR" } });
  });

  it("lista los cobros que trae el servidor, con su estado", async () => {
    listarCobros.mockResolvedValueOnce({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findAllByText(/Cancha 3/)).not.toHaveLength(0);
    expect(screen.getAllByText("Saldo").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Pagado").length).toBeGreaterThan(0);
  });

  it("los ingresos no suman un cobro rechazado", async () => {
    listarCobros.mockResolvedValueOnce({ contenido: [{ ...unCobro, estado: "RECHAZADO" }], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    await screen.findAllByText(/Cancha 3/);
    expect(screen.getAllByText("$ 0,00").length).toBeGreaterThan(0);
  });

  it("el administrador puede anular un cobro pagado, pero no uno ya rechazado", async () => {
    listarCobros.mockResolvedValueOnce({
      contenido: [unCobro, { ...unCobro, id: 2, estado: "RECHAZADO" }],
      totalPaginas: 1,
      totalElementos: 2,
    });

    renderPage();

    expect((await screen.findAllByLabelText("Anular el cobro #1")).length).toBeGreaterThan(0);
    expect(screen.queryByLabelText("Anular el cobro #2")).not.toBeInTheDocument();
  });

  it("el empleado no ve la opción de anular", async () => {
    useAuthStore.setState({ user: { rol: "EMPLEADO" } });
    listarCobros.mockResolvedValueOnce({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    await screen.findAllByText(/Cancha 3/);
    expect(screen.queryByLabelText("Anular el cobro #1")).not.toBeInTheDocument();
  });

  it("al elegir un medio de pago, pide al backend con ese filtro", async () => {
    listarCobros.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await waitFor(() => expect(listarCobros).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByText("Todos los medios de pago"));
    await userEvent.click(await screen.findByText("Mercado Pago (QR)"));

    await waitFor(() =>
      expect(listarCobros).toHaveBeenLastCalledWith({
        busqueda: "",
        concepto: "",
        medioPago: "QR",
        desde: "",
        hasta: "",
        page: 0,
        size: 20,
      }),
    );
  });

  it("con desde posterior a hasta avisa y no consulta la API", async () => {
    listarCobros.mockResolvedValue({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });

    renderPage();
    await screen.findAllByText(/Cancha 3/);
    fireEvent.change(screen.getByLabelText("hasta"), { target: { value: "2026-11-07" } });
    await waitFor(() => expect(listarCobros).toHaveBeenCalledTimes(2));

    fireEvent.change(screen.getByLabelText("desde"), { target: { value: "2026-11-08" } });

    expect(await screen.findByRole("alert")).toHaveTextContent("no puede ser posterior");
    expect(screen.queryAllByText(/Cancha 3/)).toHaveLength(0);
    expect(listarCobros).toHaveBeenCalledTimes(2);
  });

  it("al corregir el rango vuelve a consultar; el mismo día es válido", async () => {
    listarCobros.mockResolvedValue({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });

    renderPage();
    await screen.findAllByText(/Cancha 3/);
    fireEvent.change(screen.getByLabelText("hasta"), { target: { value: "2026-11-07" } });
    fireEvent.change(screen.getByLabelText("desde"), { target: { value: "2026-11-08" } });
    await screen.findByRole("alert");

    fireEvent.change(screen.getByLabelText("desde"), { target: { value: "2026-11-07" } });

    await waitFor(() => expect(listarCobros).toHaveBeenLastCalledWith(expect.objectContaining({ desde: "2026-11-07", hasta: "2026-11-07" })));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("si la búsqueda tiene otro formato, muestra el mensaje de la API", async () => {
    listarCobros.mockRejectedValueOnce({
      response: { status: 400, data: { mensaje: "Buscá por el código del cobro (C-000123) o el número de reserva (R-000123)" } },
    });

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent("C-000123");
  });

  it("con filtros y sin resultados, avisa y ofrece limpiarlos (CU54 A1)", async () => {
    listarCobros.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();
    await screen.findByText("Todavía no hay cobros registrados.");
    await userEvent.type(screen.getByPlaceholderText("Buscar por C-000123 o R-000123"), "C-1");

    expect(await screen.findByText("Ningún cobro coincide con la búsqueda.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Limpiar filtros" }));
    expect(screen.getByPlaceholderText("Buscar por C-000123 o R-000123")).toHaveValue("");
  });

  it.each(["PENDIENTE", "RECHAZADO", "VENCIDO", "ANULADO"])("un cobro %s no ofrece emitir recibo", async (estado) => {
    listarCobros.mockResolvedValueOnce({ contenido: [{ ...unCobro, estado }], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    await screen.findAllByText(/Cancha 3/);
    expect(screen.queryByLabelText("Emitir el recibo de el cobro #1")).not.toBeInTheDocument();
  });

  it("un cobro pagado con recibo no ofrece emitirlo de nuevo", async () => {
    listarCobros.mockResolvedValueOnce({ contenido: [{ ...unCobro, recibo: { numeroRecibo: "REC-1" } }], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    await screen.findAllByText(/Cancha 3/);
    expect(screen.queryByLabelText("Emitir el recibo de el cobro #1")).not.toBeInTheDocument();
  });

  it("si falla la impresión, informa el error (CU56 A1)", async () => {
    listarCobros.mockResolvedValueOnce({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });
    imprimirCobro.mockRejectedValueOnce({ response: undefined });

    renderPage();
    await userEvent.click((await screen.findAllByLabelText("Imprimir el cobro #1"))[0]);

    expect(await screen.findByRole("alert")).toBeInTheDocument();
  });

  it("emite el recibo de un cobro que no lo tiene y avisa el número", async () => {
    listarCobros.mockResolvedValue({ contenido: [unCobro], totalPaginas: 1, totalElementos: 1 });
    emitirRecibo.mockResolvedValueOnce({ id: 5, numeroRecibo: "REC-0000005" });

    renderPage();

    await userEvent.click((await screen.findAllByLabelText("Emitir el recibo de el cobro #1"))[0]);

    expect(await screen.findByText(/REC-0000005/)).toBeInTheDocument();
    expect(emitirRecibo).toHaveBeenCalledWith(1);
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarCobros.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });

  it("sin cobros y sin filtros, ofrece registrar el primero", async () => {
    listarCobros.mockResolvedValueOnce({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    renderPage();

    expect(await screen.findByText("Todavía no hay cobros registrados.")).toBeInTheDocument();
    expect(screen.getByText("Registrar el primero")).toBeInTheDocument();
  });
});
