import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";
import { listarEntrenamientos, listarMisEntrenamientos } from "../entrenamientosApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("entrenamientosApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: null });
  });

  describe("listarMisEntrenamientos", () => {
    it("limita el tamaño máximo de página a 50 para respetar la restricción del backend", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisEntrenamientos({ page: 0, size: 100 });

      expect(axiosClient.get).toHaveBeenCalledWith("/entrenamientos/mis-entrenamientos", {
        params: { page: 0, size: 50 },
      });
    });

    it("asegura un tamaño mínimo de 1", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisEntrenamientos({ page: 0, size: 0 });

      expect(axiosClient.get).toHaveBeenCalledWith("/entrenamientos/mis-entrenamientos", {
        params: { page: 0, size: 50 },
      });
    });

    it("respeta un tamaño válido menor o igual a 50", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMisEntrenamientos({ page: 1, size: 25 });

      expect(axiosClient.get).toHaveBeenCalledWith("/entrenamientos/mis-entrenamientos", {
        params: { page: 1, size: 25 },
      });
    });
  });

  describe("listarEntrenamientos", () => {
    it("delega a listarMisEntrenamientos si el usuario autenticado es ENTRENADOR", async () => {
      useAuthStore.setState({ user: { id: 7, rol: "ENTRENADOR" } });
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarEntrenamientos({ page: 0, size: 100 });

      expect(axiosClient.get).toHaveBeenCalledWith("/entrenamientos/mis-entrenamientos", {
        params: { page: 0, size: 50 },
      });
    });

    it("consulta /entrenamientos con los filtros normales si no es ENTRENADOR", async () => {
      useAuthStore.setState({ user: { id: 2, rol: "EMPLEADO" } });
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarEntrenamientos({ page: 0, size: 20, estado: "PROGRAMADA" });

      expect(axiosClient.get).toHaveBeenCalledWith("/entrenamientos", {
        params: { page: 0, size: 20, estado: "PROGRAMADA" },
      });
    });
  });
});
