import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { anularCobro } from "../../api/cobrosApi";
import { AnularCobroDialog } from "../AnularCobroDialog";

vi.mock("../../api/cobrosApi", () => ({
  anularCobro: vi.fn(),
}));

const SENIA = {
  id: 1,
  concepto: "SEÑA",
  estado: "PAGADO",
  medioPago: "DEBITO",
  montoFinal: 8000,
  fecha: "2026-10-27T18:30:00",
  reservaId: 12,
};

function renderDialog(cobro = SENIA) {
  const onAnulado = vi.fn();
  const onCerrar = vi.fn();
  render(<AnularCobroDialog abierto cobro={cobro} onCerrar={onCerrar} onAnulado={onAnulado} />);
  return { onAnulado, onCerrar };
}

const botonAnular = () => screen.getByRole("button", { name: /Anular cobro|Anulando/ });

describe("AnularCobroDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sin cobro no muestra nada", () => {
    render(<AnularCobroDialog abierto={false} cobro={null} onCerrar={vi.fn()} onAnulado={vi.fn()} />);

    expect(screen.queryByText("Anular cobro")).not.toBeInTheDocument();
  });

  it.each(["", "    "])("no anula con el motivo vacío o solo con espacios (%j)", async (motivo) => {
    renderDialog();

    if (motivo) await userEvent.type(screen.getByLabelText("Motivo"), motivo);
    await userEvent.click(botonAnular());

    expect(await screen.findByText("Indicá el motivo.")).toBeInTheDocument();
    expect(anularCobro).not.toHaveBeenCalled();
  });

  it("el motivo no admite más de 255 caracteres", () => {
    renderDialog();

    expect(screen.getByLabelText("Motivo")).toHaveAttribute("maxlength", "255");
  });

  it("manda el motivo sin espacios de más y avisa el monto", async () => {
    anularCobro.mockResolvedValueOnce({});
    const { onAnulado } = renderDialog();

    await userEvent.type(screen.getByLabelText("Motivo"), "  Reserva equivocada  ");
    await userEvent.click(botonAnular());

    await waitFor(() => expect(anularCobro).toHaveBeenCalledWith(1, "Reserva equivocada"));
    expect(onAnulado).toHaveBeenCalledWith(expect.stringContaining("Se anuló el cobro"));
  });

  it("si la API lo rechaza, muestra el motivo y deja reintentar", async () => {
    anularCobro.mockRejectedValueOnce({
      response: { status: 422, data: { mensaje: "Hay un saldo pagado: primero hay que anular ese cobro" } },
    });
    const { onAnulado } = renderDialog();

    await userEvent.type(screen.getByLabelText("Motivo"), "Motivo");
    await userEvent.click(botonAnular());

    expect(await screen.findByRole("alert")).toHaveTextContent("Hay un saldo pagado");
    expect(onAnulado).not.toHaveBeenCalled();
    expect(botonAnular()).toBeEnabled();
  });

  it("mientras anula, el botón queda deshabilitado", async () => {
    anularCobro.mockReturnValueOnce(new Promise(() => {}));
    renderDialog();

    await userEvent.type(screen.getByLabelText("Motivo"), "Motivo");
    await userEvent.click(botonAnular());

    expect(await screen.findByRole("button", { name: "Anulando..." })).toBeDisabled();
  });

  it("cancelar no anula", async () => {
    const { onCerrar } = renderDialog();

    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onCerrar).toHaveBeenCalled();
    expect(anularCobro).not.toHaveBeenCalled();
  });

  it("una seña pagada avisa que vuelve a quedar pendiente de pago", () => {
    renderDialog();

    expect(screen.getByText(/vuelve a quedar pendiente de pago/)).toBeInTheDocument();
  });

  it("un arancel de torneo pagado avisa lo mismo", () => {
    renderDialog({ ...SENIA, concepto: "INSCRIPCION" });

    expect(screen.getByText(/vuelve a quedar pendiente de pago/)).toBeInTheDocument();
  });

  it.each([
    ["un saldo pagado", { concepto: "SALDO" }],
    ["un reembolso", { concepto: "REEMBOLSO" }],
    ["un cobro pendiente", { estado: "PENDIENTE" }],
  ])("%s no avisa que vuelve a pendiente, porque no cambia", (_, cambios) => {
    renderDialog({ ...SENIA, ...cambios });

    expect(screen.queryByText(/vuelve a quedar pendiente de pago/)).not.toBeInTheDocument();
  });

  it("un pago con Mercado Pago avisa dónde se devuelve el dinero", () => {
    renderDialog({ ...SENIA, medioPago: "QR" });

    expect(screen.getByText(/panel de Mercado Pago/)).toBeInTheDocument();
  });

  it.each([
    ["con tarjeta", { medioPago: "DEBITO" }],
    ["un QR sin pagar", { medioPago: "QR", estado: "PENDIENTE" }],
  ])("%s no habla de devolver dinero en Mercado Pago", (_, cambios) => {
    renderDialog({ ...SENIA, ...cambios });

    expect(screen.queryByText(/panel de Mercado Pago/)).not.toBeInTheDocument();
  });
});
