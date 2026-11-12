import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { TorneoFormPage } from "../TorneoFormPage";
import * as torneosApi from "../../api/torneosApi";
import axiosClient from "@/config/axiosClient";

vi.mock("../../api/torneosApi", () => ({
  crearTorneo: vi.fn(),
  modificarTorneo: vi.fn(),
  obtenerTorneo: vi.fn(),
}));

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("TorneoFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    axiosClient.get.mockResolvedValue({
      data: {
        contenido: [
          { id: 1, identificador: "Cancha Central" },
          { id: 2, identificador: "Cancha 2" },
        ],
      },
    });
  });

  it("renderiza campos del formulario y valida fechas invertidas", async () => {
    render(
      <MemoryRouter initialEntries={["/torneos/nuevo"]}>
        <Routes>
          <Route path="/torneos/nuevo" element={<TorneoFormPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByLabelText(/nombre/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/nombre/i), { target: { value: "Torneo Verano" } });
    fireEvent.change(screen.getByLabelText(/fecha de inicio/i), { target: { value: "2026-12-10" } });
    fireEvent.change(screen.getByLabelText(/fecha de fin/i), { target: { value: "2026-12-05" } });

    fireEvent.click(screen.getByRole("button", { name: /guardar|crear torneo/i }));

    expect(await screen.findByText(/la fecha de fin no puede ser anterior/i)).toBeInTheDocument();
    expect(torneosApi.crearTorneo).not.toHaveBeenCalled();
  });

  it("permite dar de alta un torneo con datos válidos", async () => {
    torneosApi.crearTorneo.mockResolvedValueOnce({ id: 10, nombre: "Torneo Primavera" });

    render(
      <MemoryRouter initialEntries={["/torneos/nuevo"]}>
        <Routes>
          <Route path="/torneos/nuevo" element={<TorneoFormPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByLabelText(/nombre/i)).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/nombre/i), { target: { value: "Torneo Primavera" } });
    fireEvent.change(screen.getByLabelText(/categoría/i), { target: { value: "MASCULINO" } });
    fireEvent.change(screen.getByLabelText(/modalidad/i), { target: { value: "SINGLE" } });
    fireEvent.change(screen.getByLabelText(/superficie/i), { target: { value: "LADRILLO" } });
    fireEvent.change(screen.getByLabelText(/fecha de inicio/i), { target: { value: "2026-11-10" } });
    fireEvent.change(screen.getByLabelText(/fecha de fin/i), { target: { value: "2026-11-20" } });
    fireEvent.change(screen.getByLabelText(/costo/i), { target: { value: "5000" } });

    fireEvent.click(screen.getByRole("button", { name: /guardar|crear torneo/i }));

    await waitFor(() => {
      expect(torneosApi.crearTorneo).toHaveBeenCalledWith(
        expect.objectContaining({
          nombre: "Torneo Primavera",
          categoria: "MASCULINO",
          modalidad: "SINGLE",
          tipoSuperficie: "LADRILLO",
          fechaInicio: "2026-11-10",
          fechaFin: "2026-11-20",
          costoInscripcion: 5000,
        })
      );
    });
  });
});
