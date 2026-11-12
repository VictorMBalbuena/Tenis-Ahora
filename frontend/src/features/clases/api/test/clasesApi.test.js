import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";
import { listarClases, listarMisClases } from "../clasesApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("clasesApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: null });
  });

  describe("listarMisClases", () => {
    it("limita el tamaño máximo de página a 50 para respetar la restricción del backend", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisClases({ page: 0, size: 100 });

      expect(axiosClient.get).toHaveBeenCalledWith("/clases/mis-clases", {
        params: { page: 0, size: 50 },
      });
    });

    it("asegura un tamaño mínimo de 1", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisClases({ page: 0, size: 0 });

      expect(axiosClient.get).toHaveBeenCalledWith("/clases/mis-clases", {
        params: { page: 0, size: 50 },
      });
    });

    it("respeta un tamaño válido menor o igual a 50", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisClases({ page: 1, size: 25 });

      expect(axiosClient.get).toHaveBeenCalledWith("/clases/mis-clases", {
        params: { page: 1, size: 25 },
      });
    });
  });

  describe("listarClases", () => {
    it("delega a listarMisClases si el usuario autenticado es PROFESOR", async () => {
      useAuthStore.setState({ user: { id: 7, rol: "PROFESOR" } });
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarClases({ page: 0, size: 100 });

      expect(axiosClient.get).toHaveBeenCalledWith("/clases/mis-clases", {
        params: { page: 0, size: 50 },
      });
    });

    it("consulta /clases normal si no es PROFESOR", async () => {
      useAuthStore.setState({ user: { id: 7, rol: "ADMINISTRADOR" } });
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarClases({ page: 0, size: 20 });

      expect(axiosClient.get).toHaveBeenCalledWith("/clases", {
        params: { page: 0, size: 20 },
      });
    });
  });
});
