import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { IdentificacionCard } from "../IdentificacionCard";

describe("IdentificacionCard", () => {
  const formData = { identificador: "Cancha 7", superficie: "LADRILLO", capacidad: 4, precioPorHora: "12000" };

  it("muestra los valores actuales del formulario", () => {
    render(<IdentificacionCard formData={formData} onChange={vi.fn()} />);
    expect(screen.getByDisplayValue("Cancha 7")).toBeInTheDocument();
    expect(screen.getByDisplayValue("12000")).toBeInTheDocument();
  });

  it("escribir en el nombre dispara onChange", async () => {
    const onChange = vi.fn();
    render(<IdentificacionCard formData={formData} onChange={onChange} />);

    await userEvent.type(screen.getByDisplayValue("Cancha 7"), "!");
    expect(onChange).toHaveBeenCalled();
  });
});
