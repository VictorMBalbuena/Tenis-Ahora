import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { CaracteristicasCard } from "../CaracteristicasCard";

describe("CaracteristicasCard", () => {
  const formData = {
    iluminacion: true,
    techada: false,
    tribuna: true,
    torneos: false,
    observaciones: "",
  };

  it("muestra los cuatro checkboxes con su estado", () => {
    render(<CaracteristicasCard formData={formData} onChange={vi.fn()} />);
    expect(screen.getByText("Iluminación nocturna")).toBeInTheDocument();
    expect(screen.getByText("Techada")).toBeInTheDocument();
    expect(screen.getByText("Tribuna para público")).toBeInTheDocument();
    expect(screen.getByText("Apta para torneos oficiales")).toBeInTheDocument();
  });

  it("escribir en observaciones dispara onChange", async () => {
    const onChange = vi.fn();
    render(<CaracteristicasCard formData={formData} onChange={onChange} />);

    await userEvent.type(screen.getByPlaceholderText(/Detalles de la superficie/), "x");
    expect(onChange).toHaveBeenCalled();
  });
});
