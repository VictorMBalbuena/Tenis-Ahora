import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CargarResultadoDialog } from "../CargarResultadoDialog";
import * as torneosApi from "../../api/torneosApi";

vi.mock("../../api/torneosApi", () => ({
  cargarResultado: vi.fn(),
}));

describe("CargarResultadoDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const partidoMock = {
    id: 10,
    torneoId: 1,
    participante1: { id: 101, nombre: "Joaquín Ferreyra" },
    participante2: { id: 102, nombre: "Gastón Luna" },
    sets: null,
  };

  it("renderiza participantes y campos de sets", () => {
    render(
      <CargarResultadoDialog
        open={true}
        partido={partidoMock}
        onClose={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    expect(screen.getByText("Joaquín Ferreyra")).toBeInTheDocument();
    expect(screen.getByText("Gastón Luna")).toBeInTheDocument();
    expect(screen.getAllByRole("spinbutton").length).toBeGreaterThanOrEqual(4);
  });

  it("muestra badge y permite guardar cuando el resultado es válido", async () => {
    const onGuardado = vi.fn();
    torneosApi.cargarResultado.mockResolvedValueOnce({ id: 10, sets: "6-4 6-3" });

    render(
      <CargarResultadoDialog
        open={true}
        partido={partidoMock}
        onClose={vi.fn()}
        onGuardado={onGuardado}
      />
    );

    // Set 1: 6-4
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 1/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 1/i), { target: { value: "4" } });
    // Set 2: 6-3
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 2/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 2/i), { target: { value: "3" } });

    expect(await screen.findByText(/resultado v[aá]lido/i)).toBeInTheDocument();

    const botonGuardar = screen.getByRole("button", { name: /guardar resultado/i });
    expect(botonGuardar).not.toBeDisabled();

    fireEvent.click(botonGuardar);

    await waitFor(() => {
      expect(torneosApi.cargarResultado).toHaveBeenCalledWith(1, 10, "6-4 6-3");
      expect(onGuardado).toHaveBeenCalled();
    });
  });

  it("muestra alerta y deshabilita guardar cuando el tanteador es inválido (RD14)", async () => {
    render(
      <CargarResultadoDialog
        open={true}
        partido={partidoMock}
        onClose={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    // Cargar tanteador inválido: 6-5 6-3
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 1/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 1/i), { target: { value: "5" } });
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 2/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 2/i), { target: { value: "3" } });

    expect(await screen.findByText(/resultado inv[aá]lido/i)).toBeInTheDocument();

    const botonGuardar = screen.getByRole("button", { name: /guardar resultado/i });
    expect(botonGuardar).toBeDisabled();
  });

  it("rechaza sets parcialmente cargados donde uno tiene games y el otro está vacío", () => {
    render(
      <CargarResultadoDialog
        open={true}
        partido={partidoMock}
        onClose={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    // Set 1: 6-4
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 1/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 1/i), { target: { value: "4" } });
    // Set 2: 6-3
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 2/i), { target: { value: "6" } });
    fireEvent.change(screen.getByLabelText(/Games de Gastón Luna en el set 2/i), { target: { value: "3" } });
    // Set 3: 2 - (vacío)
    fireEvent.change(screen.getByLabelText(/Games de Joaquín Ferreyra en el set 3/i), { target: { value: "2" } });

    const botonGuardar = screen.getByRole("button", { name: /guardar resultado/i });
    expect(botonGuardar).toBeDisabled();
  });

  it("soporta nombres desde participanteANombre y participanteBNombre del backend", () => {
    const partidoBackend = {
      id: 50,
      torneoId: 1,
      participanteAId: 1,
      participanteANombre: "Rafael Nadal",
      participanteBId: 2,
      participanteBNombre: "Novak Djokovic",
      sets: null,
    };

    render(
      <CargarResultadoDialog
        open={true}
        partido={partidoBackend}
        onClose={vi.fn()}
        onGuardado={vi.fn()}
      />
    );

    expect(screen.getByText("Rafael Nadal")).toBeInTheDocument();
    expect(screen.getByText("Novak Djokovic")).toBeInTheDocument();
  });
});
