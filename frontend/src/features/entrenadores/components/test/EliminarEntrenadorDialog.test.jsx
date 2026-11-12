import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EliminarEntrenadorDialog } from "../EliminarEntrenadorDialog";
import { eliminarEntrenador } from "../../api/entrenadoresApi";

vi.mock("../../api/entrenadoresApi", () => ({ eliminarEntrenador: vi.fn() }));

const entrenador = { id: 1, nombre: "Lucas", apellido: "Pérez", dni: "31123456", email: "lucas@correo.com", estadoValidacion: "PENDIENTE" };

describe("EliminarEntrenadorDialog", () => {
  it("confirma la baja y avisa con el mensaje", async () => {
    eliminarEntrenador.mockResolvedValueOnce();
    const onEliminado = vi.fn();
    render(<EliminarEntrenadorDialog abierto entrenador={entrenador} onCerrar={vi.fn()} onEliminado={onEliminado} />);

    await userEvent.click(screen.getByRole("button", { name: "Eliminar entrenador" }));

    expect(eliminarEntrenador).toHaveBeenCalledWith(1);
    expect(onEliminado).toHaveBeenCalledWith("Se eliminó a Lucas Pérez.");
  });

  it("si falla, muestra el mensaje de error", async () => {
    eliminarEntrenador.mockRejectedValueOnce({ response: { data: { mensaje: "Tiene entrenamientos asignados" } } });
    render(<EliminarEntrenadorDialog abierto entrenador={entrenador} onCerrar={vi.fn()} onEliminado={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "Eliminar entrenador" }));

    expect(await screen.findByText("Tiene entrenamientos asignados")).toBeInTheDocument();
  });
});
