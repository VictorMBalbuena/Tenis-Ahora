import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { HistorialMovimientos } from "../HistorialMovimientos";

describe("HistorialMovimientos", () => {
  it("muestra el mensaje vacío sin movimientos", () => {
    render(<HistorialMovimientos movimientos={[]} />);
    expect(screen.getByText("Todavía no hay movimientos registrados.")).toBeInTheDocument();
  });

  it("muestra cada movimiento con su variación, signada", () => {
    const movimientos = [
      { id: 1, tipo: "REPOSICION", variacion: 10, motivo: "Compra", fecha: "2026-09-22T18:00:00" },
      { id: 2, tipo: "DESCUENTO", variacion: -3, motivo: "", fecha: "2026-09-23T10:00:00" },
    ];
    render(<HistorialMovimientos movimientos={movimientos} />);

    expect(screen.getByText("Reposición")).toBeInTheDocument();
    expect(screen.getByText("+10")).toBeInTheDocument();
    expect(screen.getByText("Compra")).toBeInTheDocument();

    expect(screen.getByText("Descuento")).toBeInTheDocument();
    expect(screen.getByText("-3")).toBeInTheDocument();
    expect(screen.getByText("Sin motivo registrado")).toBeInTheDocument();
  });
});
