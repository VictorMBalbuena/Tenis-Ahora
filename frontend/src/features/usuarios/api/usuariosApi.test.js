import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import {
  actualizarUsuario,
  crearUsuario,
  eliminarUsuario,
  imprimirFichaUsuario,
  listarUsuarios,
  obtenerUsuario,
} from "./usuariosApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("usuariosApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("listarUsuarios", () => {
    it("llama al endpoint correcto según el tipo de persona con paginación por defecto", async () => {
      axiosClient.get.mockResolvedValueOnce({
        data: { contenido: [], totalPaginas: 0, totalElementos: 0 },
      });

      const res = await listarUsuarios("SOCIO");

      expect(axiosClient.get).toHaveBeenCalledWith("/socios", {
        params: { page: 0, size: 20 },
      });
      expect(res.contenido).toEqual([]);
    });

    it("envía filtro dni si se proporciona", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarUsuarios("EMPLEADO", { dni: "30123456", page: 1, size: 10 });

      expect(axiosClient.get).toHaveBeenCalledWith("/empleados", {
        params: { dni: "30123456", page: 1, size: 10 },
      });
    });

    it("envía filtro nombre si se proporciona", async () => {
      axiosClient.get.mockResolvedValueOnce({ data: { contenido: [] } });

      await listarUsuarios("ADMINISTRADOR", { nombre: "Gómez" });

      expect(axiosClient.get).toHaveBeenCalledWith("/administradores", {
        params: { nombre: "Gómez", page: 0, size: 20 },
      });
    });

    it("lanza error si el tipo de usuario no es válido", async () => {
      await expect(listarUsuarios("INVALIDO")).rejects.toThrow("Tipo de usuario desconocido");
    });
  });

  describe("obtenerUsuario", () => {
    it("consulta el detalle en /{recurso}/{id}", async () => {
      const socioMock = { id: 12, nombre: "Ana", apellido: "Gómez" };
      axiosClient.get.mockResolvedValueOnce({ data: socioMock });

      const res = await obtenerUsuario("SOCIO", 12);

      expect(axiosClient.get).toHaveBeenCalledWith("/socios/12");
      expect(res).toEqual(socioMock);
    });
  });

  describe("crearUsuario", () => {
    it("envía POST al endpoint del rol con los datos del usuario", async () => {
      const nuevo = { nombre: "Carlos", apellido: "Pérez", password: "Password123" };
      axiosClient.post.mockResolvedValueOnce({ data: { id: 99, ...nuevo } });

      const res = await crearUsuario("EMPLEADO", nuevo);

      expect(axiosClient.post).toHaveBeenCalledWith("/empleados", nuevo);
      expect(res.id).toBe(99);
    });
  });

  describe("actualizarUsuario", () => {
    it("envía PATCH a /{recurso}/{id} con los campos a modificar", async () => {
      const cambios = { calle: "Nueva Calle", numeroCalle: "456" };
      axiosClient.patch.mockResolvedValueOnce({ data: { id: 10, ...cambios } });

      const res = await actualizarUsuario("ADMINISTRADOR", 10, cambios);

      expect(axiosClient.patch).toHaveBeenCalledWith("/administradores/10", cambios);
      expect(res.id).toBe(10);
    });
  });

  describe("eliminarUsuario", () => {
    it("envía DELETE a /{recurso}/{id}", async () => {
      axiosClient.delete.mockResolvedValueOnce({ status: 204 });

      await eliminarUsuario("SOCIO", 5);

      expect(axiosClient.delete).toHaveBeenCalledWith("/socios/5");
    });
  });

  describe("imprimirFichaUsuario", () => {
    it("solicita el PDF en /{recurso}/{id}/pdf con responseType blob", async () => {
      const blobMock = new Blob(["fake-pdf"], { type: "application/pdf" });
      axiosClient.get.mockResolvedValueOnce({ data: blobMock });

      const res = await imprimirFichaUsuario("SOCIO", 12);

      expect(axiosClient.get).toHaveBeenCalledWith("/socios/12/pdf", {
        responseType: "blob",
      });
      expect(res).toBe(blobMock);
    });
  });
});
