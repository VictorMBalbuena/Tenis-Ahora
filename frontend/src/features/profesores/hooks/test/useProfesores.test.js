import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listarProfesores } from "../../api/profesoresApi";
import { useProfesores, useValorDemorado } from "../useProfesores";

vi.mock("../../api/profesoresApi", () => ({
  listarProfesores: vi.fn(),
}));

describe("useProfesores", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("pide la página con los filtros armados y expone el resultado", async () => {
    listarProfesores.mockResolvedValueOnce({
      contenido: [{ id: 1, nombre: "Ana" }],
      totalPaginas: 2,
      totalElementos: 21,
    });

    const { result } = renderHook(() =>
      useProfesores({ busqueda: "ana", estado: "VALIDADO", activo: "true", pagina: 1 })
    );

    await waitFor(() => expect(result.current.cargando).toBe(false));

    expect(listarProfesores).toHaveBeenCalledWith({
      busqueda: "ana",
      estado: "VALIDADO",
      activo: "true",
      page: 1,
      size: 20,
    });
    expect(result.current.profesores).toEqual([{ id: 1, nombre: "Ana" }]);
    expect(result.current.totalPaginas).toBe(2);
    expect(result.current.totalElementos).toBe(21);
  });

  it("marca error si falla el pedido", async () => {
    listarProfesores.mockRejectedValueOnce(new Error("500"));

    const { result } = renderHook(() => useProfesores({}));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.profesores).toEqual([]);
  });

  it("recargar vuelve a pedir", async () => {
    listarProfesores.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    const { result } = renderHook(() => useProfesores({}));
    await waitFor(() => expect(result.current.cargando).toBe(false));

    act(() => result.current.recargar());

    await waitFor(() => expect(listarProfesores).toHaveBeenCalledTimes(2));
  });
});

describe("useValorDemorado", () => {
  it("devuelve el valor recién después del tiempo de espera", async () => {
    vi.useFakeTimers();
    const { result, rerender } = renderHook(({ valor }) => useValorDemorado(valor, 300), {
      initialProps: { valor: "a" },
    });

    rerender({ valor: "ab" });
    expect(result.current).toBe("a");

    act(() => vi.advanceTimersByTime(300));
    expect(result.current).toBe("ab");
    vi.useRealTimers();
  });
});
