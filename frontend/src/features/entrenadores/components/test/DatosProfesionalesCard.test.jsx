import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { DatosProfesionalesCard } from "../DatosProfesionalesCard";

describe("DatosProfesionalesCard", () => {
  const datos = { certificacionDeportiva: "Entrenador Nacional de Tenis", especialidad: "Alto rendimiento", valorPorHora: "9500" };

  it("muestra los valores actuales", () => {
    render(<DatosProfesionalesCard datos={datos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByDisplayValue("Entrenador Nacional de Tenis")).toBeInTheDocument();
    expect(screen.getByDisplayValue("9500")).toBeInTheDocument();
  });

  it("escribir en especialidad dispara onChange con el campo correcto", async () => {
    const onChange = vi.fn(() => vi.fn());
    render(<DatosProfesionalesCard datos={datos} errores={{}} onChange={onChange} />);

    await userEvent.type(screen.getByDisplayValue("Alto rendimiento"), "!");

    expect(onChange).toHaveBeenCalledWith("especialidad");
  });
});
