import { useEffect, useState } from "react";
import axiosClient from "@/config/axiosClient";
import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { DateField } from "@/components/ui/date-field";
import { TimeField } from "@/components/ui/time-field";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { CUPO_MAXIMO_INSTITUCIONAL, DIAS_SEMANA, OPCIONES_MODALIDAD } from "../model/entrenamiento";

/**
 * Datos de un entrenamiento: lo comparten el alta (CU37) y la modificación (CU38).
 *
 * Es una actividad recurrente: se programa entre una fecha de inicio y una de
 * fin, dictándose en uno o más días fijos de la semana. Solo se ofrecen
 * entrenadores con la certificación validada (RD02, RD03).
 */
export function EntrenamientoFormCard({ datos, errores, onChange }) {
  const [canchas, setCanchas] = useState([]);
  const [entrenadores, setEntrenadores] = useState([]);

  useEffect(() => {
    let vigente = true;

    axiosClient
      .get("/canchas", { params: { size: 50 } })
      .then(({ data }) => vigente && setCanchas(data.contenido || []))
      .catch(() => vigente && setCanchas([]));

    axiosClient
      .get("/entrenadores", { params: { size: 50, activo: true, estado: "VALIDADO" } })
      .then(({ data }) => vigente && setEntrenadores(data.contenido || []))
      .catch(() => vigente && setEntrenadores([]));

    return () => {
      vigente = false;
    };
  }, []);

  const opcionesCancha = canchas.map((c) => ({
    value: String(c.id),
    label: c.identificador ? `${c.identificador} — ${c.superficie ?? ""}`.trim() : `Cancha ${c.id}`,
  }));

  const opcionesEntrenador = entrenadores.map((e) => ({
    value: String(e.id),
    label: `${e.nombre} ${e.apellido}`.trim(),
  }));

  const dias = datos.dias || [];

  function alternarDia(valor) {
    const nuevos = dias.includes(valor) ? dias.filter((d) => d !== valor) : [...dias, valor];
    onChange({ target: { name: "dias", value: nuevos } });
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
      <h2 className="mb-5 text-lg font-bold">Datos del entrenamiento</h2>

      <div className="grid gap-5 sm:grid-cols-2">
        <CampoFormulario id="ent-nombre" etiqueta="Nombre" ancho="sm:col-span-2" error={errores.nombre}>
          {(props) => (
            <input
              {...props}
              name="nombre"
              value={datos.nombre}
              placeholder="Entrenamiento funcional"
              onChange={onChange}
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario
          id="ent-entrenador"
          etiqueta="Entrenador"
          info="Solo aparecen los que tienen la certificación validada."
          error={errores.entrenadorId}
        >
          {() => (
            <SelectField
              name="entrenadorId"
              value={datos.entrenadorId}
              onChange={onChange}
              opciones={opcionesEntrenador}
              placeholder="Elegí el entrenador"
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="ent-cancha" etiqueta="Cancha" error={errores.canchaId}>
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

        <CampoFormulario id="ent-objetivo" etiqueta="Objetivo" ancho="sm:col-span-2" error={errores.objetivo}>
          {(props) => (
            <input
              {...props}
              name="objetivo"
              value={datos.objetivo}
              placeholder="Pretemporada, preparación física, alto rendimiento..."
              onChange={onChange}
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="ent-modalidad" etiqueta="Modalidad" error={errores.modalidad}>
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

        <CampoFormulario
          id="ent-cupo"
          etiqueta="Cupo máximo"
          info={`El club no admite más de ${CUPO_MAXIMO_INSTITUCIONAL} alumnos por actividad.`}
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

        <CampoFormulario id="ent-inicio-vigencia" etiqueta="Fecha de inicio" error={errores.fechaInicio}>
          {() => <DateField name="fechaInicio" value={datos.fechaInicio} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="ent-fin-vigencia" etiqueta="Fecha de fin" error={errores.fechaFin}>
          {() => <DateField name="fechaFin" value={datos.fechaFin} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="ent-inicio" etiqueta="Hora de inicio" error={errores.horaInicio}>
          {() => <TimeField name="horaInicio" value={datos.horaInicio} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="ent-fin" etiqueta="Hora de fin" error={errores.horaFin}>
          {() => <TimeField name="horaFin" value={datos.horaFin} onChange={onChange} />}
        </CampoFormulario>

        <CampoFormulario id="ent-categoria" etiqueta="Categoría" error={errores.categoria}>
          {(props) => (
            <input
              {...props}
              name="categoria"
              value={datos.categoria}
              placeholder="Adultos, juveniles..."
              onChange={onChange}
              className={CLASE_INPUT}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="ent-valor-mensual" etiqueta="Valor de inscripción" info="El socio paga una seña para confirmar el lugar y después el saldo." error={errores.valorMensual}>
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

        <CampoFormulario id="ent-dias" etiqueta="Días de dictado" ancho="sm:col-span-2" error={errores.dias}>
          {() => (
            <div className="flex flex-wrap gap-x-5 gap-y-2">
              {DIAS_SEMANA.map((d) => (
                <CustomCheckbox
                  key={d.value}
                  name={`dia-${d.value}`}
                  label={d.etiqueta}
                  checked={dias.includes(d.value)}
                  onChange={() => alternarDia(d.value)}
                />
              ))}
            </div>
          )}
        </CampoFormulario>
      </div>
    </div>
  );
}
