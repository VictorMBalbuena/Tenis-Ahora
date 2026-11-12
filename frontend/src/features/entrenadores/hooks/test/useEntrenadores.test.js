import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useEntrenadores } from "../useEntrenadores";
import { listarEntrenadores } from "../../api/entrenadoresApi";

vi.mock("../../api/entrenadoresApi", () => ({ listarEntrenadores: vi.fn() }));

describe("useEntrenadores", () => {
  it("carga el listado y expone la paginación", async () => {
    listarEntrenadores.mockResolvedValueOnce({ contenido: [{ id: 1 }], totalPaginas: 2, totalElementos: 15 });

    const { result } = renderHook(() => useEntrenadores({ busqueda: "", pagina: 0 }));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.entrenadores).toEqual([{ id: 1 }]);
    expect(result.current.totalPaginas).toBe(2);
  });

  it("si falla, expone el error y una lista vacía", async () => {
    listarEntrenadores.mockRejectedValueOnce(new Error("falló"));

    const { result } = renderHook(() => useEntrenadores({ busqueda: "", pagina: 0 }));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.entrenadores).toEqual([]);
    expect(result.current.error).toBeInstanceOf(Error);
  });
});
