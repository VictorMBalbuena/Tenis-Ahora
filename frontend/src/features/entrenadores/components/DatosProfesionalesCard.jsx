import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";

// Certificación deportiva, especialidad y valor por hora.
export function DatosProfesionalesCard({ datos, errores, onChange }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <h2 className="text-lg font-bold text-foreground">Datos profesionales</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <CampoFormulario
          id="entr-certificacion-deportiva"
          etiqueta="Certificación deportiva"
          info="Sin esto no se puede registrar al entrenador"
          obligatorio
          error={errores.certificacionDeportiva}
          ancho="sm:col-span-2"
        >
          {(props) => (
            <input
              {...props}
              className={CLASE_INPUT}
              value={datos.certificacionDeportiva}
              placeholder="Entrenador Nacional de Tenis"
              onChange={onChange("certificacionDeportiva")}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="entr-especialidad" etiqueta="Especialidad" error={errores.especialidad}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.especialidad || ""} placeholder="Preparación física" onChange={onChange("especialidad")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="entr-valor-hora" etiqueta="Valor por hora" error={errores.valorPorHora}>
          {(props) => (
            <input
              {...props}
              type="number"
              min="0"
              step="0.01"
              className={CLASE_INPUT}
              value={datos.valorPorHora ?? ""}
              placeholder="9500.00"
              onChange={onChange("valorPorHora")}
            />
          )}
        </CampoFormulario>
      </div>
    </div>
  );
}
