import { useState } from "react";
import { BadgePercent, Check, Loader2, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { validarCodigo } from "@/features/descuentos/api/descuentosApi";
import { normalizarCodigo } from "@/features/descuentos/model/descuento";
import { formatearMoneda, extraerMensajeError } from "../model/reserva";

/**
 * El campo donde se escribe un código de descuento al reservar (RF69).
 *
 * Validar y aplicar son el mismo botón a propósito: el socio escribe el
 * código, lo valida, y si existe ya ve en el total cuánto le queda a pagar.
 * No hay un segundo paso de "aplicar" que se pueda olvidar.
 *
 * Lo que se muestra acá es una vista previa. El descuento de verdad lo vuelve
 * a calcular el servidor al guardar la reserva, sobre el total real: si el
 * código se dio de baja entremedio, la reserva falla en vez de cobrar mal.
 */
export function CodigoDeDescuento({ montoBase = 0, aplicado = null, onAplicar, onQuitar, deshabilitado = false }) {
  const [codigo, setCodigo] = useState("");
  const [validando, setValidando] = useState(false);
  const [error, setError] = useState("");

  async function validar() {
    const limpio = normalizarCodigo(codigo);
    if (!limpio) {
      setError("Escribí un código para validarlo.");
      return;
    }

    setValidando(true);
    setError("");
    try {
      const resultado = await validarCodigo(limpio, montoBase);
      onAplicar(resultado);
      setCodigo("");
    } catch (fallo) {
      setError(extraerMensajeError(fallo, "Ese código no existe o no está vigente."));
    } finally {
      setValidando(false);
    }
  }

  function quitar() {
    setError("");
    onQuitar();
  }

  if (aplicado) {
    return (
      <div className="flex items-center justify-between gap-3 rounded-lg bg-[var(--estado-ok-bg)] px-3.5 py-3 text-sm text-[var(--estado-ok-fg)]">
        <span className="flex min-w-0 items-center gap-2">
          <Check className="size-4 shrink-0" aria-hidden="true" />
          <span className="min-w-0">
            <span className="font-mono font-semibold tracking-wide">{aplicado.codigo}</span>
            {" — "}
            {aplicado.descripcion}
            {aplicado.montoDescontado != null && (
              <span className="font-semibold"> ({formatearMoneda(aplicado.montoDescontado)} menos)</span>
            )}
          </span>
        </span>
        <button
          type="button"
          onClick={quitar}
          disabled={deshabilitado}
          className="flex shrink-0 items-center gap-1 font-medium underline-offset-2 hover:underline disabled:opacity-50"
        >
          <X className="size-3.5" aria-hidden="true" />
          Quitar
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-1.5">
      <Label htmlFor="codigo-descuento" className="flex items-center gap-1.5 text-sm">
        <BadgePercent className="size-4 text-muted-foreground" aria-hidden="true" />
        ¿Tenés un código de descuento?
      </Label>
      <div className="flex gap-2">
        <Input
          id="codigo-descuento"
          value={codigo}
          onChange={(e) => {
            setCodigo(normalizarCodigo(e.target.value));
            if (error) setError("");
          }}
          onKeyDown={(e) => {
            // Enter valida el código y no envía el formulario de la reserva.
            if (e.key === "Enter") {
              e.preventDefault();
              validar();
            }
          }}
          placeholder="CYBERWEEK"
          maxLength={30}
          autoComplete="off"
          disabled={deshabilitado || validando}
          aria-describedby={error ? "codigo-descuento-error" : undefined}
          aria-invalid={error ? true : undefined}
          className="font-mono tracking-wide"
        />
        <Button
          type="button"
          variant="outline"
          onClick={validar}
          disabled={deshabilitado || validando || !codigo}
        >
          {validando ? <Loader2 className="size-4 animate-spin" aria-hidden="true" /> : "Validar"}
        </Button>
      </div>
      {error && (
        <p id="codigo-descuento-error" role="alert" className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}
