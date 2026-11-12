import { describe, expect, it } from "vitest";
import {
  CUPO_MAXIMO_INSTITUCIONAL,
  EstadoActividad,
  Modalidad,
  admiteAsistencia,
  admiteInscripcion,
  cupoCompleto,
  fechaCorta,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreEntrenador,
  porcentajeOcupado,
  resumirEntrenamientos,
  validarEntrenamiento,
  yaPaso,
} from "../entrenamiento";

const entrenamiento = (campos) => ({
  id: 1,
  fechaInicio: "2026-11-01",
  fechaFin: "2026-11-30",
  dias: ["MONDAY", "WEDNESDAY", "FRIDAY"],
  horaInicio: "07:00:00",
  horaFin: "09:00:00",
  cupoMaximo: 12,
  inscriptos: 9,
  objetivo: "Pretemporada",
  modalidad: Modalidad.GRUPAL,
  estado: EstadoActividad.PROGRAMADA,
  entrenador: { id: 5, nombre: "Ezequiel", apellido: "Roldan" },
  cancha: { id: 2, identificador: "Cancha 3" },
  ...campos,
});

const formulario = (campos) => ({
  nombre: "Entrenamiento de Tenis",
  entrenadorId: "5",
  canchaId: "2",
  objetivo: "Pretemporada",
  modalidad: Modalidad.GRUPAL,
  fechaInicio: "2026-11-01",
  fechaFin: "2026-11-30",
  dias: ["MONDAY"],
  horaInicio: "07:00",
  horaFin: "09:00",
  cupoMaximo: "12",
  ...campos,
});

describe("modelo de entrenamiento", () => {
  describe("horario", () => {
    it("recorta los segundos que manda el backend", () => {
      expect(horario(entrenamiento())).toBe("07:00 a 09:00");
    });
  });

  describe("cupo", () => {
    it("calcula los lugares libres", () => {
      expect(lugaresLibres(entrenamiento())).toBe(3);
      expect(cupoCompleto(entrenamiento())).toBe(false);
    });

    it("calcula los lugares libres usando cupoMaximo - cupoOcupado cuando cupoDisponible es undefined", () => {
      const e = { cupoMaximo: 8, inscriptos: 3 };
      expect(lugaresLibres(e)).toBe(5);
      expect(cupoCompleto(e)).toBe(false);
    });

    it("prioriza cupoDisponible si viene del backend", () => {
      const e = { cupoMaximo: 10, cupoOcupado: 2, cupoDisponible: 4 };
      expect(lugaresLibres(e)).toBe(4);
    });

    it("detecta el cupo completo", () => {
      expect(cupoCompleto(entrenamiento({ inscriptos: 12 }))).toBe(true);
    });

    it("no devuelve lugares negativos", () => {
      expect(lugaresLibres(entrenamiento({ inscriptos: 20 }))).toBe(0);
    });

    it("calcula el porcentaje sin pasarse de 100", () => {
      expect(porcentajeOcupado(entrenamiento({ inscriptos: 6, cupoMaximo: 12 }))).toBe(50);
      expect(porcentajeOcupado(entrenamiento({ inscriptos: 20, cupoMaximo: 12 }))).toBe(100);
      expect(porcentajeOcupado(entrenamiento({ cupoMaximo: 0 }))).toBe(0);
    });
  });

  describe("estados y permisos", () => {
    it("admite asistencia para estados PROGRAMADA, EN_CURSO y FINALIZADA", () => {
      expect(admiteAsistencia({ estado: EstadoActividad.PROGRAMADA })).toBe(true);
      expect(admiteAsistencia({ estado: EstadoActividad.EN_CURSO })).toBe(true);
      expect(admiteAsistencia({ estado: EstadoActividad.FINALIZADA })).toBe(true);
    });

    it("no admite asistencia si el entrenamiento esta cancelado o es nulo", () => {
      expect(admiteAsistencia({ estado: EstadoActividad.CANCELADA })).toBe(false);
      expect(admiteAsistencia(null)).toBe(false);
    });

    it("admite inscripcion si esta programado o en curso y tiene lugar", () => {
      expect(admiteInscripcion({ estado: EstadoActividad.PROGRAMADA, cupoMaximo: 10, cupoOcupado: 5 })).toBe(true);
      expect(admiteInscripcion({ estado: EstadoActividad.EN_CURSO, cupoMaximo: 10, cupoOcupado: 5 })).toBe(true);
    });

    it("no admite inscripcion si esta finalizado, cancelado o con cupo completo", () => {
      expect(admiteInscripcion({ estado: EstadoActividad.FINALIZADA, cupoMaximo: 10, cupoOcupado: 5 })).toBe(false);
      expect(admiteInscripcion({ estado: EstadoActividad.CANCELADA, cupoMaximo: 10, cupoOcupado: 5 })).toBe(false);
      expect(admiteInscripcion({ estado: EstadoActividad.PROGRAMADA, cupoMaximo: 10, cupoOcupado: 10 })).toBe(false);
    });
  });

  describe("nombres", () => {
    it("arma el nombre del entrenador", () => {
      expect(nombreEntrenador(entrenamiento())).toBe("Ezequiel Roldan");
    });

    it("muestra un guion si no trae entrenador", () => {
      expect(nombreEntrenador(entrenamiento({ entrenador: null }))).toBe("—");
    });

    it("usa el identificador de la cancha", () => {
      expect(nombreCancha(entrenamiento())).toBe("Cancha 3");
    });
  });

  describe("fechaCorta", () => {
    it("pasa la fecha ISO a formato local", () => {
      expect(fechaCorta("2026-11-20")).toBe("20/11/2026");
    });

    it("muestra un guion si no hay fecha", () => {
      expect(fechaCorta(null)).toBe("—");
    });
  });

  describe("validarEntrenamiento", () => {
    it("acepta un formulario completo", () => {
      expect(validarEntrenamiento(formulario())).toEqual({});
    });

    it("exige campos obligatorios si faltan", () => {
      const e = validarEntrenamiento({
        nombre: "",
        entrenadorId: "",
        canchaId: "",
        modalidad: "",
        objetivo: "",
        fechaInicio: "",
        fechaFin: "",
        dias: [],
        horaInicio: "",
        horaFin: "",
        cupoMaximo: "",
      });
      expect(Object.keys(e).sort()).toEqual([
        "canchaId",
        "cupoMaximo",
        "dias",
        "entrenadorId",
        "fechaFin",
        "fechaInicio",
        "horaFin",
        "horaInicio",
        "modalidad",
        "nombre",
        "objetivo",
      ]);
    });

    it("no acepta un objetivo que sean solo espacios", () => {
      expect(validarEntrenamiento(formulario({ objetivo: "   " })).objetivo).toBeDefined();
    });

    it("rechaza una hora de fin anterior o igual al inicio", () => {
      expect(validarEntrenamiento(formulario({ horaFin: "06:00" })).horaFin).toBeDefined();
      expect(validarEntrenamiento(formulario({ horaFin: "07:00" })).horaFin).toBeDefined();
    });

    it("rechaza un cupo por encima del limite del club (RD01)", () => {
      const e = validarEntrenamiento(formulario({ cupoMaximo: String(CUPO_MAXIMO_INSTITUCIONAL + 1) }));
      expect(e.cupoMaximo).toContain(String(CUPO_MAXIMO_INSTITUCIONAL));
    });

    it("acepta exactamente el limite del club", () => {
      expect(validarEntrenamiento(formulario({ cupoMaximo: String(CUPO_MAXIMO_INSTITUCIONAL) }))).toEqual({});
    });
  });

  describe("resumirEntrenamientos", () => {
    const lista = [
      entrenamiento({ id: 1, inscriptos: 9 }),
      entrenamiento({ id: 2, inscriptos: 12 }),
      entrenamiento({ id: 3, inscriptos: 0 }),
    ];

    it("suma los inscriptos de la pagina", () => {
      expect(resumirEntrenamientos(lista).inscriptos).toBe(21);
    });

    it("cuenta los completos y los que tienen lugar", () => {
      const r = resumirEntrenamientos(lista);
      expect(r.completos).toBe(1);
      expect(r.conLugar).toBe(2);
    });

    it("toma el total del backend si se lo pasan", () => {
      expect(resumirEntrenamientos(lista, 31).total).toBe(31);
    });
  });
});
