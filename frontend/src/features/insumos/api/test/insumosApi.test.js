import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import {
  actualizarInsumo,
  crearCategoriaInsumo,
  crearInsumo,
  eliminarInsumo,
  listarCategoriasInsumo,
  listarInsumos,
  listarMovimientos,
  obtenerInsumo,
  obtenerResumenInsumos,
  registrarMovimiento,
} from "../insumosApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("insumosApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("listarInsumos", () => {
    it("pide la primera página con el tamaño por defecto", async () => {
      axiosClient.get.mockResolvedValueOnce({
        data: { contenido: [], totalPaginas: 0, totalElementos: 0 },
      });

      const res = await listarInsumos();

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos", {
        params: { page: 0, size: 20 },
      });
      expect(res.contenido).toEqual([]);
    });

    it("manda los filtros que vienen con valor", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarInsumos({
        busqueda: "pelota",
        categoriaId: 2,
        estado: "STOCK_BAJO",
        page: 2,
        size: 10,
      });

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos", {
        params: {
          busqueda: "pelota",
          categoriaId: 2,
          estado: "STOCK_BAJO",
          page: 2,
          size: 10,
        },
      });
    });

    it("no manda los filtros vacíos, para no pedir un filtro en blanco", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarInsumos({ busqueda: "", categoriaId: "", estado: "" });

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos", {
        params: { page: 0, size: 20 },
      });
    });
  });

  describe("obtenerResumenInsumos", () => {
    it("pide los totales globales para la tira de KPIs", async () => {
      axiosClient.get.mockResolvedValueOnce({
        data: { distintos: 12, enDeposito: 340, asignadas: 18, alertas: 2 },
      });

      const res = await obtenerResumenInsumos();

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos/resumen");
      expect(res).toEqual({ distintos: 12, enDeposito: 340, asignadas: 18, alertas: 2 });
    });
  });

  describe("listarCategoriasInsumo", () => {
    it("pide la lista de categorías", async () => {
      axiosClient.get.mockResolvedValueOnce({
        data: [{ id: 1, nombre: "Raqueta" }],
      });

      const res = await listarCategoriasInsumo();

      expect(axiosClient.get).toHaveBeenCalledWith("/categorias-insumo");
      expect(res).toEqual([{ id: 1, nombre: "Raqueta" }]);
    });
  });

  describe("crearCategoriaInsumo", () => {
    it("manda el alta por POST y devuelve la categoría creada", async () => {
      axiosClient.post.mockResolvedValueOnce({
        data: { id: 4, nombre: "Grip / overgrip" },
      });

      const res = await crearCategoriaInsumo("Grip / overgrip");

      expect(axiosClient.post).toHaveBeenCalledWith("/categorias-insumo", {
        nombre: "Grip / overgrip",
      });
      expect(res.id).toBe(4);
    });
  });

  describe("obtenerInsumo", () => {
    it("pide el insumo por su id", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { id: 3 } });

      const res = await obtenerInsumo(3);

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos/3");
      expect(res.id).toBe(3);
    });
  });

  describe("crearInsumo", () => {
    it("manda el alta por POST y devuelve el insumo creado", async () => {
      const datos = {
        nombre: "Pelotas de torneo",
        categoriaId: 2,
        cantidadDisponible: 40,
        cantidadMinima: 10,
      };
      axiosClient.post.mockResolvedValueOnce({ data: { id: 9, ...datos } });

      const res = await crearInsumo(datos);

      expect(axiosClient.post).toHaveBeenCalledWith("/insumos", datos);
      expect(res.id).toBe(9);
    });
  });

  describe("actualizarInsumo", () => {
    it("usa PATCH y manda solo los campos recibidos", async () => {
      axiosClient.patch.mockResolvedValueOnce({ data: { id: 1 } });

      await actualizarInsumo(1, { cantidadMinima: 12 });

      expect(axiosClient.patch).toHaveBeenCalledWith("/insumos/1", {
        cantidadMinima: 12,
      });
    });
  });

  describe("eliminarInsumo", () => {
    it("manda el DELETE y no devuelve contenido", async () => {
      axiosClient.delete.mockResolvedValueOnce({});

      const res = await eliminarInsumo(5);

      expect(axiosClient.delete).toHaveBeenCalledWith("/insumos/5");
      expect(res).toBeUndefined();
    });
  });

  describe("registrarMovimiento", () => {
    it("postea al sub-recurso de movimientos del insumo", async () => {
      axiosClient.post.mockResolvedValueOnce({
        data: { id: 1, cantidadDisponible: 3 },
      });

      const res = await registrarMovimiento(1, {
        tipo: "DESCUENTO",
        cantidad: 4,
        motivo: "Rotura en la cancha 2",
      });

      expect(axiosClient.post).toHaveBeenCalledWith("/insumos/1/movimientos", {
        tipo: "DESCUENTO",
        cantidad: 4,
        motivo: "Rotura en la cancha 2",
      });
      expect(res.cantidadDisponible).toBe(3);
    });

    it("propaga el error del backend cuando el stock no alcanza", async () => {
      const error = {
        response: {
          status: 422,
          data: {
            codigo: "STOCK_INSUFICIENTE",
            mensaje: "Solo hay 7 unidades disponibles de Redes",
          },
        },
      };
      axiosClient.post.mockRejectedValueOnce(error);

      await expect(
        registrarMovimiento(3, { tipo: "DESCUENTO", cantidad: 20 }),
      ).rejects.toBe(error);
    });
  });

  describe("listarMovimientos", () => {
    it("pide el historial paginado del insumo", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarMovimientos(2, { size: 10 });

      expect(axiosClient.get).toHaveBeenCalledWith("/insumos/2/movimientos", {
        params: { page: 0, size: 10 },
      });
    });
  });
});
