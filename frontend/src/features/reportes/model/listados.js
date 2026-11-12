import { ETIQUETA_NIVEL, ETIQUETA_ESTADO as ESTADO_CLASE, diasCortos, horario, vigencia } from "@/features/clases/model/clase";
import { ETIQUETA_ESTADO as ESTADO_ENTRENAMIENTO, diasTexto, rangoFechas } from "@/features/entrenamientos/model/entrenamiento";
import { ETIQUETA_CONCEPTO, ETIQUETA_MEDIO, pesos, fechaCorta, fechaHora } from "@/features/cobros/model/cobro";
import { ETIQUETA_TIPO as TIPO_DESCUENTO, beneficio } from "@/features/descuentos/model/descuento";
import { etiquetaSuperficie } from "@/features/canchas/model/cancha";

/**
 * Los diez listados imprimibles que piden los casos de uso de cada módulo.
 *
 * Cada uno declara su título, sus columnas y cómo pasar de lo que devuelve el
 * servidor a las filas del impreso. `mapear` no toca la red ni el DOM: recibe
 * los datos y devuelve un array de arrays de texto, uno por fila.
 *
 * Que sea texto y no JSX es a propósito: el impreso es una tabla y nada más,
 * y así el armado de cada listado se puede probar sin montar un componente.
 */

const RAYA = "—";

/** Un valor que puede faltar, listo para imprimir. */
const texto = (valor) => {
  if (valor == null || valor === "") return RAYA;
  return String(valor);
};

/** Un estado que viene del backend en mayúsculas (PAGADO, ANULADO) legible. */
function comoTitulo(valor) {
  if (!valor) return RAYA;
  const limpio = String(valor).replaceAll("_", " ").toLowerCase();
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
}

const nombreCompleto = (persona) => `${persona.apellido ?? ""}, ${persona.nombre ?? ""}`.replace(/^, |, $/g, "") || RAYA;

const siNo = (valor) => (valor ? "Sí" : "No");

const activoONo = (persona) => (persona.activo ? "Activo" : "Dado de baja");

/** El cupo de una actividad: ocupados sobre el máximo. */
const cupo = (actividad) => `${actividad.cupoOcupado ?? 0} / ${actividad.cupoMaximo ?? RAYA}`;

const DERECHA = "derecha";

export const LISTADOS = [
  {
    id: "usuarios",
    titulo: "Datos de usuarios",
    descripcion: "Socios, empleados y administradores",
    columnas: [
      { titulo: "Apellido y nombre" },
      { titulo: "DNI" },
      { titulo: "Correo electrónico" },
      { titulo: "Rol" },
      { titulo: "Estado" },
    ],
    mapear: (personas) =>
      personas.map((p) => [nombreCompleto(p), texto(p.dni), texto(p.email), comoTitulo(p.rol), activoONo(p)]),
  },
  {
    id: "canchas",
    titulo: "Tipos de cancha y características",
    descripcion: "Superficie, capacidad y precio por hora",
    columnas: [
      { titulo: "Cancha" },
      { titulo: "Superficie" },
      { titulo: "Capacidad", alineacion: DERECHA },
      { titulo: "Precio por hora", alineacion: DERECHA },
      { titulo: "Estado" },
    ],
    mapear: (canchas) =>
      canchas.map((c) => [
        texto(c.identificador),
        etiquetaSuperficie(c.tipoSuperficie),
        texto(c.capacidad),
        pesos(c.precioPorHora),
        c.enMantenimiento ? "En mantenimiento" : c.activa ? "Disponible" : "Deshabilitada",
      ]),
  },
  {
    id: "profesores",
    titulo: "Datos de profesores",
    descripcion: "Título habilitante y estado de validación",
    columnas: [
      { titulo: "Apellido y nombre" },
      { titulo: "DNI" },
      { titulo: "Título habilitante" },
      { titulo: "Valor por hora", alineacion: DERECHA },
      { titulo: "Validación" },
      { titulo: "Estado" },
    ],
    mapear: (profesores) =>
      profesores.map((p) => [
        nombreCompleto(p),
        texto(p.dni),
        texto(p.tituloHabilitante),
        pesos(p.valorPorHora),
        comoTitulo(p.estadoValidacion),
        activoONo(p),
      ]),
  },
  {
    id: "entrenadores",
    titulo: "Datos de entrenadores",
    descripcion: "Certificación deportiva y especialidad",
    columnas: [
      { titulo: "Apellido y nombre" },
      { titulo: "DNI" },
      { titulo: "Certificación" },
      { titulo: "Especialidad" },
      { titulo: "Valor por hora", alineacion: DERECHA },
      { titulo: "Validación" },
      { titulo: "Estado" },
    ],
    mapear: (entrenadores) =>
      entrenadores.map((e) => [
        nombreCompleto(e),
        texto(e.dni),
        texto(e.certificacionDeportiva),
        texto(e.especialidad),
        pesos(e.valorPorHora),
        comoTitulo(e.estadoValidacion),
        activoONo(e),
      ]),
  },
  {
    id: "clases",
    titulo: "Clases programadas",
    descripcion: "Profesor, cancha, días y cupo de cada clase",
    columnas: [
      { titulo: "Código" },
      { titulo: "Clase" },
      { titulo: "Nivel" },
      { titulo: "Profesor" },
      { titulo: "Cancha" },
      { titulo: "Días" },
      { titulo: "Horario" },
      { titulo: "Vigencia" },
      { titulo: "Cupo", alineacion: DERECHA },
      { titulo: "Estado" },
    ],
    mapear: (clases) =>
      clases.map((c) => [
        texto(c.codigo),
        texto(c.nombre),
        texto(ETIQUETA_NIVEL[c.nivel] ?? c.nivel),
        texto(c.profesor),
        texto(c.cancha),
        diasCortos(c),
        horario(c),
        vigencia(c),
        cupo(c),
        texto(ESTADO_CLASE[c.estado] ?? comoTitulo(c.estado)),
      ]),
  },
  {
    id: "entrenamientos",
    titulo: "Entrenamientos programados",
    descripcion: "Entrenador, cancha, días y cupo de cada entrenamiento",
    columnas: [
      { titulo: "Código" },
      { titulo: "Entrenamiento" },
      { titulo: "Objetivo" },
      { titulo: "Entrenador" },
      { titulo: "Cancha" },
      { titulo: "Días" },
      { titulo: "Horario" },
      { titulo: "Vigencia" },
      { titulo: "Cupo", alineacion: DERECHA },
      { titulo: "Estado" },
    ],
    mapear: (entrenamientos) =>
      entrenamientos.map((e) => [
        texto(e.codigo),
        texto(e.nombre),
        texto(e.objetivo),
        texto(e.entrenador),
        texto(e.cancha),
        diasTexto(e),
        horario(e),
        rangoFechas(e),
        cupo(e),
        texto(ESTADO_ENTRENAMIENTO[e.estado] ?? comoTitulo(e.estado)),
      ]),
  },
  {
    id: "partidos",
    titulo: "Resultados de partidos",
    descripcion: "Todos los partidos de torneos y ligas, jugados o por jugar",
    columnas: [
      { titulo: "Competencia" },
      { titulo: "Instancia" },
      { titulo: "Partido" },
      { titulo: "Fecha" },
      { titulo: "Sets" },
      { titulo: "Resultado" },
    ],
    mapear: (partidos) =>
      partidos.map((p) => [
        texto(p.torneo?.nombre),
        // En una liga las vueltas son fechas; en un torneo, rondas.
        `${p.torneo?.tipo === "LIGA" ? "Fecha" : "Ronda"} ${p.ronda ?? RAYA}`,
        `${texto(p.participanteANombre)} vs ${texto(p.participanteBNombre)}`,
        fechaCorta(p.fecha),
        texto(p.sets),
        texto(p.resultado) === RAYA ? "Sin jugar" : texto(p.resultado),
      ]),
  },
  {
    id: "cobros",
    titulo: "Cobros",
    descripcion: "Concepto, medio de pago y estado de cada cobro",
    columnas: [
      { titulo: "Código" },
      { titulo: "Concepto" },
      { titulo: "Medio de pago" },
      { titulo: "Cuotas", alineacion: DERECHA },
      { titulo: "Monto base", alineacion: DERECHA },
      { titulo: "Monto final", alineacion: DERECHA },
      { titulo: "Estado" },
      { titulo: "Fecha" },
    ],
    mapear: (cobros) =>
      cobros.map((c) => [
        texto(c.codigo),
        texto(ETIQUETA_CONCEPTO[c.concepto] ?? comoTitulo(c.concepto)),
        texto(ETIQUETA_MEDIO[c.medioPago] ?? comoTitulo(c.medioPago)),
        texto(c.cuotas),
        pesos(c.montoBase),
        pesos(c.montoFinal),
        comoTitulo(c.estado),
        fechaHora(c.fecha),
      ]),
  },
  {
    id: "recibos",
    titulo: "Recibos de pago",
    descripcion: "Recibos emitidos, con su monto y su estado",
    columnas: [
      { titulo: "N.º de recibo" },
      { titulo: "Concepto" },
      { titulo: "Medio de pago" },
      { titulo: "N.º de operación" },
      { titulo: "Monto", alineacion: DERECHA },
      { titulo: "Estado" },
      { titulo: "Emisión" },
    ],
    mapear: (recibos) =>
      recibos.map((r) => [
        texto(r.numeroRecibo),
        texto(r.concepto),
        texto(ETIQUETA_MEDIO[r.medioPago] ?? comoTitulo(r.medioPago)),
        texto(r.numeroOperacion),
        pesos(r.monto),
        comoTitulo(r.estado),
        fechaHora(r.fechaEmision),
      ]),
  },
  {
    id: "descuentos",
    titulo: "Descuentos",
    descripcion: "Tipos de descuento vigentes y su beneficio",
    // El módulo de descuentos todavía no tiene backend (BACK 16): la API del
    // front responde contra un mock. El impreso lo dice para que nadie tome
    // estos números por datos del club.
    aviso: "Datos de prueba: el módulo de descuentos todavía no tiene backend.",
    columnas: [
      { titulo: "Tipo" },
      { titulo: "Beneficio", alineacion: DERECHA },
      { titulo: "Condiciones" },
      { titulo: "Veces aplicado", alineacion: DERECHA },
      { titulo: "Vigente" },
    ],
    mapear: (descuentos) =>
      descuentos.map((d) => [
        texto(TIPO_DESCUENTO[d.tipo] ?? comoTitulo(d.tipo)),
        beneficio(d),
        texto(d.condiciones),
        texto(d.vecesAplicado),
        siNo(d.activo),
      ]),
  },
];

/** Un listado por su id, o undefined si no existe. */
export function listadoPorId(id) {
  return LISTADOS.find((listado) => listado.id === id);
}
