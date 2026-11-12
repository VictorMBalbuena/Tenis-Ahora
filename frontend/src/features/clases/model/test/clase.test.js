import { describe, expect, it } from "vitest";
import {
  CUPO_MAXIMO_INSTITUCIONAL,
  DiaSemana,
  EstadoActividad,
  Modalidad,
  NivelClase,
  admiteInscripcion,
  cupoCompleto,
  diasCortos,
  diasTexto,
  fechaCorta,
  horario,
  lugaresLibres,
  nombreCancha,
  nombreProfesor,
  porcentajeOcupado,
  resumirClases,
  validarClase,
  vigencia,
  yaPaso,
} from "../clase";

const clase = (campos) => ({
  id: 1,
  fechaInicio: "2026-11-02",
  fechaFin: "2027-03-02",
  dias: [DiaSemana.MONDAY, DiaSemana.WEDNESDAY],
  horaInicio: "18:00:00",
  horaFin: "20:00:00",
  cupoMaximo: 20,
  cupoOcupado: 14,
  cupoDisponible: 6,
  estado: EstadoActividad.PROGRAMADA,
  nivel: NivelClase.INICIAL,
  modalidad: Modalidad.GRUPAL,
  profesor: "Tealdi, Bruno",
  cancha: "Cancha 1",
  ...campos,
});

const formulario = (campos) => ({
  nombre: "Tenis para principiantes",
  profesorId: "3",
  canchaId: "2",
  nivel: NivelClase.INICIAL,
  modalidad: Modalidad.GRUPAL,
  fechaInicio: "2026-11-02",
  fechaFin: "2027-03-02",
  dias: [DiaSemana.MONDAY],
  horaInicio: "18:00",
  horaFin: "20:00",
  cupoMaximo: "20",
  ...campos,
});

describe("modelo de clase", () => {
  describe("horario", () => {
    it("recorta los segundos que manda el backend", () => {
      expect(horario(clase())).toBe("18:00 a 20:00");
    });

    it("no rompe si falta una hora", () => {
      expect(horario(clase({ horaFin: null }))).toBe("18:00 a —");
    });
  });

  describe("vigencia y diasCortos", () => {
    it("muestra el rango de fechas", () => {
      expect(vigencia(clase())).toBe("02/11/2026 a 02/03/2027");
    });

    it("ordena los dias de lunes a domingo, sin importar el orden del backend", () => {
      expect(diasCortos(clase({ dias: [DiaSemana.WEDNESDAY, DiaSemana.MONDAY] }))).toBe("Lu, Mi");
    });

    it("muestra un guion sin dias cargados", () => {
      expect(diasCortos(clase({ dias: [] }))).toBe("—");
    });
  });

  describe("lugaresLibres y cupoCompleto", () => {
    it("usa el cupo disponible que calcula el backend", () => {
      expect(lugaresLibres(clase())).toBe(6);
      expect(cupoCompleto(clase())).toBe(false);
    });

    it("detecta el cupo completo", () => {
      const llena = clase({ cupoOcupado: 20, cupoDisponible: 0 });
      expect(lugaresLibres(llena)).toBe(0);
      expect(cupoCompleto(llena)).toBe(true);
    });
  });

  describe("porcentajeOcupado", () => {
    it("calcula la proporcion del cupo", () => {
      expect(porcentajeOcupado(clase({ cupoOcupado: 10, cupoMaximo: 20 }))).toBe(50);
    });

    it("no divide por cero si no hay cupo cargado", () => {
      expect(porcentajeOcupado(clase({ cupoMaximo: 0 }))).toBe(0);
    });

    it("no pasa de 100", () => {
      expect(porcentajeOcupado(clase({ cupoOcupado: 30, cupoMaximo: 20 }))).toBe(100);
    });
  });

  describe("yaPaso y admiteInscripcion", () => {
    it("una clase finalizada ya paso", () => {
      expect(yaPaso(clase({ estado: EstadoActividad.FINALIZADA }))).toBe(true);
    });

    it("una clase cancelada ya paso", () => {
      expect(yaPaso(clase({ estado: EstadoActividad.CANCELADA }))).toBe(true);
    });

    it("una clase programada todavia no paso", () => {
      expect(yaPaso(clase())).toBe(false);
    });

    it("no admite inscripcion con el cupo completo", () => {
      expect(admiteInscripcion(clase({ cupoOcupado: 20, cupoDisponible: 0 }))).toBe(false);
    });

    it("no admite inscripcion si no esta programada", () => {
      expect(admiteInscripcion(clase({ estado: EstadoActividad.EN_CURSO }))).toBe(false);
    });

    it("admite inscripcion programada y con lugar", () => {
      expect(admiteInscripcion(clase())).toBe(true);
    });
  });

  describe("nombreProfesor y nombreCancha", () => {
    it("usa el nombre que ya manda formateado el backend", () => {
      expect(nombreProfesor(clase())).toBe("Tealdi, Bruno");
    });

    it("muestra un guion si la clase no trae profesor", () => {
      expect(nombreProfesor(clase({ profesor: null }))).toBe("—");
    });

    it("usa el nombre de la cancha", () => {
      expect(nombreCancha(clase())).toBe("Cancha 1");
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

  describe("validarClase", () => {
    it("acepta un formulario completo", () => {
      expect(validarClase(formulario())).toEqual({});
    });

    it("exige nombre, profesor, cancha, nivel, modalidad, fechas y dias", () => {
      const e = validarClase(
        formulario({
          nombre: "",
          profesorId: "",
          canchaId: "",
          nivel: "",
          modalidad: "",
          fechaInicio: "",
          fechaFin: "",
          dias: [],
        }),
      );
      expect(Object.keys(e).sort()).toEqual([
        "canchaId",
        "dias",
        "fechaFin",
        "fechaInicio",
        "modalidad",
        "nivel",
        "nombre",
        "profesorId",
      ]);
    });

    it("rechaza una fecha de fin anterior o igual a la de inicio", () => {
      expect(validarClase(formulario({ fechaFin: "2026-10-01" })).fechaFin).toBeDefined();
      expect(validarClase(formulario({ fechaFin: "2026-11-02" })).fechaFin).toBeDefined();
    });

    it("rechaza una hora de fin anterior o igual al inicio", () => {
      expect(validarClase(formulario({ horaFin: "17:00" })).horaFin).toBeDefined();
      expect(validarClase(formulario({ horaFin: "18:00" })).horaFin).toBeDefined();
    });

    it("rechaza un cupo menor a 1", () => {
      expect(validarClase(formulario({ cupoMaximo: "0" })).cupoMaximo).toBeDefined();
    });

    it("rechaza un cupo por encima del limite del club (RD01)", () => {
      const e = validarClase(formulario({ cupoMaximo: String(CUPO_MAXIMO_INSTITUCIONAL + 1) }));
      expect(e.cupoMaximo).toContain(String(CUPO_MAXIMO_INSTITUCIONAL));
    });

    it("acepta exactamente el limite del club", () => {
      expect(validarClase(formulario({ cupoMaximo: String(CUPO_MAXIMO_INSTITUCIONAL) }))).toEqual({});
    });
  });

  describe("resumirClases", () => {
    const lista = [
      clase({ id: 1, cupoOcupado: 14, cupoDisponible: 6 }),
      clase({ id: 2, cupoOcupado: 20, cupoDisponible: 0 }),
      clase({ id: 3, cupoOcupado: 0, cupoDisponible: 20 }),
    ];

    it("suma los inscriptos de la pagina", () => {
      expect(resumirClases(lista).inscriptos).toBe(34);
    });

    it("cuenta las completas y las que tienen lugar", () => {
      const r = resumirClases(lista);
      expect(r.completas).toBe(1);
      expect(r.conLugar).toBe(2);
    });

    it("toma el total del backend si se lo pasan", () => {
      expect(resumirClases(lista, 47).total).toBe(47);
    });
  });

  describe("diasTexto y nombreCancha helpers", () => {
    it("diasTexto devuelve los nombres abreviados de los días en orden", () => {
      const clase = { dias: ["WEDNESDAY", "MONDAY"] };
      expect(diasTexto(clase)).toBe("Lun, Mié");
    });

    it("diasTexto devuelve guión medio si no hay días", () => {
      expect(diasTexto({})).toBe("—");
    });

    it("nombreCancha soporta tanto string como objeto cancha", () => {
      expect(nombreCancha({ cancha: "Cancha Central" })).toBe("Cancha Central");
      expect(nombreCancha({ cancha: { nombre: "Cancha 2" } })).toBe("Cancha 2");
      expect(nombreCancha({ cancha: { identificador: "3" } })).toBe("Cancha 3");
      expect(nombreCancha({})).toBe("—");
    });
  });
});
