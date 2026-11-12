import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BarraStock } from "../BarraStock";

describe("BarraStock", () => {
  it("muestra disponible sobre el total (disponible + en uso)", () => {
    render(<BarraStock insumo={{ cantidadDisponible: 24, enUso: 8 }} />);
    expect(screen.getByText("24")).toBeInTheDocument();
    expect(screen.getByText("/ 32")).toBeInTheDocument();
  });

  it("no rompe cuando el total es cero", () => {
    render(<BarraStock insumo={{ cantidadDisponible: 0, enUso: 0 }} />);
    expect(screen.getByText("0")).toBeInTheDocument();
    expect(screen.getByText("/ 0")).toBeInTheDocument();
  });
});
