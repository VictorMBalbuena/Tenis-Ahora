import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import CobroQR from "../CobroQR";

vi.mock("@/config/axiosClient", () => ({
  default: { post: vi.fn() },
}));

vi.mock("qrcode.react", () => ({
  QRCodeSVG: ({ value }) => <svg data-testid="qr" data-valor={value} />,
}));

const INFO = { subtitulo: "Reserva RES-1", importe: 4500 };

function responderCon({ qr = { qrData: "https://mp/link", venceEn: null }, verificacion = { estado: "PENDIENTE" } } = {}) {
  axiosClient.post.mockImplementation((url) => {
    if (url.endsWith("/qr")) return qr instanceof Error ? Promise.reject(qr) : Promise.resolve({ data: qr });
    return verificacion instanceof Error ? Promise.reject(verificacion) : Promise.resolve({ data: verificacion });
  });
}

function renderQr(props = {}) {
  const onPagoExitoso = vi.fn();
  render(<CobroQR reservaInfo={INFO} cobroId={77} onCancelar={vi.fn()} onPagoExitoso={onPagoExitoso} {...props} />);
  return { onPagoExitoso };
}

async function avanzar(ms) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

describe("CobroQR", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("muestra el QR y el link de pago", async () => {
    responderCon();
    renderQr();

    expect(await screen.findByTestId("qr")).toHaveAttribute("data-valor", "https://mp/link");
    expect(screen.getByRole("link", { name: "Abrir el link de pago" })).toHaveAttribute("href", "https://mp/link");
    expect(axiosClient.post).toHaveBeenCalledWith("/cobros/77/qr");
  });

  it("sin vencimiento informado usa los 10 minutos por defecto", async () => {
    responderCon();
    renderQr();

    await screen.findByTestId("qr");
    expect(screen.getByText("10:00")).toBeInTheDocument();
  });

  it("toma el tiempo restante del vencimiento del servidor", async () => {
    responderCon({ qr: { qrData: "x", venceEn: new Date(Date.now() + 90_000).toISOString() } });
    renderQr();

    await screen.findByTestId("qr");
    expect(screen.getByText(/^01:(29|30)$/)).toBeInTheDocument();
  });

  it("con un vencimiento ya pasado, enseguida queda vencido", async () => {
    responderCon({ qr: { qrData: "x", venceEn: new Date(Date.now() - 60_000).toISOString() } });
    renderQr();

    await screen.findByTestId("qr");
    await avanzar(1000);

    expect(screen.getByText("QR vencido")).toBeInTheDocument();
  });

  it("al llegar a cero avisa, no muestra el QR y permite generar otro (CU77 A1)", async () => {
    responderCon({ qr: { qrData: "x", venceEn: new Date(Date.now() + 2000).toISOString() } });
    renderQr();

    await screen.findByTestId("qr");
    await avanzar(2100);

    expect(screen.getByText("QR vencido")).toBeInTheDocument();
    expect(screen.getByText("00:00")).toBeInTheDocument();
    expect(screen.queryByTestId("qr")).not.toBeInTheDocument();

    responderCon({ qr: { qrData: "nuevo", venceEn: null } });
    await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).click(screen.getByRole("button", { name: "Generar nuevo QR" }));

    expect(await screen.findByTestId("qr")).toHaveAttribute("data-valor", "nuevo");
  });

  it("un pago en el último segundo se confirma aunque el QR ya figure vencido", async () => {
    responderCon({ qr: { qrData: "x", venceEn: new Date(Date.now() + 1000).toISOString() } });
    const { onPagoExitoso } = renderQr();

    await screen.findByTestId("qr");
    await avanzar(1100);
    expect(screen.getByText("QR vencido")).toBeInTheDocument();

    responderCon({ verificacion: { estado: "PAGADO", medioPago: "QR", numeroOperacion: "9", id: 77 } });
    await avanzar(5000);

    await waitFor(() => expect(onPagoExitoso).toHaveBeenCalledWith({ medio: "QR", operacion: "9", reciboId: 77 }));
  });

  it("cuando se acredita el pago, avisa a la pantalla", async () => {
    responderCon({ verificacion: { estado: "PAGADO", medioPago: "QR", numeroOperacion: "9", id: 77 } });
    const { onPagoExitoso } = renderQr();

    await screen.findByTestId("qr");
    await avanzar(5000);

    expect(onPagoExitoso).toHaveBeenCalled();
  });

  it("mientras está pendiente sigue esperando", async () => {
    responderCon();
    const { onPagoExitoso } = renderQr();

    await screen.findByTestId("qr");
    await avanzar(15_000);

    expect(screen.getByTestId("qr")).toBeInTheDocument();
    expect(onPagoExitoso).not.toHaveBeenCalled();
  });

  it("si falla una verificación, no corta la espera", async () => {
    responderCon({ verificacion: new Error("sin conexión") });
    renderQr();

    await screen.findByTestId("qr");
    await avanzar(10_000);

    expect(screen.getByTestId("qr")).toBeInTheDocument();
  });

  it("al socio con el pago rechazado le ofrece reintentar o pagar en el club (CU77 A2)", async () => {
    responderCon({ verificacion: { estado: "RECHAZADO" } });
    renderQr();

    await screen.findByTestId("qr");
    await avanzar(5000);

    expect(screen.getByText("Pago rechazado")).toBeInTheDocument();
    expect(screen.getByText(/pagar en el club/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Generar nuevo QR" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cambiar medio de pago" })).not.toBeInTheDocument();
  });

  it("al personal con el pago rechazado le deja elegir otro medio (CU88 A2)", async () => {
    responderCon({ verificacion: { estado: "RECHAZADO" } });
    const onCambiarMedio = vi.fn();
    renderQr({ onCambiarMedio });

    await screen.findByTestId("qr");
    await avanzar(5000);
    await userEvent.setup({ advanceTimers: vi.advanceTimersByTime }).click(screen.getByRole("button", { name: "Cambiar medio de pago" }));

    expect(onCambiarMedio).toHaveBeenCalledWith("DEBITO");
    expect(screen.queryByText(/pagar en el club/)).not.toBeInTheDocument();
  });

  it("si no se puede generar el QR, muestra el motivo y permite reintentar", async () => {
    const error = Object.assign(new Error("caída"), { response: { status: 503, data: { mensaje: "La pasarela no responde." } } });
    responderCon({ qr: error });
    renderQr();

    expect(await screen.findByText("La pasarela no responde.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Generar nuevo QR" })).toBeInTheDocument();
    expect(screen.queryByTestId("qr")).not.toBeInTheDocument();
  });

  it("con el pago rechazado deja de consultar a Mercado Pago", async () => {
    responderCon({ verificacion: { estado: "RECHAZADO" } });
    renderQr();

    await screen.findByTestId("qr");
    await avanzar(5000);
    const llamadas = axiosClient.post.mock.calls.length;
    await avanzar(20_000);

    expect(axiosClient.post.mock.calls).toHaveLength(llamadas);
  });
});
