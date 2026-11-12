import { beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient from "@/config/axiosClient";
import {
  cargarResultado,
  crearTorneo,
  descargarPdfResultados,
  eliminarTorneo,
  generarFixture,
  inscribirTorneo,
  listarInscripciones,
  listarTorneos,
  modificarTorneo,
  obtenerPartidos,
  obtenerTorneo,
} from "../torneosApi";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("torneosApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("listarTorneos llama a GET /torneos", async () => {
    const torneosMock = [{ id: 1, nombre: "Torneo Abierto" }];
    axiosClient.get.mockResolvedValueOnce({ data: torneosMock });

    const data = await listarTorneos();

    expect(axiosClient.get).toHaveBeenCalledWith("/torneos");
    expect(data).toEqual(torneosMock);
  });

  it("obtenerTorneo llama a GET /torneos/:id", async () => {
    const torneoMock = { id: 1, nombre: "Torneo Abierto" };
    axiosClient.get.mockResolvedValueOnce({ data: torneoMock });

    const data = await obtenerTorneo(1);

    expect(axiosClient.get).toHaveBeenCalledWith("/torneos/1");
    expect(data).toEqual(torneoMock);
  });

  it("crearTorneo llama a POST /torneos", async () => {
    const nuevo = { nombre: "Nuevo Torneo", categoria: "MASCULINO", modalidad: "SINGLE" };
    axiosClient.post.mockResolvedValueOnce({ data: { id: 2, ...nuevo } });

    const data = await crearTorneo(nuevo);

    expect(axiosClient.post).toHaveBeenCalledWith("/torneos", nuevo);
    expect(data.id).toBe(2);
  });

  it("modificarTorneo llama a PUT /torneos/:id", async () => {
    const edicion = { nombre: "Torneo Modificado" };
    axiosClient.put.mockResolvedValueOnce({ data: { id: 1, ...edicion } });

    const data = await modificarTorneo(1, edicion);

    expect(axiosClient.put).toHaveBeenCalledWith("/torneos/1", edicion);
    expect(data.nombre).toBe("Torneo Modificado");
  });

  it("eliminarTorneo llama a DELETE /torneos/:id", async () => {
    axiosClient.delete.mockResolvedValueOnce({});

    await eliminarTorneo(1);

    expect(axiosClient.delete).toHaveBeenCalledWith("/torneos/1");
  });

  it("listarInscripciones llama a GET /torneos/:id/inscripciones", async () => {
    const inscripcionesMock = [{ id: 10, jugador1Id: 5 }];
    axiosClient.get.mockResolvedValueOnce({ data: inscripcionesMock });

    const data = await listarInscripciones(1);

    expect(axiosClient.get).toHaveBeenCalledWith("/torneos/1/inscripciones");
    expect(data).toEqual(inscripcionesMock);
  });

  it("inscribirTorneo llama a POST /torneos/:id/inscripciones", async () => {
    const inscripcionReq = { jugador1Id: 5, parejaId: null };
    axiosClient.post.mockResolvedValueOnce({ data: { id: 10, ...inscripcionReq } });

    const data = await inscribirTorneo(1, inscripcionReq);

    expect(axiosClient.post).toHaveBeenCalledWith("/torneos/1/inscripciones", inscripcionReq);
    expect(data.id).toBe(10);
  });

  it("generarFixture llama a POST /torneos/:id/fixture", async () => {
    const fixtureMock = [{ id: 100, ronda: 1 }];
    axiosClient.post.mockResolvedValueOnce({ data: fixtureMock });

    const data = await generarFixture(1);

    expect(axiosClient.post).toHaveBeenCalledWith("/torneos/1/fixture");
    expect(data).toEqual(fixtureMock);
  });

  it("obtenerPartidos llama a GET /torneos/:id/partidos", async () => {
    const partidosMock = [{ id: 100, ronda: 1 }];
    axiosClient.get.mockResolvedValueOnce({ data: partidosMock });

    const data = await obtenerPartidos(1);

    expect(axiosClient.get).toHaveBeenCalledWith("/torneos/1/partidos");
    expect(data).toEqual(partidosMock);
  });

  it("cargarResultado llama a POST /torneos/:id/partidos/:partidoId/resultado con objeto sets", async () => {
    const partidoMock = { id: 100, sets: "6-4 6-3" };
    axiosClient.post.mockResolvedValueOnce({ data: partidoMock });

    const data = await cargarResultado(1, 100, "6-4 6-3");

    expect(axiosClient.post).toHaveBeenCalledWith("/torneos/1/partidos/100/resultado", {
      sets: "6-4 6-3",
    });
    expect(data).toEqual(partidoMock);
  });

  it("descargarPdfResultados llama a GET /torneos/:id/resultados/pdf con responseType blob", async () => {
    const blobMock = new Blob(["%PDF-1.4"], { type: "application/pdf" });
    axiosClient.get.mockResolvedValueOnce({ data: blobMock });

    const data = await descargarPdfResultados(1);

    expect(axiosClient.get).toHaveBeenCalledWith("/torneos/1/resultados/pdf", {
      responseType: "blob",
    });
    expect(data).toEqual(blobMock);
  });
});
