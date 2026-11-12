import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EntrenadoresTabla } from "../EntrenadoresTabla";

const entrenador = {
  id: 1,
  nombre: "Lucas",
  apellido: "Pérez",
  dni: "31123456",
  email: "lucas@correo.com",
  especialidad: "Alto rendimiento",
  estadoValidacion: "PENDIENTE",
  activo: true,
};

describe("EntrenadoresTabla", () => {
  it("muestra los datos del entrenador", () => {
    render(<EntrenadoresTabla entrenadores={[entrenador]} onVer={vi.fn()} onEditar={vi.fn()} onImprimir={vi.fn()} onEliminar={vi.fn()} />);
    expect(screen.getAllByText("Lucas Pérez")[0]).toBeInTheDocument();
    expect(screen.getAllByText("Alto rendimiento")[0]).toBeInTheDocument();
  });

  it("dispara onVer al tocar el ícono de detalle", async () => {
    const onVer = vi.fn();
    render(<EntrenadoresTabla entrenadores={[entrenador]} onVer={onVer} onEditar={vi.fn()} onImprimir={vi.fn()} onEliminar={vi.fn()} />);

    await userEvent.click(screen.getAllByRole("button", { name: /Ver el detalle de/ })[0]);
    expect(onVer).toHaveBeenCalledWith(entrenador);
  });

  it("muestra la paginación cuando hay más de una página", () => {
    render(
      <EntrenadoresTabla
        entrenadores={[entrenador]}
        pagina={0}
        totalPaginas={2}
        totalElementos={2}
        onCambiarPagina={vi.fn()}
        onVer={vi.fn()}
        onEditar={vi.fn()}
        onImprimir={vi.fn()}
        onEliminar={vi.fn()}
      />
    );
    expect(screen.getByRole("navigation", { name: "Paginación de entrenadores" })).toBeInTheDocument();
  });
});
