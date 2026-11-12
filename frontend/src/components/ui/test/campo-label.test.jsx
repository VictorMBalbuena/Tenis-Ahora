import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CampoLabel } from "../campo-label";

describe("CampoLabel", () => {
  it("muestra el texto del campo", () => {
    render(<CampoLabel>Capacidad</CampoLabel>);
    expect(screen.getByText("Capacidad")).toBeInTheDocument();
  });

  it("sin info no agrega el ícono de ayuda", () => {
    render(<CampoLabel>Capacidad</CampoLabel>);
    expect(screen.queryByRole("button", { name: "Más información" })).not.toBeInTheDocument();
  });

  it("con info agrega el ícono de ayuda", () => {
    render(<CampoLabel info="Mínimo 2, máximo 4.">Capacidad</CampoLabel>);
    expect(screen.getByRole("button", { name: "Más información" })).toBeInTheDocument();
  });
});
