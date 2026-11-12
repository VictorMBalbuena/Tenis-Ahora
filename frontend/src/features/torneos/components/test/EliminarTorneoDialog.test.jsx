import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EliminarTorneoDialog } from "../EliminarTorneoDialog";
import * as torneosApi from "../../api/torneosApi";

vi.mock("../../api/torneosApi", () => ({
  eliminarTorneo: vi.fn(),
}));

describe("EliminarTorneoDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const torneoMock = {
    id: 1,
    nombre: "Torneo Anual Primavera",
    categoria: "MASCULINO",
    modalidad: "SINGLE",
  };

  it("renderiza datos de confirmación del torneo", () => {
    render(
      <EliminarTorneoDialog
        open={true}
        torneo={torneoMock}
        onClose={vi.fn()}
        onEliminado={vi.fn()}
      />
    );

    expect(screen.getByText("Torneo Anual Primavera")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /eliminar torneo/i })).toBeInTheDocument();
  });

  it("dispara eliminarTorneo y onEliminado al confirmar", async () => {
    const onEliminado = vi.fn();
    torneosApi.eliminarTorneo.mockResolvedValueOnce({});

    render(
      <EliminarTorneoDialog
        open={true}
        torneo={torneoMock}
        onClose={vi.fn()}
        onEliminado={onEliminado}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /eliminar torneo/i }));

    await waitFor(() => {
      expect(torneosApi.eliminarTorneo).toHaveBeenCalledWith(1);
      expect(onEliminado).toHaveBeenCalled();
    });
  });
});
