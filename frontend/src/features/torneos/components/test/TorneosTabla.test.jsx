import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { TorneosTabla } from "../TorneosTabla";

describe("TorneosTabla", () => {
  const torneosMock = [
    {
      id: 1,
      nombre: "Torneo Abierto Primavera",
      categoria: "MASCULINO",
      modalidad: "SINGLE",
      tipoSuperficie: "LADRILLO",
      estado: "PLANIFICADO",
      cantidadInscriptos: 8,
      cupoMaximo: 16,
    },
    {
      id: 2,
      nombre: "Torneo Dobles Master",
      categoria: "FEMENINO",
      modalidad: "PAREJA",
      tipoSuperficie: "PASTO",
      estado: "EN_CURSO",
      cantidadInscriptos: 16,
      cupoMaximo: 16,
    },
  ];

  it("renderiza columnas y datos de torneos", () => {
    render(
      <TorneosTabla
        torneos={torneosMock}
        esAdmin={false}
        onVerDetalle={vi.fn()}
        onEditar={vi.fn()}
        onEliminar={vi.fn()}
      />
    );

    expect(screen.getByText("Torneo Abierto Primavera")).toBeInTheDocument();
    expect(screen.getByText("Torneo Dobles Master")).toBeInTheDocument();
    expect(screen.getByText("MASCULINO")).toBeInTheDocument();
    expect(screen.getByText("Polvo de ladrillo")).toBeInTheDocument();
    expect(screen.getByText("SINGLE")).toBeInTheDocument();
    expect(screen.getByText("8/16")).toBeInTheDocument();
  });

  it("oculta botones de editar y eliminar para usuarios no administradores", () => {
    render(
      <TorneosTabla
        torneos={torneosMock}
        esAdmin={false}
        onVerDetalle={vi.fn()}
        onEditar={vi.fn()}
        onEliminar={vi.fn()}
      />
    );

    expect(screen.getAllByRole("button", { name: /ver detalle/i }).length).toBe(2);
    expect(screen.queryByRole("button", { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /eliminar|baja/i })).not.toBeInTheDocument();
  });

  it("muestra botones de editar y eliminar si esAdmin es true y dispara callbacks", () => {
    const onEditar = vi.fn();
    const onEliminar = vi.fn();
    const onVerDetalle = vi.fn();

    render(
      <TorneosTabla
        torneos={torneosMock}
        esAdmin={true}
        onVerDetalle={onVerDetalle}
        onEditar={onEditar}
        onEliminar={onEliminar}
      />
    );

    const botonesEditar = screen.getAllByRole("button", { name: /editar/i });
    const botonesEliminar = screen.getAllByRole("button", { name: /eliminar|baja/i });

    expect(botonesEditar.length).toBe(2);
    expect(botonesEliminar.length).toBe(2);

    fireEvent.click(botonesEditar[0]);
    expect(onEditar).toHaveBeenCalledWith(torneosMock[0]);

    fireEvent.click(botonesEliminar[0]);
    expect(onEliminar).toHaveBeenCalledWith(torneosMock[0]);

    const botonesVer = screen.getAllByRole("button", { name: /ver detalle/i });
    fireEvent.click(botonesVer[0]);
    expect(onVerDetalle).toHaveBeenCalledWith(torneosMock[0]);
  });
});
