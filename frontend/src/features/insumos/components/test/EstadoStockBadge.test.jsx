import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EstadoStockBadge } from "../EstadoStockBadge";

describe("EstadoStockBadge", () => {
  it("muestra la etiqueta Disponible cuando hay stock", () => {
    render(<EstadoStockBadge insumo={{ cantidadDisponible: 10, cantidadMinima: 2 }} />);
    expect(screen.getByText("Disponible")).toBeInTheDocument();
  });

  it("muestra Stock bajo cuando está en el mínimo", () => {
    render(<EstadoStockBadge insumo={{ cantidadDisponible: 2, cantidadMinima: 2 }} />);
    expect(screen.getByText("Stock bajo")).toBeInTheDocument();
  });

  it("muestra Sin stock cuando está en cero", () => {
    render(<EstadoStockBadge insumo={{ cantidadDisponible: 0, cantidadMinima: 2 }} />);
    expect(screen.getByText("Sin stock")).toBeInTheDocument();
  });
});
