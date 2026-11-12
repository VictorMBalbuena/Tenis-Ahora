import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { anularRecibo, listarRecibos } from "../../api/recibosApi";
import { RecibosPage } from "../RecibosPage";

vi.mock("../../api/recibosApi", () => ({
  listarRecibos: vi.fn(),
  imprimirRecibo: vi.fn(),
  anularRecibo: vi.fn(),
}));

const unRecibo = {
  id: 9,
  numeroRecibo: "REC-0000009",
  monto: 18000,
  concepto: "Saldo del alquiler",
  fechaEmision: "2026-10-27T18:30:00",
  cobro: { id: 4, concepto: "SALDO", medioPago: "QR" },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/recibos"]}>
      <RecibosPage />
    </MemoryRouter>,
  );
}

describe("RecibosPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lista los recibos que trae el servidor", async () => {
    listarRecibos.mockResolvedValueOnce({ contenido: [unRecibo], totalPaginas: 1, totalElementos: 1 });

    renderPage();

    expect(await screen.findAllByText("REC-0000009")).not.toHaveLength(0);
    expect(screen.getAllByText("Saldo del alquiler").length).toBeGreaterThan(0);
  });

  it("antes de anular, avisa que se invalida el comprobante entregado y pide confirmación", async () => {
    listarRecibos.mockResolvedValue({ contenido: [unRecibo], totalPaginas: 1, totalElementos: 1 });
    anularRecibo.mockResolvedValueOnce(undefined);

    renderPage();

    await userEvent.click((await screen.findAllByLabelText("Anular el recibo REC-0000009"))[0]);

    expect(await screen.findByText(/se invalida el comprobante/)).toBeInTheDocument();
    expect(anularRecibo).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole("button", { name: "Anular recibo" }));

    await waitFor(() => expect(anularRecibo).toHaveBeenCalledWith(9));
    expect(await screen.findByText(/Se anuló el recibo REC-0000009/)).toBeInTheDocument();
  });

  it("un recibo anulado no ofrece editarlo ni volver a anularlo", async () => {
    listarRecibos.mockResolvedValueOnce({
      contenido: [{ ...unRecibo, estado: "ANULADO" }],
      totalPaginas: 1,
      totalElementos: 1,
    });

    renderPage();

    await screen.findAllByText("REC-0000009");
    expect(screen.queryByLabelText("Modificar el recibo REC-0000009")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Anular el recibo REC-0000009")).not.toBeInTheDocument();
    expect(screen.getAllByText("Anulado").length).toBeGreaterThan(0);
  });

  it("si falla la carga, muestra el aviso de error", async () => {
    listarRecibos.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
  });
});
