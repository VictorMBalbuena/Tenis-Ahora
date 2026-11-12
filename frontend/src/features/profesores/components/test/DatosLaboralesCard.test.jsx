import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatosLaboralesCard } from "../DatosLaboralesCard";

describe("DatosLaboralesCard", () => {
  const datos = {
    tituloHabilitante: "Profesor Nacional de Tenis",
    especialidad: "Tenis infantil",
    valorPorHora: "8500",
  };

  it("muestra los valores actuales", () => {
    render(<DatosLaboralesCard datos={datos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByDisplayValue("Profesor Nacional de Tenis")).toBeInTheDocument();
    expect(screen.getByDisplayValue("8500")).toBeInTheDocument();
  });

  it("escribir en especialidad dispara onChange con el campo correcto", async () => {
    const onChange = vi.fn(() => vi.fn());
    render(<DatosLaboralesCard datos={datos} errores={{}} onChange={onChange} />);

    await userEvent.type(screen.getByDisplayValue("Tenis infantil"), "!");

    expect(onChange).toHaveBeenCalledWith("especialidad");
  });
});
