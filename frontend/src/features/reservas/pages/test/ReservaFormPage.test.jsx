import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import ReservaFormPage from "../ReservaFormPage";
import { useAuthStore } from "@/store/authStore";
import * as reservasApi from "../../api/reservasApi";
import axiosClient from "@/config/axiosClient";

vi.mock("../../api/reservasApi");
vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

function renderReservaForm(initialPath = "/reservas/nuevo") {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/reservas/nuevo" element={<ReservaFormPage />} />
        <Route path="/reservas/editar/:id" element={<ReservaFormPage />} />
        <Route path="/reservas" element={<div>Listado Reservas</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("ReservaFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      token: "mock-token",
      user: {
        id: 1,
        email: "admin@tenisahora.com",
        rol: "ADMINISTRADOR",
        nombre: "Admin",
        apellido: "General",
        dni: "10000001",
      },
      inicializado: true,
    });
    reservasApi.crearReserva.mockResolvedValue({ id: 1050, codigo: "RES-1050" });
    reservasApi.obtenerReserva.mockResolvedValue({
      id: 1041,
      canchaId: 4,
      fecha: "2026-09-22",
      horaInicio: "19:00",
      horaFin: "20:00",
      modalidad: "SINGLE",
      socioTitular: { nombre: "Joaquín", apellido: "Ferreyra", dni: "41226904" },
      acompaniantes: [{ nombre: "Nicolás", apellido: "Paz", dni: "42880331" }],
      insumos: [{ id: 1, nombre: "Red" }],
    });
  });

  it("renderiza las tarjetas de Turno, Jugadores e Insumos asignados", async () => {
    renderReservaForm();

    expect(await screen.findByRole("heading", { name: /turno/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /jugadores/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /insumos asignados/i })).toBeInTheDocument();
  });

  it("en rol Socio, el titular viene precompletado y bloqueado (readonly)", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: {
        id: 2,
        email: "socio@tenisahora.com",
        rol: "SOCIO",
        nombre: "Carlos",
        apellido: "Alcaraz",
        dni: "44111222",
      },
      inicializado: true,
    });

    renderReservaForm();

    const titularInput = await screen.findByLabelText(/jugador 1 \(titular\)/i);
    const dniInput = screen.getByLabelText(/dni jugador 1/i);

    expect(titularInput).toHaveValue("Carlos Alcaraz");
    expect(titularInput).toHaveAttribute("readonly");
    expect(dniInput).toHaveValue("44111222");
    expect(dniInput).toHaveAttribute("readonly");
  });

  it("en rol Administrador, buscar o ingresar DNI del titular consulta /socios y autocompleta el nombre", async () => {
    axiosClient.get.mockResolvedValueOnce({
      data: [{ id: 99, nombre: "Joaquín", apellido: "Ferreyra", dni: "41226904", activo: true }],
    });

    const user = userEvent.setup();
    renderReservaForm();

    const dniInput = await screen.findByLabelText(/dni jugador 1/i);
    await user.clear(dniInput);
    await user.type(dniInput, "41226904");

    const buscarBtn = screen.queryByRole("button", { name: /verificar|buscar/i });
    if (buscarBtn) {
      await user.click(buscarBtn);
    }

    await waitFor(() => {
      expect(axiosClient.get).toHaveBeenCalledWith(
        expect.stringContaining("/socios"),
        expect.anything()
      );
    });

    await waitFor(() => {
      const titularInput = screen.getByLabelText(/jugador 1 \(titular\)/i);
      expect(titularInput).toHaveValue("Joaquín Ferreyra");
    });
  });

  it("conmuta reactivamente entre modalidad Single (2 jugadores) y Dobles (4 jugadores)", async () => {
    const user = userEvent.setup();
    renderReservaForm();

    expect(screen.getByLabelText(/^jugador 2/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/^jugador 3/i)).not.toBeInTheDocument();

    const doblesRadio = screen.getByLabelText(/4 jugadores/i);
    await user.click(doblesRadio);

    expect(screen.getByLabelText(/^jugador 3/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^jugador 4/i)).toBeInTheDocument();

    const singleRadio = screen.getByLabelText(/2 jugadores/i);
    await user.click(singleRadio);

    expect(screen.queryByLabelText(/^jugador 3/i)).not.toBeInTheDocument();
  });

  it("permite enviar el formulario y navega a /reservas", async () => {
    const user = userEvent.setup();
    renderReservaForm();

    const titularInput = await screen.findByLabelText(/jugador 1 \(titular\)/i);
    await user.type(titularInput, "Juan Perez");

    const dniTitular = screen.getByLabelText(/dni jugador 1/i);
    await user.type(dniTitular, "30111222");

    const jugador2 = screen.getByLabelText(/^jugador 2/i);
    await user.type(jugador2, "Pedro Gomez");

    const dniJugador2 = screen.getByLabelText(/dni jugador 2/i);
    await user.type(dniJugador2, "30333444");

    const guardarBtn = screen.getByRole("button", { name: /guardar/i });
    await user.click(guardarBtn);

    await waitFor(() => {
      expect(reservasApi.crearReserva).toHaveBeenCalled();
    });

    expect(await screen.findByText("Listado Reservas")).toBeInTheDocument();
  }, 30000);
});
