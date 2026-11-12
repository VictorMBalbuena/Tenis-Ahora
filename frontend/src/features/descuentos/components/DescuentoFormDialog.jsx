import { useState } from "react";
import { AlertCircle, Percent, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "cn";
import {
  FormaDeCalculo,
  LARGO_CODIGO,
  TipoDescuento,
  formaDeCalculo,
  normalizarCodigo,
  validarCodigo,
} from "../model/descuento";
import { actualizarDescuento, crearDescuento } from "../api/descuentosApi";
import { camposDeError, mensajeDeError } from "../api/errores";

/**
 * Alta (CU62) y modificación (CU63) de un descuento.
 *
 * La entidad tiene porcentaje y montoFijo, los dos anulables, pero son dos
 * formas excluyentes de calcular el mismo beneficio. El formulario obliga a
 * elegir una y manda la otra en null, así no quedan descuentos con los dos
 * cargados ni con ninguno.
 */

// El tipo ya no se elige ni se muestra, pero el backend lo sigue exigiendo: un descuento nuevo
// nace promocional, y uno que se edita conserva el que tenía.
const VACIO = {
  codigo: "",
  tipo: TipoDescuento.PROMOCIONAL,
  porcentaje: "",
  montoFijo: "",
  condiciones: "",
  activo: true,
};

/** Campo con su error debajo, enlazado por aria. */
function Campo({ id, etiqueta, ayuda, error, ancho = "", children }) {
  const idAyuda = ayuda ? `${id}-ayuda` : undefined;
  const idError = error ? `${id}-error` : undefined;

  return (
    <div className={cn("space-y-1.5", ancho)}>
      <Label htmlFor={id}>{etiqueta}</Label>
      {children({
        id,
        "aria-describedby":
          [idAyuda, idError].filter(Boolean).join(" ") || undefined,
        "aria-invalid": error ? true : undefined,
      })}
      {ayuda && !error && (
        <p id={idAyuda} className="text-xs text-muted-foreground">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={idError} className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

export function DescuentoFormDialog({
  abierto,
  descuento,
  onCerrar,
  onGuardado,
}) {
  const esModificacion = Boolean(descuento);
  // El padre remonta este diálogo con un `key` cada vez que se abre, así que
  // el valor inicial de useState ya es el correcto y no hace falta resetear.
  const [datos, setDatos] = useState(() =>
    descuento
      ? {
          codigo: descuento.codigo ?? "",
          tipo: descuento.tipo,
          porcentaje: descuento.porcentaje ?? "",
          montoFijo: descuento.montoFijo ?? "",
          condiciones: descuento.condiciones ?? "",
          activo: descuento.activo,
        }
      : VACIO,
  );
  const [forma, setForma] = useState(
    () => formaDeCalculo(descuento ?? {}) ?? FormaDeCalculo.PORCENTAJE,
  );
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [guardando, setGuardando] = useState(false);

  const cambiar = (campo) => (valor) =>
    setDatos((prev) => ({ ...prev, [campo]: valor }));

  /** Cambiar de forma limpia la otra, para no mandar las dos. */
  function elegirForma(nueva) {
    setForma(nueva);
    setDatos((prev) =>
      nueva === FormaDeCalculo.PORCENTAJE
        ? { ...prev, montoFijo: "" }
        : { ...prev, porcentaje: "" },
    );
  }

  const esPorcentaje = forma === FormaDeCalculo.PORCENTAJE;

  function validar() {
    const nuevos = {};

    if (esPorcentaje) {
      const p = Number(datos.porcentaje);
      if (datos.porcentaje === "" || Number.isNaN(p)) {
        nuevos.porcentaje = "Escribí el porcentaje.";
      } else if (p <= 0 || p > 100) {
        nuevos.porcentaje = "Tiene que estar entre 1 y 100.";
      }
    } else {
      const m = Number(datos.montoFijo);
      if (datos.montoFijo === "" || Number.isNaN(m)) {
        nuevos.montoFijo = "Escribí el monto.";
      } else if (m <= 0) {
        nuevos.montoFijo = "Tiene que ser mayor que cero.";
      }
    }

    if (!datos.condiciones.trim()) {
      nuevos.condiciones =
        "Explicá a quién alcanza: es lo único que identifica al descuento.";
    }

    const errorCodigo = validarCodigo(datos.codigo);
    if (errorCodigo) nuevos.codigo = errorCodigo;

    setErrores(nuevos);
    return Object.keys(nuevos).length === 0;
  }

  /** Lo que viaja: la forma elegida con valor, la otra en null. */
  function paraEnviar() {
    return {
      codigo: datos.codigo,
      tipo: datos.tipo,
      porcentaje: esPorcentaje ? Number(datos.porcentaje) : null,
      montoFijo: esPorcentaje ? null : Number(datos.montoFijo),
      condiciones: datos.condiciones.trim(),
      activo: datos.activo,
    };
  }

  async function guardar(e) {
    e.preventDefault();
    setErrorGeneral("");
    if (!validar()) return;

    setGuardando(true);
    try {
      const payload = paraEnviar();
      if (esModificacion) {
        await actualizarDescuento(descuento.id, payload);
        onGuardado(
          `Se guardaron los cambios del descuento ${payload.codigo}.`,
        );
      } else {
        await crearDescuento(payload);
        onGuardado(
          `Se registró el descuento ${payload.codigo}. Ya se puede usar al reservar.`,
        );
      }
    } catch (error) {
      const campos = camposDeError(error);
      if (campos) setErrores(campos);
      setErrorGeneral(
        mensajeDeError(error, "No se pudo guardar el descuento."),
      );
    } finally {
      setGuardando(false);
    }
  }

  const opciones = [
    { valor: FormaDeCalculo.PORCENTAJE, texto: "Porcentaje", icono: Percent },
    { valor: FormaDeCalculo.MONTO_FIJO, texto: "Monto fijo", icono: Wallet },
  ];

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {esModificacion ? "Modificar descuento" : "Nuevo descuento"}
          </DialogTitle>
          <DialogDescription>
            Definí el código que se escribe al reservar y cómo se calcula el
            beneficio sobre el monto a cobrar.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={guardar} noValidate className="space-y-5">
          {errorGeneral && (
            <p
              role="alert"
              className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {errorGeneral}
            </p>
          )}

          <Campo
            id="descuento-codigo"
            etiqueta="Código"
            ayuda="Es lo que se escribe al reservar para que se aplique. Solo letras y números."
            error={errores.codigo}
          >
            {(props) => (
              <Input
                {...props}
                value={datos.codigo}
                onChange={(e) => cambiar("codigo")(normalizarCodigo(e.target.value))}
                placeholder="CYBERWEEK"
                maxLength={LARGO_CODIGO}
                autoComplete="off"
                className="font-mono tracking-wide"
              />
            )}
          </Campo>

          <fieldset className="space-y-3">
            <legend className="mb-2 text-sm font-medium">
              Cómo se calcula
            </legend>
            <div className="grid grid-cols-2 gap-2">
              {opciones.map(({ valor, texto, icono: Icono }) => (
                <label
                  key={valor}
                  className={cn(
                    "flex cursor-pointer items-center justify-center gap-2 rounded-lg border p-3 text-sm font-medium transition-colors",
                    "has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
                    forma === valor
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border hover:bg-muted",
                  )}
                >
                  <input
                    type="radio"
                    name="forma-de-calculo"
                    value={valor}
                    checked={forma === valor}
                    onChange={() => elegirForma(valor)}
                    className="sr-only"
                  />
                  <Icono className="size-4" />
                  {texto}
                </label>
              ))}
            </div>

            {esPorcentaje ? (
              <Campo
                id="descuento-porcentaje"
                etiqueta="Porcentaje de descuento"
                ayuda="Sobre el monto total a cobrar"
                error={errores.porcentaje}
              >
                {(props) => (
                  <Input
                    {...props}
                    type="number"
                    min={1}
                    max={100}
                    value={datos.porcentaje}
                    placeholder="15"
                    onChange={(e) => cambiar("porcentaje")(e.target.value)}
                  />
                )}
              </Campo>
            ) : (
              <Campo
                id="descuento-monto"
                etiqueta="Monto fijo a descontar"
                ayuda="En pesos. Nunca descuenta más que el total del cobro."
                error={errores.montoFijo}
              >
                {(props) => (
                  <Input
                    {...props}
                    type="number"
                    min={1}
                    value={datos.montoFijo}
                    placeholder="4000"
                    onChange={(e) => cambiar("montoFijo")(e.target.value)}
                  />
                )}
              </Campo>
            )}
          </fieldset>

          <Campo
            id="descuento-condiciones"
            etiqueta="Condiciones"
            ayuda="Es lo que identifica al descuento: la entidad no tiene un campo de nombre."
            error={errores.condiciones}
          >
            {(props) => (
              <Textarea
                {...props}
                rows={3}
                value={datos.condiciones}
                placeholder="Equipos inscriptos en una liga regular del club..."
                onChange={(e) => cambiar("condiciones")(e.target.value)}
              />
            )}
          </Campo>

          <div className="flex gap-2.5">
            <input
              id="descuento-activo"
              type="checkbox"
              checked={datos.activo}
              onChange={(e) => cambiar("activo")(e.target.checked)}
              aria-describedby="descuento-activo-ayuda"
              className="mt-0.5 size-4 shrink-0 accent-primary"
            />
            <div className="space-y-0.5">
              <Label htmlFor="descuento-activo" className="font-normal">
                Descuento activo
              </Label>
              <p
                id="descuento-activo-ayuda"
                className="text-xs text-muted-foreground"
              >
                Solo los activos se aplican al calcular un cobro
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando}>
              {guardando
                ? "Guardando..."
                : esModificacion
                  ? "Guardar cambios"
                  : "Registrar descuento"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
