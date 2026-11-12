import { useEffect, useState } from "react";
import axiosClient from "@/config/axiosClient";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { DateField } from "@/components/ui/date-field";
import { TimeField } from "@/components/ui/time-field";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { CUPO_MAXIMO_INSTITUCIONAL, OPCIONES_DIA, OPCIONES_MODALIDAD, OPCIONES_NIVEL } from "../model/clase";

/**
 * Datos de una clase: lo comparten el alta (CU32) y la modificación (CU33).
 *
 * El cupo máximo se carga acá, que es lo que pide CU70. El tope del club son
 * 30 alumnos (RD01), así que el campo lo limita y la validación lo rechaza.
 *
 * Las canchas y los profesores se traen del backend para poder elegirlos; si
 * esos listados fallan, los selectores quedan vacíos pero el resto del
 * formulario sigue usable.
 */
export function ClaseFormCard({ datos, errores, onChange }) {
  const [canchas, setCanchas] = useState([]);
  const [profesores, setProfesores] = useState([]);

  useEffect(() => {
    let vigente = true;

    axiosClient
      .get("/canchas", { params: { size: 50 } })
      .then(({ data }) => vigente && setCanchas(data.contenido || []))
      .catch(() => vigente && setCanchas([]));

    axiosClient
      .get("/profesores", { params: { size: 50, activo: true } })
      .then(({ data }) => vigente && setProfesores(data.contenido || []))
      .catch(() => vigente && setProfesores([]));

    return () => {
      vigente = false;
    };
  }, []);

  const opcionesCancha = canchas.map((c) => ({
    value: String(c.id),
    label: c.identificador ? `${c.identificador} — ${c.superficie ?? ""}`.trim() : `Cancha ${c.id}`,
  }));

  const opcionesProfesor = profesores.map((p) => ({
    value: String(p.id),
    label: `${p.nombre} ${p.apellido}`.trim(),
  }));

  function alternarDia(dia) {
    const dias = datos.dias ?? [];
    const nuevos = dias.includes(dia) ? dias.filter((d) => d !== dia) : [...dias, dia];
    onChange({ target: { name: "dias", value: nuevos } });
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
      <h2 className="mb-5 text-lg font-bold">Datos de la clase</h2>

      <div className="grid gap-5 sm:grid-cols-2">
        <CampoFormulario id="clase-nombre" etiqueta="Nombre" error={errores.nombre}>
          {(props) => (
            <input
              {...props}
              name="nombre"
              value={datos.nombre}
              onChange={onChange}
              placeholder="Tenis para principiantes"
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-profesor" etiqueta="Profesor" error={errores.profesorId}>
          {() => (
            <SelectField
              name="profesorId"
              value={datos.profesorId}
              onChange={onChange}
              opciones={opcionesProfesor}
              placeholder="Elegí el profesor"
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-cancha" etiqueta="Cancha" error={errores.canchaId}>
          {() => (
            <SelectField
              name="canchaId"
              value={datos.canchaId}
              onChange={onChange}
              opciones={opcionesCancha}
              placeholder="Elegí la cancha"
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-nivel" etiqueta="Nivel" error={errores.nivel}>
          {() => (
            <SelectField
              name="nivel"
              value={datos.nivel}
              onChange={onChange}
              opciones={OPCIONES_NIVEL}
              placeholder="Elegí el nivel"
            />
          )}
        </CampoFormulario>

        <CampoFormulario
          id="clase-modalidad"
          etiqueta="Modalidad"
          info="Una clase particular suele tener cupo 1; la grupal, varios alumnos."
          error={errores.modalidad}
        >
          {() => (
            <SelectField
              name="modalidad"
              value={datos.modalidad}
              onChange={onChange}
              opciones={OPCIONES_MODALIDAD}
              placeholder="Elegí la modalidad"
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-inicio" etiqueta="Fecha de inicio" error={errores.fechaInicio}>
          {() => <DateField name="fechaInicio" value={datos.fechaInicio} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="clase-fin" etiqueta="Fecha de fin" error={errores.fechaFin}>
          {() => <DateField name="fechaFin" value={datos.fechaFin} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario
          id="clase-cupo"
          etiqueta="Cupo máximo"
          info={`El club no admite más de ${CUPO_MAXIMO_INSTITUCIONAL} alumnos por clase.`}
          ayuda={`De 1 a ${CUPO_MAXIMO_INSTITUCIONAL} alumnos`}
          error={errores.cupoMaximo}
        >
          {(props) => (
            <input
              {...props}
              name="cupoMaximo"
              type="number"
              min={1}
              max={CUPO_MAXIMO_INSTITUCIONAL}
              value={datos.cupoMaximo}
              onChange={onChange}
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-valor" etiqueta="Valor de inscripción" info="El socio paga una seña para confirmar el lugar y después el saldo." error={errores.valorMensual}>
          {(props) => (
            <input
              {...props}
              name="valorMensual"
              type="number"
              min={0}
              step="0.01"
              value={datos.valorMensual}
              onChange={onChange}
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="clase-hora-inicio" etiqueta="Hora de inicio" error={errores.horaInicio}>
          {() => <TimeField name="horaInicio" value={datos.horaInicio} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="clase-hora-fin" etiqueta="Hora de fin" error={errores.horaFin}>
          {() => <TimeField name="horaFin" value={datos.horaFin} onChange={onChange} />}
        </CampoFormulario>
      </div>

      <CampoFormulario id="clase-dias" etiqueta="Días de la semana" error={errores.dias}>
        {() => (
          <div className="flex flex-wrap gap-4">
            {OPCIONES_DIA.map((opcion) => (
              <CustomCheckbox
                key={opcion.value}
                name={`dia-${opcion.value}`}
                label={opcion.label}
                checked={(datos.dias ?? []).includes(opcion.value)}
                onChange={() => alternarDia(opcion.value)}
              />
            ))}
          </div>
        )}
      </CampoFormulario>
    </div>
  );
}
