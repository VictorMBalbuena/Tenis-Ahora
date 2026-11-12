import { CampoFormulario, CLASE_INPUT } from "@/components/ui/campo-formulario";
import { SelectField } from "@/components/ui/select-field";
import { CustomCheckbox } from "@/components/ui/custom-checkbox";
import { MedioPago, OPCIONES_MEDIO, etiquetaReserva } from "../model/cobro";

/**
 * Formulario para cobrar la seña de una reserva (CU51).
 *
 * El monto y los descuentos los calcula siempre el backend: acá solo se
 * eligen la reserva y el medio de pago. numeroOperacion y aprobado los
 * carga el personal según lo que muestra la terminal física del club;
 * cuotas solo aplica a crédito.
 */
export function CobroFormCard({ datos, errores, onChange, onCambiarAprobado, reservas = [] }) {
  const requiereTerminal = datos.medioPago === MedioPago.DEBITO || datos.medioPago === MedioPago.CREDITO;
  const opcionesReserva = reservas.map((r) => ({ value: String(r.id), label: etiquetaReserva(r) }));

  return (
    <div className="rounded-xl border border-border bg-card p-8 shadow-sm space-y-6">
      <h2 className="text-lg font-bold text-black">Cobrar la seña</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        <CampoFormulario
          id="cobro-reserva"
          etiqueta="Reserva"
          ayuda="Solo aparecen las reservas con saldo pendiente."
          error={errores.reservaId}
        >
          {(props) => (
            <SelectField
              {...props}
              name="reservaId"
              value={datos.reservaId}
              opciones={opcionesReserva}
              placeholder={opcionesReserva.length > 0 ? "Elegí la reserva" : "No hay reservas con saldo"}
              onChange={onChange("reservaId")}
            />
          )}
        </CampoFormulario>

        <CampoFormulario id="cobro-medio" etiqueta="Medio de pago" error={errores.medioPago}>
          {(props) => (
            <SelectField
              {...props}
              name="medioPago"
              value={datos.medioPago}
              opciones={OPCIONES_MEDIO}
              placeholder="Elegí el medio de pago"
              onChange={onChange("medioPago")}
            />
          )}
        </CampoFormulario>

        {requiereTerminal && (
          <CampoFormulario
            id="cobro-numero-operacion"
            etiqueta="Número de operación"
            ayuda="El que da la terminal física del club."
            error={errores.numeroOperacion}
          >
            {(props) => (
              <input
                {...props}
                type="text"
                inputMode="numeric"
                maxLength={20}
                className={CLASE_INPUT}
                value={datos.numeroOperacion}
                placeholder="000123"
                onChange={onChange("numeroOperacion")}
              />
            )}
          </CampoFormulario>
        )}

        {datos.medioPago === MedioPago.CREDITO && (
          <CampoFormulario id="cobro-cuotas" etiqueta="Cuotas" ayuda="Si no se indica, se cobra en 1 cuota." error={errores.cuotas}>
            {(props) => (
              <input
                {...props}
                type="number"
                min="1"
                max="24"
                step="1"
                className={CLASE_INPUT}
                value={datos.cuotas}
                placeholder="1"
                onChange={onChange("cuotas")}
              />
            )}
          </CampoFormulario>
        )}
      </div>

      {requiereTerminal && (
        <CustomCheckbox
          label="La terminal aprobó la operación"
          name="aprobado"
          checked={datos.aprobado}
          onChange={(e) => onCambiarAprobado(e.target.checked)}
        />
      )}
    </div>
  );
}
