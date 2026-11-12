import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { FixtureTree } from "../FixtureTree";

describe("FixtureTree", () => {
  const partidosMock = [
    {
      id: 1,
      ronda: 1,
      orden: 1,
      participante1: { id: 10, nombre: "Roger Federer" },
      participante2: { id: 20, nombre: "Rafael Nadal" },
      sets: "6-4 6-3",
      ganadorId: 10,
    },
    {
      id: 2,
      ronda: 1,
      orden: 2,
      participante1: { id: 30, nombre: "Novak Djokovic" },
      participante2: { id: 40, nombre: "Carlos Alcaraz" },
      sets: null,
      ganadorId: null,
    },
    {
      id: 3,
      ronda: 2,
      orden: 1,
      participante1: { id: 10, nombre: "Roger Federer" },
      participante2: null,
      sets: null,
      ganadorId: null,
    },
  ];

  it("calcula dinámicamente etiquetas de rondas (Semifinal, Final)", () => {
    render(
      <FixtureTree
        partidos={partidosMock}
        esAdmin={false}
        onCargarResultado={vi.fn()}
      />
    );

    expect(screen.getByText("Semifinal")).toBeInTheDocument();
    expect(screen.getByText("Final")).toBeInTheDocument();
  });

  it("muestra nombres de participantes, 'Por definir' y sets", () => {
    render(
      <FixtureTree
        partidos={partidosMock}
        esAdmin={false}
        onCargarResultado={vi.fn()}
      />
    );

    expect(screen.getAllByText("Roger Federer").length).toBe(2);
    expect(screen.getByText("Rafael Nadal")).toBeInTheDocument();
    expect(screen.getByText("6-4 6-3")).toBeInTheDocument();
    expect(screen.getByText("Por definir")).toBeInTheDocument();
  });

  it("muestra botón 'Cargar resultado' únicamente para admin en partidos pendientes con ambos participantes", () => {
    const onCargar = vi.fn();

    const { rerender } = render(
      <FixtureTree
        partidos={partidosMock}
        esAdmin={false}
        onCargarResultado={onCargar}
      />
    );

    // No admin: no debe ver el botón
    expect(screen.queryByRole("button", { name: /cargar resultado/i })).not.toBeInTheDocument();

    // Admin: ve el botón solo en el partido 2 (pendiente y con 2 contrincantes)
    rerender(
      <FixtureTree
        partidos={partidosMock}
        esAdmin={true}
        onCargarResultado={onCargar}
      />
    );

    const botonCargar = screen.getByRole("button", { name: /cargar resultado/i });
    expect(botonCargar).toBeInTheDocument();

    fireEvent.click(botonCargar);
    expect(onCargar).toHaveBeenCalledWith(partidosMock[1]);
  });

  it("soporta el contrato DTO de backend (participanteANombre/participanteBNombre) y determina ganador por tanteador", () => {
    const partidosBackend = [
      {
        id: 201,
        ronda: 1,
        orden: 1,
        participanteAId: 101,
        participanteANombre: "Carlos Alcaraz",
        participanteBId: 102,
        participanteBNombre: "Jannik Sinner",
        sets: "7-6 6-4",
      },
      {
        id: 202,
        ronda: 1,
        orden: 2,
        participanteAId: 103,
        participanteANombre: "Daniil Medvedev",
        participanteBId: 104,
        participanteBNombre: "Alexander Zverev",
        sets: null,
      },
    ];

    render(
      <FixtureTree
        partidos={partidosBackend}
        esAdmin={true}
        onCargarResultado={vi.fn()}
      />
    );

    expect(screen.getByText("Carlos Alcaraz")).toBeInTheDocument();
    expect(screen.getByText("Jannik Sinner")).toBeInTheDocument();
    expect(screen.getByText("Daniil Medvedev")).toBeInTheDocument();
    expect(screen.getByText("Alexander Zverev")).toBeInTheDocument();
    // Partido 202 pendiente con ambos participantes debe mostrar botón Cargar resultado
    expect(screen.getByRole("button", { name: /cargar resultado/i })).toBeInTheDocument();
  });
});
