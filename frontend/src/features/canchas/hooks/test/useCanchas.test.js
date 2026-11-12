import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { useCanchas } from "../useCanchas";

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn() },
}));

describe("useCanchas", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("pide la página con los filtros armados y expone el resultado", async () => {
    axiosClient.get.mockResolvedValueOnce({
      data: { contenido: [{ id: 1, identificador: "Cancha 1" }], totalPaginas: 2, totalElementos: 21 },
    });

    const { result } = renderHook(() =>
      useCanchas({ busqueda: "cancha", superficie: "PASTO", estado: "MANTENIMIENTO", pagina: 1 })
    );

    await waitFor(() => expect(result.current.cargando).toBe(false));

    expect(axiosClient.get).toHaveBeenCalledWith("/canchas", {
      params: { page: 1, size: 20, identificador: "cancha", superficie: "PASTO", estado: "MANTENIMIENTO" },
    });
    expect(result.current.canchas).toEqual([{ id: 1, identificador: "Cancha 1" }]);
    expect(result.current.totalPaginas).toBe(2);
    expect(result.current.totalElementos).toBe(21);
    expect(result.current.error).toBe(false);
  });

  it("no manda identificador ni superficie cuando vienen vacíos", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

    renderHook(() => useCanchas({ busqueda: "", superficie: "" }));

    await waitFor(() => expect(axiosClient.get).toHaveBeenCalled());
    expect(axiosClient.get).toHaveBeenCalledWith("/canchas", { params: { page: 0, size: 20 } });
  });

  it("marca error si falla el pedido", async () => {
    axiosClient.get.mockRejectedValueOnce(new Error("red caída"));

    const { result } = renderHook(() => useCanchas({}));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.error).toBe(true);
    expect(result.current.canchas).toEqual([]);
  });

  it("quitarCancha saca la cancha de la lista sin volver a pedir la página", async () => {
    axiosClient.get.mockResolvedValueOnce({
      data: { contenido: [{ id: 1 }, { id: 2 }] },
    });

    const { result } = renderHook(() => useCanchas({}));
    await waitFor(() => expect(result.current.cargando).toBe(false));

    act(() => result.current.quitarCancha(1));

    expect(result.current.canchas).toEqual([{ id: 2 }]);
    expect(axiosClient.get).toHaveBeenCalledTimes(1);
  });

  it("recargar vuelve a pedir la página", async () => {
    axiosClient.get.mockResolvedValue({ data: { contenido: [] } });

    const { result } = renderHook(() => useCanchas({}));
    await waitFor(() => expect(result.current.cargando).toBe(false));

    act(() => result.current.recargar());

    await waitFor(() => expect(axiosClient.get).toHaveBeenCalledTimes(2));
  });
});
