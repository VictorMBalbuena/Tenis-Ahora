import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { CodigoDeDescuento } from "../CodigoDeDescuento";
import { validarCodigo } from "@/features/descuentos/api/descuentosApi";

vi.mock("@/features/descuentos/api/descuentosApi", () => ({
  validarCodigo: vi.fn(),
}));

/** Un error con la forma de uno de axios, que es lo que lee extraerMensajeError. */
function errorDeApi(mensaje) {
  const error = new Error(mensaje);
  error.response = { status: 404, data: { mensaje } };
  return error;
}

describe("CodigoDeDescuento", () => {
  const onAplicar = vi.fn();
  const onQuitar = vi.fn();

  function montar(props = {}) {
    return render(
      <CodigoDeDescuento montoBase={16000} aplicado={null} onAplicar={onAplicar} onQuitar={onQuitar} {...props} />,
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sin nada escrito no deja validar", () => {
    montar();

    expect(screen.getByRole("button", { name: "Validar" })).toBeDisabled();
  });

  it("pasa a mayúsculas lo que se escribe, para que sea lo mismo que se guardó", async () => {
    const usuario = userEvent.setup();
    montar();

    await usuario.type(screen.getByLabelText(/código de descuento/i), "cyberweek");

    expect(screen.getByLabelText(/código de descuento/i)).toHaveValue("CYBERWEEK");
  });

  it("no deja escribir espacios ni símbolos", async () => {
    const usuario = userEvent.setup();
    montar();

    await usuario.type(screen.getByLabelText(/código de descuento/i), "cyber week!");

    expect(screen.getByLabelText(/código de descuento/i)).toHaveValue("CYBERWEEK");
  });

  it("valida el código contra el monto de la reserva y avisa al padre", async () => {
    const usuario = userEvent.setup();
    const resultado = { codigo: "CYBERWEEK", descripcion: "25% de descuento", montoDescontado: 4000 };
    validarCodigo.mockResolvedValue(resultado);
    montar();

    await usuario.type(screen.getByLabelText(/código de descuento/i), "CYBERWEEK");
    await usuario.click(screen.getByRole("button", { name: "Validar" }));

    await waitFor(() => expect(onAplicar).toHaveBeenCalledWith(resultado));
    expect(validarCodigo).toHaveBeenCalledWith("CYBERWEEK", 16000);
  });

  it("un código que no existe se avisa y no se aplica nada", async () => {
    const usuario = userEvent.setup();
    validarCodigo.mockRejectedValue(errorDeApi("El código de descuento no existe o no está vigente"));
    montar();

    await usuario.type(screen.getByLabelText(/código de descuento/i), "NOEXISTE");
    await usuario.click(screen.getByRole("button", { name: "Validar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/no existe o no está vigente/i);
    expect(onAplicar).not.toHaveBeenCalled();
  });

  it("con un descuento aplicado muestra cuál es y cuánto descuenta", () => {
    montar({ aplicado: { codigo: "CYBERWEEK", descripcion: "25% de descuento", montoDescontado: 4000 } });

    expect(screen.getByText("CYBERWEEK")).toBeInTheDocument();
    expect(screen.getByText(/25% de descuento/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Validar" })).not.toBeInTheDocument();
  });

  it("se puede quitar el descuento aplicado", async () => {
    const usuario = userEvent.setup();
    montar({ aplicado: { codigo: "CYBERWEEK", descripcion: "25% de descuento", montoDescontado: 4000 } });

    await usuario.click(screen.getByRole("button", { name: /quitar/i }));

    expect(onQuitar).toHaveBeenCalled();
  });

  it("mientras se envía la reserva no se puede tocar el código", () => {
    montar({ deshabilitado: true });

    expect(screen.getByLabelText(/código de descuento/i)).toBeDisabled();
  });
});
