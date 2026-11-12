import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { GrillaDisponibilidad } from "../GrillaDisponibilidad";

describe("GrillaDisponibilidad", () => {
  const canchas = [{ id: 1, identificador: "Cancha 1" }];

  it("muestra Libre cuando no hay franja para esa hora", () => {
    render(<GrillaDisponibilidad canchas={canchas} grillaData={{ 1: { franjas: [] } }} />);
    expect(screen.getByText("Cancha 1")).toBeInTheDocument();
  });

  it("muestra Mant. en la franja bloqueada", () => {
    const grillaData = { 1: { franjas: [{ desde: "10:00", estado: "BLOQUEADA" }] } };
    render(<GrillaDisponibilidad canchas={canchas} grillaData={grillaData} />);
    expect(screen.getByText("Mant.")).toBeInTheDocument();
  });

  it("muestra Reservada en la franja ocupada, y acepta la disponibilidad como array", () => {
    const grillaData = { 1: [{ desde: "11:00", estado: "OCUPADA" }] };
    render(<GrillaDisponibilidad canchas={canchas} grillaData={grillaData} />);
    expect(screen.getByText("Reservada")).toBeInTheDocument();
  });

  it("muestra Clase en la franja ocupada por una clase o entrenamiento", () => {
    const grillaData = { 1: [{ desde: "12:00", estado: "OCUPADA", tipo: "CLASE" }] };
    render(<GrillaDisponibilidad canchas={canchas} grillaData={grillaData} />);
    expect(screen.getByText("Clase")).toBeInTheDocument();
  });

  it("si no hay datos de disponibilidad para la cancha, todo queda Libre", () => {
    render(<GrillaDisponibilidad canchas={canchas} grillaData={{}} />);
    expect(screen.queryByText("Mant.")).not.toBeInTheDocument();
    expect(screen.queryByText("Reservada")).not.toBeInTheDocument();
  });
});
