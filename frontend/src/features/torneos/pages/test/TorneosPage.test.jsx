import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { TorneosPage } from "../TorneosPage";
import * as torneosApi from "../../api/torneosApi";
import { useAuthStore } from "@/store/authStore";

vi.mock("../../api/torneosApi", () => ({
  listarTorneos: vi.fn(),
  eliminarTorneo: vi.fn(),
}));

const torneosMock = [
  {
    id: 1,
    nombre: "Torneo Abierto Primavera",
    categoria: "MASCULINO",
    modalidad: "SINGLE",
    tipoSuperficie: "LADRILLO",
    estado: "PLANIFICADO",
  },
  {
    id: 2,
    nombre: "Torneo Copa Invierno",
    categoria: "FEMENINO",
    modalidad: "PAREJA",
    tipoSuperficie: "PASTO",
    estado: "FINALIZADO",
  },
];

describe("TorneosPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    torneosApi.listarTorneos.mockResolvedValue(torneosMock);
  });

  it("renderiza lista de torneos y permite filtrar por categoría", async () => {
    useAuthStore.setState({ user: { email: "socio@tenis.com", rol: "SOCIO" } });

    render(
      <MemoryRouter>
        <TorneosPage />
      </MemoryRouter>
    );

    expect(await screen.findByText("Torneo Abierto Primavera")).toBeInTheDocument();
    expect(screen.getByText("Torneo Copa Invierno")).toBeInTheDocument();

    // Filtro de categoría
    const selectCat = screen.getByLabelText(/filtrar por categoría|categoría/i);
    fireEvent.change(selectCat, { target: { value: "MASCULINO" } });

    expect(screen.getByText("Torneo Abierto Primavera")).toBeInTheDocument();
    expect(screen.queryByText("Torneo Copa Invierno")).not.toBeInTheDocument();
  });

  it("muestra botón 'Nuevo torneo' solo para roles autorizados (EMPLEADO/ADMINISTRADOR)", async () => {
    // Caso SOCIO
    useAuthStore.setState({ user: { email: "socio@tenis.com", rol: "SOCIO" } });
    const { rerender } = render(
      <MemoryRouter>
        <TorneosPage />
      </MemoryRouter>
    );

    await screen.findByText("Torneo Abierto Primavera");
    expect(screen.queryByRole("link", { name: /nuevo torneo/i })).not.toBeInTheDocument();

    // Caso ADMINISTRADOR
    await act(async () => {
      useAuthStore.setState({ user: { email: "admin@tenis.com", rol: "ADMINISTRADOR" } });
    });
    rerender(
      <MemoryRouter>
        <TorneosPage />
      </MemoryRouter>
    );

    expect(screen.getByRole("link", { name: /nuevo torneo/i })).toBeInTheDocument();
  });
});
