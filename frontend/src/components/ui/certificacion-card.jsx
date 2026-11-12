import { DateField } from "@/components/ui/date-field";
import { hoyLocal } from "@/lib/fechas";
import { CampoFormulario, CLASE_INPUT } from "./campo-formulario";

// Certificación que respalda el título habilitante (CU30): número, entidad emisora y vigencia.
export function CertificacionCard({ datos, errores, onChange }) {
  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <h2 className="text-lg font-bold text-foreground">Certificación</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <CampoFormulario id="cert-numero" etiqueta="Número de certificación" obligatorio error={errores.numeroCertificacion}>
          {(props) => (
            <input {...props} className={CLASE_INPUT} value={datos.numeroCertificacion || ""} placeholder="CERT-2026-001" onChange={onChange("numeroCertificacion")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="cert-entidad" etiqueta="Entidad emisora" obligatorio error={errores.entidadEmisora}>
          {(props) => (
            <input
              {...props}
              className={CLASE_INPUT}
              value={datos.entidadEmisora || ""}
              placeholder="Federación Argentina de Tenis"
              onChange={onChange("entidadEmisora")}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="cert-emision" etiqueta="Fecha de emisión" obligatorio error={errores.fechaEmision}>
          {/* Un titulo no se puede emitir manana: el calendario corta en hoy. */}
          {(props) => (
            <DateField {...props} max={hoyLocal()} value={datos.fechaEmision || ""} onChange={onChange("fechaEmision")} />
          )}
        </CampoFormulario>

        <CampoFormulario id="cert-vencimiento" etiqueta="Fecha de vencimiento" info="Si no vence, dejalo vacío" error={errores.fechaVencimiento}>
          {(props) => <DateField {...props} value={datos.fechaVencimiento || ""} onChange={onChange("fechaVencimiento")} />}
        </CampoFormulario>

        <CampoFormulario id="cert-archivo" etiqueta="Archivo del título" info="URL del escaneo o PDF" error={errores.archivoTituloUrl} ancho="sm:col-span-2">
          {(props) => (
            <input
              {...props}
              className={CLASE_INPUT}
              value={datos.archivoTituloUrl || ""}
              placeholder="https://club.com/titulos/cert-2026-001.pdf"
              onChange={onChange("archivoTituloUrl")}
            />
          )}
        </CampoFormulario>
      </div>
    </div>
  );
}
