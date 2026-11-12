import { describe, expect, it } from "vitest";
import {
  alternar,
  aMarcas,
  contarAusentes,
  contarPresentes,
  diaDeSemanaIso,
  esHorarioNocturno,
  fechaSugeridaAsistencia,
  filasDesdePlanilla,
  formatearFechaLocal,
  marcarTodos,
  porcentajePresentes,
  restarDias,
  ultimaFechaDictado,
  validarFechaAsistencia,
} from "../asistencia";

const filas = [
  { socioId: 1, nombre: "Tealdi, Bruno", presente: true },
  { socioId: 2, nombre: "Bermudez, Lucia", presente: false },
  { socioId: 3, nombre: "Miranda, Diego", presente: true },
];

describe("modelo de asistencia", () => {
  describe("filasDesdePlanilla", () => {
    it("arma las filas a partir de la lista plana que devuelve el backend", () => {
      const r = filasDesdePlanilla([{ actividadId: 5, socioId: 7, socio: "Paz, Ana", fecha: "2026-11-02", presente: true }]);
      expect(r).toEqual([{ socioId: 7, nombre: "Paz, Ana", presente: true }]);
    });

    it("devuelve vacio si la planilla no es un arreglo", () => {
      expect(filasDesdePlanilla({})).toEqual([]);
      expect(filasDesdePlanilla(null)).toEqual([]);
    });

    it("toma como ausente lo que no venga marcado", () => {
      const r = filasDesdePlanilla([{ socioId: 1, socio: "Paz, Ana" }]);
      expect(r[0].presente).toBe(false);
    });
  });

  describe("aMarcas", () => {
    it("manda solo el socio y si estuvo", () => {
      expect(aMarcas(filas)).toEqual([
        { socioId: 1, presente: true },
        { socioId: 2, presente: false },
        { socioId: 3, presente: true },
      ]);
    });
  });

  describe("conteos", () => {
    it("cuenta presentes y ausentes", () => {
      expect(contarPresentes(filas)).toBe(2);
      expect(contarAusentes(filas)).toBe(1);
    });

    it("calcula el porcentaje de asistencia", () => {
      expect(porcentajePresentes(filas)).toBe(67);
    });

    it("no divide por cero con la planilla vacia", () => {
      expect(porcentajePresentes([])).toBe(0);
    });
  });

  describe("marcarTodos", () => {
    it("marca a todos presentes sin tocar el resto de los datos", () => {
      const r = marcarTodos(filas, true);
      expect(r.every((f) => f.presente)).toBe(true);
      expect(r[1].nombre).toBe("Bermudez, Lucia");
    });

    it("marca a todos ausentes", () => {
      expect(marcarTodos(filas, false).every((f) => !f.presente)).toBe(true);
    });

    it("no muta el arreglo original", () => {
      marcarTodos(filas, false);
      expect(filas[0].presente).toBe(true);
    });
  });

  describe("alternar", () => {
    it("cambia solo la marca del alumno indicado", () => {
      const r = alternar(filas, 2);
      expect(r[1].presente).toBe(true);
      expect(r[0].presente).toBe(true);
      expect(r[2].presente).toBe(true);
    });

    it("no hace nada si el socio no esta en la planilla", () => {
      expect(alternar(filas, 999)).toEqual(filas);
    });
  });

  describe("diaDeSemanaIso", () => {
    it("devuelve el dia de semana en ingles compatible con Java DayOfWeek", () => {
      // 2026-11-02 fue Lunes
      expect(diaDeSemanaIso("2026-11-02")).toBe("MONDAY");
      // 2026-11-05 fue Jueves
      expect(diaDeSemanaIso("2026-11-05")).toBe("THURSDAY");
    });
  });

  describe("validarFechaAsistencia", () => {
    const act = {
      id: 3,
      fechaInicio: "2026-10-26",
      fechaFin: "2026-12-02",
      dias: ["MONDAY", "WEDNESDAY"],
    };

    it("rechaza si la fecha es futura", () => {
      const res = validarFechaAsistencia(act, "2026-11-06", "2026-11-05");
      expect(res.valida).toBe(false);
      expect(res.motivo).toMatch(/futura/i);
    });

    it("rechaza si la fecha es anterior al inicio de la actividad", () => {
      const res = validarFechaAsistencia(act, "2026-10-19", "2026-11-05");
      expect(res.valida).toBe(false);
      expect(res.motivo).toMatch(/anterior al inicio/i);
    });

    it("rechaza si la fecha es posterior al fin de la actividad", () => {
      const res = validarFechaAsistencia(act, "2026-12-07", "2026-12-31");
      expect(res.valida).toBe(false);
      expect(res.motivo).toMatch(/finalizó/i);
    });

    it("rechaza si la fecha no coincide con los dias de dictado", () => {
      // 2026-11-05 es Jueves (THURSDAY), la actividad se dicta Lunes y Miércoles
      const res = validarFechaAsistencia(act, "2026-11-05", "2026-11-05");
      expect(res.valida).toBe(false);
      expect(res.motivo).toMatch(/no es un día de dictado/i);
      expect(res.motivo).toMatch(/lunes, miércoles/i);
    });

    it("informa claramente si la actividad todavia no comenzo", () => {
      const actFutura = { ...act, fechaInicio: "2026-11-09" };
      const res = validarFechaAsistencia(actFutura, "2026-11-05", "2026-11-05");
      expect(res.valida).toBe(false);
      expect(res.motivo).toMatch(/todavía no comenzó/i);
      expect(res.motivo).toMatch(/09\/11\/2026/);
    });

    it("acepta si la fecha coincide con el dia de dictado y esta en vigencia", () => {
      // 2026-11-02 es Lunes (MONDAY)
      const res = validarFechaAsistencia(act, "2026-11-02", "2026-11-05");
      expect(res.valida).toBe(true);
      expect(res.motivo).toBeUndefined();
    });
  });

  describe("utilidades de fecha y asistencia inteligente", () => {
    it("formatearFechaLocal formatea a DD/MM/AAAA", () => {
      expect(formatearFechaLocal("2026-11-05")).toBe("05/11/2026");
      expect(formatearFechaLocal("")).toBe("—");
    });

    it("restarDias calcula correctamente días anteriores", () => {
      expect(restarDias("2026-11-05", 1)).toBe("2026-11-04");
      expect(restarDias("2026-11-01", 1)).toBe("2026-10-31");
    });

    it("esHorarioNocturno detecta clases que terminan a las 19hs o más tarde", () => {
      expect(esHorarioNocturno("18:30")).toBe(false);
      expect(esHorarioNocturno("19:00")).toBe(true);
      expect(esHorarioNocturno("21:30:00")).toBe(true);
    });

    it("ultimaFechaDictado encuentra la última fecha dictada hacia atrás", () => {
      const act = {
        fechaInicio: "2026-10-01",
        fechaFin: "2026-12-31",
        dias: ["MONDAY", "WEDNESDAY"],
      };
      // Desde el jueves 05/11/2026, la última fue el miércoles 04/11/2026
      expect(ultimaFechaDictado(act, "2026-11-05")).toBe("2026-11-04");
      // Desde el lunes 02/11/2026, la última fue el miércoles 28/10/2026
      expect(ultimaFechaDictado(act, "2026-11-02")).toBe("2026-10-28");
    });

    describe("fechaSugeridaAsistencia", () => {
      const actNocturna = {
        fechaInicio: "2026-10-01",
        fechaFin: "2026-12-31",
        horaInicio: "20:00:00",
        horaFin: "21:30:00",
        dias: ["WEDNESDAY"], // Se dicta Miércoles
      };

      it("sugiere hoy si hoy es día de dictado", () => {
        // Miércoles 2026-11-04
        const sug = fechaSugeridaAsistencia(actNocturna, "2026-11-04", 10);
        expect(sug.fecha).toBe("2026-11-04");
        expect(sug.esPeriodoGracia).toBe(false);
      });

      it("sugiere ayer con período de gracia si anoche hubo clase nocturna y son menos de las 14hs", () => {
        // Jueves 2026-11-05 a las 9 am (anoche fue miércoles y terminó a las 21:30)
        const sug = fechaSugeridaAsistencia(actNocturna, "2026-11-05", 9);
        expect(sug.fecha).toBe("2026-11-04");
        expect(sug.esPeriodoGracia).toBe(true);
        expect(sug.motivo).toMatch(/período de gracia/i);
      });

      it("no sugiere ayer si ya pasaron las 14hs del día siguiente", () => {
        // Jueves 2026-11-05 a las 15 hs
        const sug = fechaSugeridaAsistencia(actNocturna, "2026-11-05", 15);
        expect(sug.fecha).toBe("2026-11-05");
        expect(sug.esPeriodoGracia).toBe(false);
      });

      it("no activa período de gracia si la clase de ayer fue matutina/diurna", () => {
        const actDiurna = { ...actNocturna, horaFin: "12:00:00" };
        const sug = fechaSugeridaAsistencia(actDiurna, "2026-11-05", 9);
        expect(sug.fecha).toBe("2026-11-05");
        expect(sug.esPeriodoGracia).toBe(false);
      });
    });
  });
});
