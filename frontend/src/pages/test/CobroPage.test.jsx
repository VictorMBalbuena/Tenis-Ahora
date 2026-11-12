import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";
import CobroPage from "../CobroPage";

// Se mockea el cliente HTTP y no `fetch`: la pantalla pasa por axiosClient, que
// es el que adjunta el token y resuelve la dirección del backend.
vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn(), post: vi.fn() },
}));

// El QR necesita un canvas que jsdom no trae; acá solo interesa que la vista cambie.
vi.mock("qrcode.react", () => ({
  QRCodeSVG: ({ value }) => <svg data-testid="qr" data-valor={value} />,
}));

const RESERVA = {
  id: 1041,
  numero: "RES-1041",
  cancha: "Cancha 4 — Cemento",
  fecha: "2026-09-22",
  horaInicio: "19:00:00",
  horaFin: "20:00:00",
  cantidadJugadores: 2,
  montoTotal: 9000,
  montoSenia: 4500,
  estado: "PENDIENTE_PAGO",
};

const COBRO = {
  id: 77,
  reservaId: 1041,
  estado: "PENDIENTE",
  medioPago: "DEBITO",
  montoFinal: 4500,
  numeroOperacion: "000123",
};

function renderCobroPage() {
  return render(
    <MemoryRouter initialEntries={["/reservas/1041/cobro"]}>
      <Routes>
        <Route path="/reservas/:idReserva/cobro" element={<CobroPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("CobroPage (FRONT 07 — CU51, CU86, CU87, CU88)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { rol: "EMPLEADO" } });
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(window, "alert").mockImplementation(() => {});
  });

  it("al montar pide la reserva y muestra la seña a cobrar", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await waitFor(() => expect(axiosClient.get).toHaveBeenCalledWith("/reservas/1041"));
    expect(await screen.findAllByText(/4\.500/)).not.toHaveLength(0);
  });

  it("si la reserva no se puede cargar, lo avisa", async () => {
    axiosClient.get.mockRejectedValueOnce({ response: { status: 404, data: { mensaje: "No existe la reserva." } } });

    renderCobroPage();

    expect(await screen.findByText("No existe la reserva.")).toBeInTheDocument();
  });

  it("con tarjeta, no cobra si falta el número de comprobante", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Debito"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro|Generar QR y cobrar/ }));

    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("registra el cobro contra la reserva, sin mandar el monto (RD09)", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockResolvedValueOnce({ data: COBRO });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Debito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "000123");
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro|Generar QR y cobrar/ }));

    await waitFor(() =>
      expect(axiosClient.post).toHaveBeenCalledWith("/reservas/1041/cobros", {
        medioPago: "DEBITO",
        numeroOperacion: "000123",
        cuotas: null,
        aprobado: true,
      }),
    );
    // El monto lo calcula el servidor: la pantalla no se lo manda.
    expect(axiosClient.post.mock.calls[0][1]).not.toHaveProperty("monto");
  });

  it("si la terminal rechazó la tarjeta, lo avisa y deja elegir otro medio (CU86 A1)", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "RECHAZADO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Debito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "000123");
    await userEvent.click(screen.getByLabelText("La terminal aprobó la operación"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("La tarjeta fue rechazada");
    expect(axiosClient.post.mock.calls[0][1].aprobado).toBe(false);
    expect(screen.getByText("Efectivo")).toBeInTheDocument();
  });

  it("el comprobante descarta lo que no es número y corta en 20 dígitos", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Debito"));
    const campo = screen.getByPlaceholderText("Ej: 000142");
    await userEvent.type(campo, "a1b-2 3");
    expect(campo).toHaveValue("123");

    await userEvent.clear(campo);
    await userEvent.type(campo, "1".repeat(25));
    expect(campo).toHaveValue("1".repeat(20));
  });

  it("al pasar de tarjeta a efectivo no manda el comprobante, las cuotas ni el rechazo cargados", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "EFECTIVO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Credito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "123");
    await userEvent.type(screen.getByLabelText("Cuotas"), "6");
    await userEvent.click(screen.getByLabelText("La terminal aprobó la operación"));
    await userEvent.click(screen.getByText("Efectivo"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    await waitFor(() =>
      expect(axiosClient.post).toHaveBeenCalledWith("/reservas/1041/cobros", {
        medioPago: "EFECTIVO",
        numeroOperacion: null,
        cuotas: null,
        aprobado: true,
      }),
    );
  });

  it.each(["0", "1.5", "25"])("no cobra con %s cuotas", async (cuotas) => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Credito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "1");
    await userEvent.type(screen.getByLabelText("Cuotas"), cuotas);
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("entre 1 y 24");
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it.each(["1", "24"])("cobra con %s cuotas", async (cuotas) => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "CREDITO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Credito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "1");
    await userEvent.type(screen.getByLabelText("Cuotas"), cuotas);
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    await waitFor(() => expect(axiosClient.post.mock.calls[0][1].cuotas).toBe(Number(cuotas)));
  });

  it("tras un rechazo, otro medio de pago cobra sin arrastrar el comprobante", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post
      .mockResolvedValueOnce({ data: { ...COBRO, estado: "RECHAZADO" } })
      .mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "EFECTIVO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Debito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "123");
    await userEvent.click(screen.getByLabelText("La terminal aprobó la operación"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));
    await screen.findByRole("alert");

    expect(screen.getByPlaceholderText("Ej: 000142")).toHaveValue("");
    await userEvent.click(screen.getByText("Efectivo"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    await waitFor(() => expect(axiosClient.post).toHaveBeenCalledTimes(2));
    expect(axiosClient.post.mock.calls[1][1]).toMatchObject({ medioPago: "EFECTIVO", aprobado: true });
  });

  it("si el pago se acreditó con la reserva ya vencida, avisa cuánto hay que devolver", async () => {
    axiosClient.get
      .mockResolvedValueOnce({ data: RESERVA })
      .mockResolvedValueOnce({ data: { ...RESERVA, estado: "CANCELADA", montoReembolso: 4500 } });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "EFECTIVO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Efectivo"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    expect(await screen.findByText(/El club tiene que devolver/)).toBeInTheDocument();
  });

  it("sin saldo pendiente no deja cobrar", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: { ...RESERVA, estado: "CONFIRMADA", saldoPendiente: 0 } });

    renderCobroPage();

    expect(await screen.findByText("No hay saldo pendiente.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Generar QR y cobrar/ })).toBeDisabled();
  });

  it("mientras registra el cobro, el botón queda deshabilitado", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockReturnValueOnce(new Promise(() => {}));

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByRole("button", { name: /Generar QR y cobrar/ }));

    await waitFor(() => expect(screen.getByRole("button", { name: /Generar QR y cobrar/ })).toBeDisabled());
  });

  it("si la pasarela no responde al registrar, avisa sin perder la pantalla", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockRejectedValueOnce({ response: { status: 503, data: { mensaje: "La pasarela no responde." } } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByRole("button", { name: /Generar QR y cobrar/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("La pasarela no responde.");
    expect(screen.getByRole("button", { name: /Generar QR y cobrar/ })).toBeEnabled();
  });

  it("el arancel de un torneo se cobra contra su inscripción (CU52)", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: { torneo: "Copa Otoño", torneoId: 3, montoTotal: 7000 } });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "EFECTIVO" } });

    render(
      <MemoryRouter initialEntries={["/torneos/inscripciones/9/cobro"]}>
        <Routes>
          <Route path="/torneos/inscripciones/:idInscripcion/cobro" element={<CobroPage origen="torneo" />} />
          <Route path="/torneos/3" element={<p>Detalle del torneo</p>} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findAllByText(/7\.000/);
    await userEvent.click(screen.getByText("Efectivo"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    await waitFor(() => expect(axiosClient.post.mock.calls[0][0]).toBe("/torneos/inscripciones/9/cobros"));
    expect(await screen.findByText("Detalle del torneo")).toBeInTheDocument();
  });

  it("si el cobro del torneo falla, no confirma la inscripción (CU52 A1)", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: { torneo: "Copa Otoño", torneoId: 3, montoTotal: 7000 } });
    axiosClient.post.mockRejectedValueOnce({ response: { status: 422, data: { mensaje: "La inscripción no tiene un arancel pendiente de pago" } } });

    render(
      <MemoryRouter initialEntries={["/torneos/inscripciones/9/cobro"]}>
        <Routes>
          <Route path="/torneos/inscripciones/:idInscripcion/cobro" element={<CobroPage origen="torneo" />} />
        </Routes>
      </MemoryRouter>,
    );

    await screen.findAllByText(/7\.000/);
    await userEvent.click(screen.getByText("Efectivo"));
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("arancel pendiente");
  });

  it("con crédito manda las cuotas elegidas (CU87)", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockResolvedValueOnce({ data: { ...COBRO, estado: "PAGADO", medioPago: "CREDITO" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Credito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "000123");
    await userEvent.type(screen.getByLabelText("Cuotas"), "6");
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    await waitFor(() => expect(axiosClient.post.mock.calls[0][1].cuotas).toBe(6));
  });

  it("no cobra con cuotas fuera de 1 a 24", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByText("Tarjeta de Credito"));
    await userEvent.type(screen.getByPlaceholderText("Ej: 000142"), "000123");
    await userEvent.type(screen.getByLabelText("Cuotas"), "25");
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("entre 1 y 24");
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("al socio con el pago rechazado le ofrece reintentar o pagar en el club (CU77 A2)", async () => {
    useAuthStore.setState({ user: { rol: "SOCIO" } });
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post
      .mockResolvedValueOnce({ data: { ...COBRO, medioPago: "QR" } })
      .mockRejectedValueOnce({ response: { status: 422, data: { mensaje: "No se pudo generar." } } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByRole("button", { name: /Generar QR y cobrar/ }));

    expect(await screen.findByText(/pagar en el club/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Generar nuevo QR" })).toBeInTheDocument();
  });

  it("con Mercado Pago registra primero el cobro y recién después pide el QR", async () => {
    axiosClient.get.mockResolvedValue({ data: RESERVA });
    axiosClient.post
      .mockResolvedValueOnce({ data: { ...COBRO, medioPago: "QR" } })
      .mockResolvedValueOnce({ data: { qrData: "00020101021243650016COM.MERCADOLIBRE", estado: "PENDIENTE" } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByRole("button", { name: /Confirmar cobro|Generar QR y cobrar/ }));

    await waitFor(() =>
      expect(axiosClient.post).toHaveBeenNthCalledWith(1, "/reservas/1041/cobros", {
        medioPago: "QR",
        numeroOperacion: null,
        cuotas: null,
        aprobado: true,
      }),
    );
    // El QR cuelga del cobro, no de la reserva.
    await waitFor(() => expect(axiosClient.post).toHaveBeenNthCalledWith(2, "/cobros/77/qr"));
  });

  it("al socio solo le ofrece Mercado Pago", async () => {
    useAuthStore.setState({ user: { rol: "SOCIO" } });
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    expect(screen.queryByText("Tarjeta de Debito")).not.toBeInTheDocument();
    expect(screen.queryByText("Efectivo")).not.toBeInTheDocument();
    expect(screen.getByText("Mercado Pago", { selector: "span" })).toBeInTheDocument();
  });

  it("si el cobro falla, avisa sin perder la pantalla", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: RESERVA });
    axiosClient.post.mockRejectedValueOnce({ response: { status: 409, data: { mensaje: "La reserva ya tiene un cobro registrado" } } });

    renderCobroPage();

    await screen.findAllByText(/4\.500/);
    await userEvent.click(screen.getByRole("button", { name: /Generar QR y cobrar/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent("La reserva ya tiene un cobro registrado");
    expect(screen.getByRole("button", { name: /Generar QR y cobrar/ })).toBeInTheDocument();
  });
});
