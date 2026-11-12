import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CategoriaField } from "../CategoriaField";

describe("CategoriaField", () => {
  const categorias = [
    { id: 1, nombre: "Raqueta" },
    { id: 2, nombre: "Pelota" },
  ];

  it("muestra la categoría seleccionada", () => {
    render(<CategoriaField categorias={categorias} value="2" onChange={vi.fn()} />);
    expect(screen.getByText("Pelota")).toBeInTheDocument();
  });

  it("muestra el error cuando viene", () => {
    render(<CategoriaField categorias={categorias} value="" onChange={vi.fn()} error="Elegí una categoría." />);
    expect(screen.getByText("Elegí una categoría.")).toBeInTheDocument();
  });
});
