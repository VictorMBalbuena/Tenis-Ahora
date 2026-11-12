import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ActivoBadge, ValidacionBadge } from "../estado-badges";

describe("ValidacionBadge", () => {
  it("muestra Pendiente de validar", () => {
    render(<ValidacionBadge persona={{ estadoValidacion: "PENDIENTE" }} />);
    expect(screen.getByText("Pendiente de validar")).toBeInTheDocument();
  });

  it("muestra Validada cuando la certificación está vigente", () => {
    render(<ValidacionBadge persona={{ estadoValidacion: "VALIDADO", certificacion: { vigente: true } }} />);
    expect(screen.getByText("Validada")).toBeInTheDocument();
  });

  it("muestra Vencida cuando está validado pero la certificación venció", () => {
    render(<ValidacionBadge persona={{ estadoValidacion: "VALIDADO", certificacion: { vigente: false } }} />);
    expect(screen.getByText("Vencida")).toBeInTheDocument();
  });

  it("muestra Rechazada", () => {
    render(<ValidacionBadge persona={{ estadoValidacion: "RECHAZADA" }} />);
    expect(screen.getByText("Rechazada")).toBeInTheDocument();
  });
});

describe("ActivoBadge", () => {
  it("muestra Activo o Inactivo según el estado", () => {
    const { rerender } = render(<ActivoBadge persona={{ activo: true }} />);
    expect(screen.getByText("Activo")).toBeInTheDocument();
    rerender(<ActivoBadge persona={{ activo: false }} />);
    expect(screen.getByText("Inactivo")).toBeInTheDocument();
  });
});
