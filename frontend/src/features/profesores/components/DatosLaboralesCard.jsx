import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";

// Título habilitante, especialidad y valor por hora.
export function DatosLaboralesCard({ datos, errores, onChange }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <h2 className="text-lg font-bold text-black">Datos laborales</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <CampoFormulario
          id="prof-titulo"
          etiqueta="Título habilitante"
          info="Queda pendiente de validar hasta que lo apruebe un empleado autorizado"
          error={errores.tituloHabilitante}
          ancho="sm:col-span-2"
        >
          {(props) => (
            <input
              {...props}
              className={CLASE_INPUT}
              value={datos.tituloHabilitante}
              placeholder="Profesor Nacional de Tenis (AAT-2023-0481)"
              onChange={onChange("tituloHabilitante")}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-especialidad" etiqueta="Especialidad">
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.especialidad || ""} placeholder="Tenis infantil" onChange={onChange("especialidad")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="prof-valor-hora" etiqueta="Valor por hora">
          {(props) => (
            <input
              {...props}
              type="number"
              min="0"
              step="0.01"
              className={CLASE_INPUT}
              value={datos.valorPorHora ?? ""}
              placeholder="8500.00"
              onChange={onChange("valorPorHora")}
            />
          )}
        </CampoFormulario>
      </div>
    </div>
  );
}
