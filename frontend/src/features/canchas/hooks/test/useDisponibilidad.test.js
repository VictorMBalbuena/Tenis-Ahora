import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { useDisponibilidad } from "../useDisponibilidad";

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn() },
}));

describe("useDisponibilidad", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("trae las canchas y la disponibilidad de cada una para esa fecha", async () => {
    axiosClient.get.mockImplementation((url) => {
      if (url === "/canchas") {
        return Promise.resolve({ data: { contenido: [{ id: 1 }, { id: 2 }] } });
      }
      if (url === "/canchas/1/disponibilidad") {
        return Promise.resolve({ data: { franjas: [{ desde: "10:00", estado: "OCUPADA" }] } });
      }
      return Promise.resolve({ data: { franjas: [] } });
    });

    const { result } = renderHook(() => useDisponibilidad("2026-10-23"));

    await waitFor(() => expect(result.current.cargando).toBe(false));

    expect(result.current.canchas).toEqual([{ id: 1 }, { id: 2 }]);
    expect(result.current.grillaData[1]).toEqual({ franjas: [{ desde: "10:00", estado: "OCUPADA" }] });
    expect(result.current.grillaData[2]).toEqual({ franjas: [] });
    expect(axiosClient.get).toHaveBeenCalledWith("/canchas/1/disponibilidad", {
      params: { desde: "2026-10-23", hasta: "2026-10-23" },
    });
  });

  it("si la disponibilidad de una cancha falla, igual muestra el resto", async () => {
    axiosClient.get.mockImplementation((url) => {
      if (url === "/canchas") return Promise.resolve({ data: { contenido: [{ id: 1 }] } });
      return Promise.reject(new Error("502"));
    });

    const { result } = renderHook(() => useDisponibilidad("2026-10-23"));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.canchas).toEqual([{ id: 1 }]);
    expect(result.current.grillaData[1]).toBeNull();
    expect(result.current.error).toBe(false);
  });

  it("marca error si falla el listado de canchas", async () => {
    axiosClient.get.mockRejectedValueOnce(new Error("red caída"));

    const { result } = renderHook(() => useDisponibilidad("2026-10-23"));

    await waitFor(() => expect(result.current.cargando).toBe(false));
    expect(result.current.error).toBe(true);
  });

  it("recargar vuelve a pedir todo", async () => {
    axiosClient.get.mockResolvedValue({ data: { contenido: [] } });

    const { result } = renderHook(() => useDisponibilidad("2026-10-23"));
    await waitFor(() => expect(result.current.cargando).toBe(false));

    act(() => result.current.recargar());

    await waitFor(() => expect(axiosClient.get).toHaveBeenCalledTimes(2));
  });
});
