import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  listarCategoriasInsumo,
  listarInsumos,
  obtenerResumenInsumos,
} from "../../api/insumosApi";
import {
  useCategoriasInsumo,
  useInsumos,
  useResumenInsumos,
  useValorDemorado,
} from "../useInsumos";

vi.mock("../../api/insumosApi", () => ({
  listarInsumos: vi.fn(),
  listarCategoriasInsumo: vi.fn(),
  obtenerResumenInsumos: vi.fn(),
}));

describe("useInsumos", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("pide la página con los filtros y expone el resultado", async () => {
    listarInsumos.mockResolvedValueOnce({
      contenido: [{ id: 1, nombre: "Raquetas" }],
      totalPaginas: 2,
      totalElementos: 15,
    });

    const { result } = renderHook(() => useInsumos({ busqueda: "ra", categoriaId: "3", estado: "", page: 0 }));

    await waitFor(() => expect(result.current.cargando).toBe(false));

    expect(listarInsumos).toHaveBeenCalledWith({ busqueda: "ra", categoriaId: "3", estado: "", page: 0, size: 20 });
    expect(result.current.contenido).toEqual([{ id: 1, nombre: "Raquetas" }]);
    expect(result.current.totalPaginas).toBe(2);
    expect(result.current.totalElementos).toBe(15);
    expect(result.current.error).toBeNull();
  });

  it("descarta la respuesta de un pedido viejo si llega después de uno nuevo", async () => {
    let resolverPrimero;
    listarInsumos
      .mockImplementationOnce(() => new Promise((resolve) => (resolverPrimero = resolve)))
      .mockResolvedValueOnce({ contenido: [{ id: 2 }], totalPaginas: 1, totalElementos: 1 });

    const { result, rerender } = renderHook(({ page }) => useInsumos({ page }), { initialProps: { page: 0 } });
    rerender({ page: 1 });

    await waitFor(() => expect(result.current.cargando).toBe(false));
    resolverPrimero({ contenido: [{ id: 1 }], totalPaginas: 1, totalElementos: 1 });

    await new Promise((r) => setTimeout(r, 0));
    expect(result.current.contenido).toEqual([{ id: 2 }]);
  });

  it("marca error si falla el pedido", async () => {
    listarInsumos.mockRejectedValueOnce(new Error("500"));

    const { result } = renderHook(() => useInsumos({}));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.contenido).toEqual([]);
  });

  it("recargar vuelve a pedir", async () => {
    listarInsumos.mockResolvedValue({ contenido: [], totalPaginas: 0, totalElementos: 0 });

    const { result } = renderHook(() => useInsumos({}));
    await waitFor(() => expect(result.current.cargando).toBe(false));

    act(() => result.current.recargar());

    await waitFor(() => expect(listarInsumos).toHaveBeenCalledTimes(2));
  });
});

describe("useResumenInsumos", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("trae el resumen y permite recargarlo", async () => {
    obtenerResumenInsumos.mockResolvedValueOnce({ distintos: 3, enDeposito: 50, asignadas: 4, alertas: 1 });

    const { result } = renderHook(() => useResumenInsumos());

    await waitFor(() => expect(result.current.resumen.distintos).toBe(3));

    obtenerResumenInsumos.mockResolvedValueOnce({ distintos: 4, enDeposito: 60, asignadas: 5, alertas: 0 });
    await act(() => result.current.recargarResumen());

    expect(result.current.resumen.distintos).toBe(4);
  });
});

describe("useCategoriasInsumo", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("trae las categorías y expone cargando", async () => {
    listarCategoriasInsumo.mockResolvedValueOnce([{ id: 1, nombre: "Raqueta" }]);

    const { result } = renderHook(() => useCategoriasInsumo());

    expect(result.current.cargando).toBe(true);
    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.categorias).toEqual([{ id: 1, nombre: "Raqueta" }]);
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
