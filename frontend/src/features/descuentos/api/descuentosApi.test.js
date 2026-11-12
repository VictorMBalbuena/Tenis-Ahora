import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import {
  actualizarDescuento,
  crearDescuento,
  eliminarDescuento,
  listarDescuentos,
  obtenerDescuento,
  validarCodigo,
} from "./descuentosApi";

vi.mock("@/config/axiosClient", () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

/**
 * Esta capa ya no decide nada: las reglas del beneficio, el paginado y la unicidad
 * del código las aplica el backend. Antes respondía contra un mock en memoria y
 * estos tests probaban ese mock; ahora prueban lo único que quedó acá, que es a
 * qué endpoint se llama, con qué parámetros y qué se devuelve.
 */
describe("descuentosApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  /** Los parámetros con los que se llamó a axios en la última llamada. */
  const paramsDeLaLlamada = () => axiosClient.get.mock.calls[0][1].params;

  describe("listarDescuentos", () => {
    beforeEach(() => {
      axiosClient.get.mockResolvedValue({ data: { contenido: [], totalElementos: 0 } });
    });

    it("pide la página al recurso de descuentos", async () => {
      await listarDescuentos({ page: 2, size: 10 });

      expect(axiosClient.get).toHaveBeenCalledWith("/descuentos", { params: { page: 2, size: 10 } });
    });

    it("devuelve lo que contesta el servidor, sin tocarlo", async () => {
      const pagina = { contenido: [{ id: 1, codigo: "CYBERWEEK" }], totalElementos: 1 };
      axiosClient.get.mockResolvedValue({ data: pagina });

      expect(await listarDescuentos()).toEqual(pagina);
    });

    it("no manda los filtros que están vacíos", async () => {
      await listarDescuentos({ busqueda: "   ", tipo: "", activo: "" });

      expect(paramsDeLaLlamada()).toEqual({ page: 0, size: 20 });
    });

    it("traduce el activo de la pantalla al booleano de la api", async () => {
      await listarDescuentos({ activo: "SI" });
      expect(paramsDeLaLlamada().activo).toBe(true);

      vi.clearAllMocks();
      axiosClient.get.mockResolvedValue({ data: {} });
      await listarDescuentos({ activo: "NO" });
      expect(paramsDeLaLlamada().activo).toBe(false);
    });

    it("recorta la búsqueda antes de mandarla", async () => {
      await listarDescuentos({ busqueda: "  liga  " });

      expect(paramsDeLaLlamada().busqueda).toBe("liga");
    });
  });

  describe("obtenerDescuento", () => {
    it("pide el descuento por su id", async () => {
      axiosClient.get.mockResolvedValue({ data: { id: 7 } });

      expect(await obtenerDescuento(7)).toEqual({ id: 7 });
      expect(axiosClient.get).toHaveBeenCalledWith("/descuentos/7");
    });
  });

  describe("crear y modificar", () => {
    it("manda el código junto con el resto del descuento", async () => {
      axiosClient.post.mockResolvedValue({ data: { id: 1 } });

      await crearDescuento({ codigo: "CYBERWEEK", tipo: "PROMOCIONAL", porcentaje: 25, condiciones: "Campaña" });

      expect(axiosClient.post).toHaveBeenCalledWith("/descuentos", {
        codigo: "CYBERWEEK",
        tipo: "PROMOCIONAL",
        porcentaje: 25,
        montoFijo: null,
        condiciones: "Campaña",
        activo: true,
      });
    });

    it("la forma que no se usa viaja en null, nunca sin la clave", async () => {
      axiosClient.post.mockResolvedValue({ data: {} });

      await crearDescuento({ codigo: "MIL", tipo: "PROMOCIONAL", montoFijo: 1000 });

      expect(axiosClient.post.mock.calls[0][1]).toMatchObject({ porcentaje: null, montoFijo: 1000 });
    });

    it("modificar va por PUT al id", async () => {
      axiosClient.put.mockResolvedValue({ data: {} });

      await actualizarDescuento(3, { codigo: "LIGA20", tipo: "LIGA_REGULAR", porcentaje: 20, activo: false });

      expect(axiosClient.put).toHaveBeenCalledWith("/descuentos/3", expect.objectContaining({
        codigo: "LIGA20",
        activo: false,
      }));
    });
  });

  describe("eliminarDescuento", () => {
    it("va por DELETE al id", async () => {
      axiosClient.delete.mockResolvedValue({});

      await eliminarDescuento(5);

      expect(axiosClient.delete).toHaveBeenCalledWith("/descuentos/5");
    });
  });

  describe("validarCodigo", () => {
    it("consulta el código con el monto sobre el que se calcula", async () => {
      axiosClient.get.mockResolvedValue({ data: { codigo: "CYBERWEEK", montoDescontado: 4000 } });

      const resultado = await validarCodigo("CYBERWEEK", 16000);

      expect(axiosClient.get).toHaveBeenCalledWith("/descuentos/codigo/CYBERWEEK", { params: { monto: 16000 } });
      expect(resultado.montoDescontado).toBe(4000);
    });

    it("recorta y escapa lo que se escribió, para que no rompa la url", async () => {
      axiosClient.get.mockResolvedValue({ data: {} });

      await validarCodigo("  CODIGO RARO  ");

      expect(axiosClient.get.mock.calls[0][0]).toBe("/descuentos/codigo/CODIGO%20RARO");
    });

    it("sin monto consulta contra cero: sirve para saber si el código existe", async () => {
      axiosClient.get.mockResolvedValue({ data: {} });

      await validarCodigo("CYBERWEEK");

      expect(paramsDeLaLlamada().monto).toBe(0);
    });
  });
});
