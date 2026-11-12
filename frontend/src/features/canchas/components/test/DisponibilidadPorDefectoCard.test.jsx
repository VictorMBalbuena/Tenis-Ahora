import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DisponibilidadPorDefectoCard } from "../DisponibilidadPorDefectoCard";

describe("DisponibilidadPorDefectoCard", () => {
  const formData = { apertura: "08:00", cierre: "23:00", duracion: "1", estado: "Disponible" };

  it("muestra los horarios configurados", () => {
    render(<DisponibilidadPorDefectoCard formData={formData} onChange={vi.fn()} />);
    expect(screen.getByText("08:00")).toBeInTheDocument();
    expect(screen.getByText("23:00")).toBeInTheDocument();
  });
});
