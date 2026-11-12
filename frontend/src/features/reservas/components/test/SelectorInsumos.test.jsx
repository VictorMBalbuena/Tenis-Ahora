import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { SelectorInsumos } from "../SelectorInsumos";

describe("SelectorInsumos", () => {
  const mockInsumos = [
    {
      insumoId: 1,
      nombre: "Raquetas Head",
      cantidadAsignable: 4,
      precioUnitario: 2000,
      categoria: { nombre: "Equipamiento" },
    },
    {
      insumoId: 2,
      nombre: "Pelotas Wilson",
      cantidadAsignable: 0,
      categoria: { nombre: "Pelotas" },
    },
  ];

  it("renderiza lista de insumos con categorías y stock", () => {
    render(<SelectorInsumos insumos={mockInsumos} cantidades={{ 1: 0, 2: 0 }} />);

    expect(screen.getByText("Raquetas Head")).toBeInTheDocument();
    expect(screen.getByText("Equipamiento")).toBeInTheDocument();
    expect(screen.getByText(/4 disponibles/i)).toBeInTheDocument();
    expect(screen.getByText(/sin stock disponible/i)).toBeInTheDocument();
  });

  it("llama a onCambiarCantidad al presionar botones de incremento y decremento", () => {
    const onCambiarCantidad = vi.fn();
    render(
      <SelectorInsumos
        insumos={mockInsumos}
        cantidades={{ 1: 1 }}
        onCambiarCantidad={onCambiarCantidad}
      />
    );

    const btnIncrementar = screen.getByLabelText(/aumentar raquetas head/i);
    fireEvent.click(btnIncrementar);
    expect(onCambiarCantidad).toHaveBeenCalledWith(1, 2);

    const btnDecrementar = screen.getByLabelText(/disminuir raquetas head/i);
    fireEvent.click(btnDecrementar);
    expect(onCambiarCantidad).toHaveBeenCalledWith(1, 0);
  });

  it("permite decrementar un ítem seleccionado previamente aunque el stock sea 0", () => {
    const onCambiarCantidad = vi.fn();
    render(
      <SelectorInsumos
        insumos={mockInsumos}
        cantidades={{ 2: 2 }} // Pelotas Wilson tiene cantidadAsignable: 0 pero 2 seleccionadas
        onCambiarCantidad={onCambiarCantidad}
      />
    );

    const btnDecrementar = screen.getByLabelText(/disminuir pelotas wilson/i);
    expect(btnDecrementar).not.toBeDisabled();
    fireEvent.click(btnDecrementar);
    expect(onCambiarCantidad).toHaveBeenCalledWith(2, 1);
  });

  it("muestra estado de carga cuando cargando es true", () => {
    render(<SelectorInsumos cargando={true} />);
    expect(screen.getByText(/cargando insumos disponibles/i)).toBeInTheDocument();
  });

  it("muestra mensaje de error cuando ocurre un fallo", () => {
    render(<SelectorInsumos error="Error de conexión" />);
    expect(screen.getByRole("alert")).toHaveTextContent("Error de conexión");
  });

  it("muestra mensaje cuando no hay insumos disponibles", () => {
    render(<SelectorInsumos insumos={[]} />);
    expect(screen.getByText(/no hay insumos disponibles/i)).toBeInTheDocument();
  });
});
