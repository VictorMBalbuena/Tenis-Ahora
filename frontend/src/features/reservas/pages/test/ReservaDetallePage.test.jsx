import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import ReservaDetallePage from "../ReservaDetallePage";
import { useAuthStore } from "@/store/authStore";
import * as reservasApi from "../../api/reservasApi";

vi.mock("../../api/reservasApi");

const mockReserva = {
  id: 1041,
  codigo: "RES-1041",
  estado: "CONFIRMADA",
  fecha: "2026-09-22",
  horaInicio: "19:00",
  horaFin: "20:00",
  duracionMinutos: 60,
  canchaId: 4,
  canchaNombre: "Cancha 4 - Cemento",
  tipoSuperficie: "Cemento",
  modalidad: "SINGLE",
  precioTotal: 9000,
  montoSenia: 4500,
  saldoPendiente: 4500,
  medioPago: "Mercado Pago (QR)",
  socioTitular: {
    id: 1,
    nombre: "Joaquín",
    apellido: "Ferreyra",
    dni: "41226904",
  },
  acompaniantes: [
    {
      id: 2,
      nombre: "Nicolás",
      apellido: "Paz",
      dni: "42880331",
    },
  ],
  insumos: [
    { id: 1, nombre: "Red", cantidad: 1 },
    { id: 2, nombre: "Pelotas de tenis", cantidad: 1 },
    { id: 3, nombre: "Raquetas", cantidad: 2 },
  ],
};

function renderReservaDetalle(id = "1041") {
  return render(
    <MemoryRouter initialEntries={[`/reservas/${id}`]}>
      <Routes>
        <Route path="/reservas/:id" element={<ReservaDetallePage />} />
        <Route path="/reservas" element={<div>Listado Reservas</div>} />
        <Route path="/reservas/editar/:id" element={<div>Editar Reserva</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("ReservaDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "admin@tenisahora.com", rol: "ADMINISTRADOR" },
      inicializado: true,
    });
    reservasApi.obtenerReserva.mockResolvedValue(mockReserva);
    if (reservasApi.previsualizarCancelacion?.mockResolvedValue) {
      reservasApi.previsualizarCancelacion.mockResolvedValue({
        aTiempo: true,
        horasRestantes: 28,
        penalidadAplicable: 0,
        reembolsoEstimado: 4500,
      });
    }
  });

  it("muestra el código, badge de estado, datos del turno y los jugadores registrados", async () => {
    renderReservaDetalle("1041");

    expect((await screen.findAllByText(/RES-1041/i))[0]).toBeInTheDocument();
    expect(screen.getByText(/Cancha 4 - Cemento/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Joaquín Ferreyra/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Nicolás Paz/i)).toBeInTheDocument();
    expect(screen.getAllByText(/41226904|41\.226\.904/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/42880331|42\.880\.331/i)).toBeInTheDocument();
    expect(screen.getByText(/JF/i)).toBeInTheDocument();
    expect(screen.getByText(/NP/i)).toBeInTheDocument();
    expect(screen.getByText(/Red/i)).toBeInTheDocument();
  });

  it("un usuario Administrador ve los botones de 'Editar' y 'Eliminar'", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 1, email: "admin@tenisahora.com", rol: "ADMINISTRADOR" },
      inicializado: true,
    });

    renderReservaDetalle("1041");

    expect(await screen.findByRole("button", { name: /editar/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /eliminar/i })).toBeInTheDocument();
  });

  it("un usuario Socio NO ve el botón de 'Eliminar'", async () => {
    useAuthStore.setState({
      token: "mock-token",
      user: { id: 2, email: "socio@tenisahora.com", rol: "SOCIO" },
      inicializado: true,
    });

    renderReservaDetalle("1041");

    await screen.findAllByText(/RES-1041/i);
    expect(screen.queryByRole("button", { name: /eliminar/i })).not.toBeInTheDocument();
  });

  it("el botón Volver navega hacia /reservas", async () => {
    const user = userEvent.setup();
    renderReservaDetalle("1041");

    const volverBtn = await screen.findByRole("button", { name: /volver/i });
    await user.click(volverBtn);

    expect(await screen.findByText("Listado Reservas")).toBeInTheDocument();
  });

  it("soporta el contrato canónico del backend con numero, jugadores y montoTotal", async () => {
    reservasApi.obtenerReserva.mockResolvedValueOnce({
      id: 2050,
      numero: "R-002050",
      estado: "CONFIRMADA",
      fecha: "2026-09-22",
      horaInicio: "19:00",
      horaFin: "20:00",
      cancha: "Cancha 2 - Rápida",
      montoTotal: 12000,
      montoSenia: 6000,
      saldoPendiente: 6000,
      modalidad: "SINGLE",
      jugadores: [
        { titular: true, nombre: "Bruno", apellido: "Tealdi", documento: "35111222" },
        { titular: false, nombre: "Nicolás", apellido: "Paz", documento: "42880331" },
      ],
    });

    renderReservaDetalle("2050");

    expect((await screen.findAllByText(/R-002050/i))[0]).toBeInTheDocument();
    expect(screen.getByText(/Cancha 2 - Rápida/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Bruno Tealdi/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/\$ 12\.000/i)).toBeInTheDocument();
  });

  it("al hacer click en 'Eliminar', abre EliminarReservaDialog", async () => {
    const user = userEvent.setup();
    renderReservaDetalle("1041");

    const eliminarBtn = await screen.findByRole("button", { name: /eliminar/i });
    await user.click(eliminarBtn);

    expect(await screen.findByText(/¿Eliminar reserva\?/i)).toBeInTheDocument();
  });

  it("al hacer click en 'Cancelar reserva', abre CancelarReservaDialog", async () => {
    const user = userEvent.setup();
    renderReservaDetalle("1041");

    const cancelarBtn = await screen.findByRole("button", { name: /cancelar reserva/i });
    await user.click(cancelarBtn);

    expect(await screen.findByRole("heading", { name: /cancelar reserva/i })).toBeInTheDocument();
  });
});
