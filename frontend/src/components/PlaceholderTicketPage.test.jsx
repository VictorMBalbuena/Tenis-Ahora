import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import PlaceholderTicketPage from "./PlaceholderTicketPage";

describe("PlaceholderTicketPage", () => {
  it("renderiza la información del ticket de Canchas y Disponibilidad (SCRUM-32)", () => {
    render(
      <MemoryRouter initialEntries={["/canchas"]}>
        <PlaceholderTicketPage moduloKey="/canchas" />
      </MemoryRouter>
    );

    expect(screen.getByText("SCRUM-32")).toBeInTheDocument();
    expect(screen.getByText(/FRONT 04 — Pantallas Canchas y Disponibilidad/i)).toBeInTheDocument();
    expect(screen.getByText("In Review")).toBeInTheDocument();
    expect(screen.getByText("feature/TN-F04-Canchas")).toBeInTheDocument();
    expect(screen.getByText("Victor Manuel Balbuena")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /ver ticket scrum-32 en jira/i })).toHaveAttribute(
      "href",
      "https://tenisahora2026.atlassian.net/browse/SCRUM-32"
    );
  });

  it("renderiza la información del ticket de Insumos y Stock (SCRUM-33)", () => {
    render(
      <MemoryRouter initialEntries={["/insumos"]}>
        <PlaceholderTicketPage moduloKey="/insumos" />
      </MemoryRouter>
    );

    expect(screen.getByText("SCRUM-33")).toBeInTheDocument();
    expect(screen.getByText(/FRONT 05 — Pantallas Insumos \/ Stock/i)).toBeInTheDocument();
    expect(screen.getByText("In Review")).toBeInTheDocument();
    expect(screen.getByText("feature/TN-F05-PantallasInsumos")).toBeInTheDocument();
    expect(screen.getByText("Lautaro Aguirre")).toBeInTheDocument();
  });

  it("renderiza la información del ticket de Reservas (SCRUM-34)", () => {
    render(
      <MemoryRouter initialEntries={["/reservas"]}>
        <PlaceholderTicketPage moduloKey="/reservas" />
      </MemoryRouter>
    );

    expect(screen.getByText("SCRUM-34")).toBeInTheDocument();
    expect(screen.getByText(/FRONT 06 — Flujo de Reserva/i)).toBeInTheDocument();
    expect(screen.getByText("Franco Herrera")).toBeInTheDocument();
  });
});
