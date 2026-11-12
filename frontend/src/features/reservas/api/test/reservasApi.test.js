import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import {
  consultarDisponibilidad,
  consultarInsumosDisponibles,
  crearReserva,
  obtenerMisReservas,
  buscarReservas,
  obtenerReserva,
  modificarReserva,
  previsualizarCancelacion,
  confirmarCancelacion,
  iniciarAlquiler,
  finalizarAlquiler,
} from "../reservasApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("reservasApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("consultarDisponibilidad", () => {
    it("llama a /reservas/disponibilidad con query params y devuelve data", async () => {
      const mockData = { fecha: "2026-10-20", canchas: [] };
      axiosClient.get.mockResolvedValueOnce({ data: mockData });

      const res = await consultarDisponibilidad({ fecha: "2026-10-20", superficie: "LADRILLO" });

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas/disponibilidad", {
        params: { fecha: "2026-10-20", superficie: "LADRILLO" },
      });
      expect(res).toEqual(mockData);
    });
  });

  describe("consultarInsumosDisponibles", () => {
    it("llama a /reservas/insumos-disponibles con fecha, horaInicio y duracionHoras", async () => {
      const mockInsumos = [{ insumoId: 1, nombre: "Tubo de pelotas", disponible: 5 }];
      axiosClient.get.mockResolvedValueOnce({ data: mockInsumos });

      const params = { fecha: "2026-10-20", horaInicio: "18:00", duracionHoras: 2 };
      const res = await consultarInsumosDisponibles(params);

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas/insumos-disponibles", { params });
      expect(res).toEqual(mockInsumos);
    });
  });

  describe("crearReserva", () => {
    it("crea una reserva enviando el payload a POST /reservas", async () => {
      const payload = {
        canchaId: 1,
        fecha: "2026-10-20",
        horaInicio: "18:00",
        duracionHoras: 2,
        acompaniantes: [],
      };
      axiosClient.post.mockResolvedValueOnce({ data: { id: 101, numero: "R-000101" } });

      const res = await crearReserva(payload);

      expect(axiosClient.post).toHaveBeenCalledWith("/reservas", payload);
      expect(res.numero).toBe("R-000101");
      expect(res.id).toBe(101);
    });
  });

  describe("obtenerMisReservas", () => {
    it("llama a /reservas/mis-reservas con filtros opcionales y paginación", async () => {
      const mockPage = { contenido: [], totalElementos: 0, totalPaginas: 0 };
      axiosClient.get.mockResolvedValueOnce({ data: mockPage });

      const params = { proximas: true, estado: "CONFIRMADA", page: 0, size: 10 };
      const res = await obtenerMisReservas(params);

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas/mis-reservas", { params });
      expect(res).toEqual(mockPage);
    });
  });

  describe("buscarReservas", () => {
    it("llama a GET /reservas con filtros de búsqueda para personal", async () => {
      const mockPage = { contenido: [], totalElementos: 0, totalPaginas: 0 };
      axiosClient.get.mockResolvedValueOnce({ data: mockPage });

      const params = { documento: "30123456", estado: "PENDIENTE_PAGO", page: 0, size: 20 };
      const res = await buscarReservas(params);

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas", { params });
      expect(res).toEqual(mockPage);
    });
  });

  describe("obtenerReserva", () => {
    it("obtiene el detalle de una reserva por su ID", async () => {
      const mockReserva = { id: 42, numero: "R-000042", estado: "CONFIRMADA" };
      axiosClient.get.mockResolvedValueOnce({ data: mockReserva });

      const res = await obtenerReserva(42);

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas/42");
      expect(res).toEqual(mockReserva);
    });
  });

  describe("modificarReserva", () => {
    it("envía actualización parcial por PATCH a /reservas/{id}", async () => {
      const payload = { canchaId: 2, horaInicio: "19:00" };
      const mockModificada = { id: 42, canchaId: 2, horaInicio: "19:00" };
      axiosClient.patch.mockResolvedValueOnce({ data: mockModificada });

      const res = await modificarReserva(42, payload);

      expect(axiosClient.patch).toHaveBeenCalledWith("/reservas/42", payload);
      expect(res).toEqual(mockModificada);
    });
  });

  describe("previsualizarCancelacion", () => {
    it("obtiene previsualización de cancelación en /reservas/{id}/cancelacion", async () => {
      const mockCancelacion = { aTiempo: true, montoReembolso: 8000, cargoCancelacion: 0 };
      axiosClient.get.mockResolvedValueOnce({ data: mockCancelacion });

      const res = await previsualizarCancelacion(101);

      expect(axiosClient.get).toHaveBeenCalledWith("/reservas/101/cancelacion");
      expect(res.aTiempo).toBe(true);
      expect(res.montoReembolso).toBe(8000);
    });
  });

  describe("confirmarCancelacion", () => {
    it("confirma la cancelación por POST a /reservas/{id}/cancelacion", async () => {
      const mockCancelada = { id: 101, estado: "CANCELADA" };
      axiosClient.post.mockResolvedValueOnce({ data: mockCancelada });

      const res = await confirmarCancelacion(101);

      expect(axiosClient.post).toHaveBeenCalledWith("/reservas/101/cancelacion");
      expect(res.estado).toBe("CANCELADA");
    });
  });

  describe("iniciarAlquiler", () => {
    it("inicia el alquiler por POST a /reservas/{id}/inicio (CU14)", async () => {
      const mockIniciada = { id: 101, estado: "EN_CURSO" };
      axiosClient.post.mockResolvedValueOnce({ data: mockIniciada });

      const res = await iniciarAlquiler(101);

      expect(axiosClient.post).toHaveBeenCalledWith("/reservas/101/inicio");
      expect(res.estado).toBe("EN_CURSO");
    });
  });

  describe("finalizarAlquiler", () => {
    it("finaliza el alquiler por POST a /reservas/{id}/finalizacion (CU15)", async () => {
      const mockFinalizada = { id: 101, estado: "FINALIZADA" };
      axiosClient.post.mockResolvedValueOnce({ data: mockFinalizada });

      const res = await finalizarAlquiler(101);

      expect(axiosClient.post).toHaveBeenCalledWith("/reservas/101/finalizacion");
      expect(res.estado).toBe("FINALIZADA");
    });
  });
});
