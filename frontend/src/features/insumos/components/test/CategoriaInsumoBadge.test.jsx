import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CategoriaInsumoBadge } from "../CategoriaInsumoBadge";

describe("CategoriaInsumoBadge", () => {
  it("muestra el nombre de la categoría", () => {
    render(<CategoriaInsumoBadge categoria={{ id: 1, nombre: "Raqueta" }} />);
    expect(screen.getByText("Raqueta")).toBeInTheDocument();
  });

  it("no renderiza nada sin categoría", () => {
    const { container } = render(<CategoriaInsumoBadge categoria={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
