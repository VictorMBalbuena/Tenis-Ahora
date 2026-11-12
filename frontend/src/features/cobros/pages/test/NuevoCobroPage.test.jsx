import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { cobrarSeniaDeReserva, listarReservasConSaldo } from "../../api/cobrosApi";
import NuevoCobroPage from "../NuevoCobroPage";

vi.mock("../../api/cobrosApi", () => ({
  cobrarSeniaDeReserva: vi.fn(),
  listarReservasConSaldo: vi.fn(),
}));

vi.mock("@/pages/CobroQR", () => ({
  default: ({ cobroId }) => <p>QR del cobro {cobroId}</p>,
}));

const reserva = { id: 12, cancha: { identificador: "Cancha 3" }, fecha: "2026-11-02", horaInicio: "18:00:00" };

function renderPage(ruta = "/cobros/nuevo") {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <NuevoCobroPage />
    </MemoryRouter>,
  );
}

describe("NuevoCobroPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    listarReservasConSaldo.mockResolvedValue([reserva]);
  });

  it("no manda el cobro si falta la reserva o el medio de pago", async () => {
    renderPage();

    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText("Elegí la reserva.")).toBeInTheDocument();
    expect(screen.getByText("Elegí el medio de pago.")).toBeInTheDocument();
    expect(cobrarSeniaDeReserva).not.toHaveBeenCalled();
  });

  it("débito pide el número de operación", async () => {
    renderPage();

    await userEvent.click(await screen.findByText("Elegí la reserva"));
    await userEvent.click(await screen.findByText(/Cancha 3/));
    await userEvent.click(screen.getByText("Elegí el medio de pago"));
    await userEvent.click(await screen.findByText("Débito"));
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText("Indicá el número de operación que dio la terminal.")).toBeInTheDocument();
    expect(cobrarSeniaDeReserva).not.toHaveBeenCalled();
  });

  it("con QR no pide el número de operación y muestra el QR para pagar", async () => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1, estado: "PENDIENTE" });
    renderPage();

    await userEvent.click(await screen.findByText("Elegí la reserva"));
    await userEvent.click(await screen.findByText(/Cancha 3/));
    await userEvent.click(screen.getByText("Elegí el medio de pago"));
    await userEvent.click(await screen.findByText("Mercado Pago (QR)"));
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    await waitFor(() => expect(cobrarSeniaDeReserva).toHaveBeenCalled());
    expect(await screen.findByText("QR del cobro 1")).toBeInTheDocument();
  });

  it("si la tarjeta fue rechazada, lo avisa y se queda en el formulario", async () => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1, estado: "RECHAZADO" });

    renderPage();

    await userEvent.click(await screen.findByText("Elegí la reserva"));
    await userEvent.click(await screen.findByText(/Cancha 3/));
    await userEvent.click(screen.getByText("Elegí el medio de pago"));
    await userEvent.click(await screen.findByText("Débito"));
    await userEvent.type(screen.getByLabelText("Número de operación"), "000123");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText(/La tarjeta fue rechazada/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Registrar cobro/ })).toBeInTheDocument();
  });

  it("registra el cobro de la seña de una reserva con débito", async () => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1 });

    renderPage();

    await userEvent.click(await screen.findByText("Elegí la reserva"));
    await userEvent.click(await screen.findByText(/Cancha 3/));
    await userEvent.click(screen.getByText("Elegí el medio de pago"));
    await userEvent.click(await screen.findByText("Débito"));
    await userEvent.type(screen.getByLabelText("Número de operación"), "000123");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    await waitFor(() =>
      expect(cobrarSeniaDeReserva).toHaveBeenCalledWith("12", {
        medioPago: "DEBITO",
        numeroOperacion: "000123",
        cuotas: null,
        aprobado: true,
      }),
    );
  });

  async function elegir(actual, opcion) {
    await userEvent.click((await screen.findAllByText(actual))[0]);
    await userEvent.click((await screen.findAllByText(opcion)).at(-1));
  }

  it("no acepta un número de operación con letras", async () => {
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Débito");
    await userEvent.type(screen.getByLabelText("Número de operación"), "ABC123");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText(/solo números/)).toBeInTheDocument();
    expect(cobrarSeniaDeReserva).not.toHaveBeenCalled();
  });

  it.each(["0", "25", "1.5"])("no acepta %s cuotas", async (cuotas) => {
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Crédito");
    await userEvent.type(screen.getByLabelText("Número de operación"), "1");
    await userEvent.type(screen.getByLabelText("Cuotas"), cuotas);
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText("Las cuotas tienen que estar entre 1 y 24.")).toBeInTheDocument();
    expect(cobrarSeniaDeReserva).not.toHaveBeenCalled();
  });

  it.each(["1", "24"])("acepta %s cuotas", async (cuotas) => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1, estado: "PAGADO" });
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Crédito");
    await userEvent.type(screen.getByLabelText("Número de operación"), "1");
    await userEvent.type(screen.getByLabelText("Cuotas"), cuotas);
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    await waitFor(() => expect(cobrarSeniaDeReserva.mock.calls[0][1].cuotas).toBe(Number(cuotas)));
  });

  it("al pasar de crédito a débito no manda las cuotas que quedaron cargadas", async () => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1, estado: "PAGADO" });
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Crédito");
    await userEvent.type(screen.getByLabelText("Cuotas"), "6");
    await elegir("Crédito", "Débito");
    await userEvent.type(screen.getByLabelText("Número de operación"), "1");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    await waitFor(() => expect(cobrarSeniaDeReserva.mock.calls[0][1].cuotas).toBeNull());
  });

  it("al pasar de débito a efectivo no manda el comprobante ni el rechazo que quedaron cargados", async () => {
    cobrarSeniaDeReserva.mockResolvedValueOnce({ id: 1, estado: "PAGADO" });
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Débito");
    await userEvent.type(screen.getByLabelText("Número de operación"), "abc");
    await userEvent.click(screen.getByLabelText("La terminal aprobó la operación"));
    await elegir("Débito", "Efectivo");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    await waitFor(() =>
      expect(cobrarSeniaDeReserva).toHaveBeenCalledWith("12", { medioPago: "EFECTIVO", numeroOperacion: null, cuotas: null, aprobado: true }),
    );
  });

  it("muestra el error de la API y conserva el formulario", async () => {
    cobrarSeniaDeReserva.mockRejectedValueOnce({ response: { status: 409, data: { mensaje: "La reserva ya tiene un cobro registrado" } } });
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Efectivo");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByText("La reserva ya tiene un cobro registrado")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Registrar cobro/ })).toBeEnabled();
  });

  it("mientras guarda, no se puede volver a enviar", async () => {
    cobrarSeniaDeReserva.mockReturnValueOnce(new Promise(() => {}));
    renderPage();

    await elegir("Elegí la reserva", /Cancha 3/);
    await elegir("Elegí el medio de pago", "Efectivo");
    await userEvent.click(screen.getByRole("button", { name: /Registrar cobro/ }));

    expect(await screen.findByRole("button", { name: "Guardando..." })).toBeDisabled();
  });

  it("sin reservas para cobrar, lo indica", async () => {
    listarReservasConSaldo.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText("No hay reservas con saldo")).toBeInTheDocument();
  });

  it("si no se pueden cargar las reservas, el formulario sigue funcionando", async () => {
    listarReservasConSaldo.mockRejectedValue(new Error("sin conexión"));
    renderPage();

    expect(await screen.findByText("No hay reservas con saldo")).toBeInTheDocument();
  });

  it("si viene la reserva por la URL, arranca con esa reserva elegida", async () => {
    renderPage("/cobros/nuevo?reserva=12");

    expect(await screen.findByText(/Cancha 3/)).toBeInTheDocument();
  });
});
