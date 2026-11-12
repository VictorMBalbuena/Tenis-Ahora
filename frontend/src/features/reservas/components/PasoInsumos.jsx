import { useState, useEffect, useMemo } from "react";
import { ArrowLeft, ArrowRight, CheckCircle2, AlertTriangle, Package } from "lucide-react";
import { consultarInsumosDisponibles } from "../api/reservasApi";
import { extraerMensajeError } from "../model/reserva";
import { SelectorInsumos } from "./SelectorInsumos";
import { InfoTip } from "@/components/ui/campo-label";

function normalizarCantidades(insumosDraft) {
  const mapa = {};
  if (Array.isArray(insumosDraft)) {
    for (const item of insumosDraft) {
      if (item && item.insumoId != null && item.cantidad > 0) {
        mapa[item.insumoId] = Number(item.cantidad);
      }
    }
  }
  return mapa;
}

/**
 * Paso 3 del Wizard de Reservas: selección de insumos deportivos opcionales.
 * Sigue el diseño de `reserva-03-insumos.html` y `reserva-03-sin-stock.html`.
 */
export function PasoInsumos({
  insumos = null,
  turno = null,
  valor = {},
  onVolver = () => {},
  onSiguiente = () => {},
}) {
  const [catalogoApi, setCatalogoApi] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [errorCarga, setErrorCarga] = useState(null);

  const [cantidades, setCantidades] = useState(() => normalizarCantidades(valor?.insumos));

  // Carga de catálogo si no se reciben como prop
  useEffect(() => {
    if (insumos != null) return;
    if (!turno?.fecha || !turno?.horaInicio || !turno?.duracionHoras) return;

    let activo = true;
    queueMicrotask(() => {
      if (activo) {
        setCargando(true);
        setErrorCarga(null);
      }
    });

    consultarInsumosDisponibles(turno)
      .then((data) => {
        if (!activo) return;
        const lista = Array.isArray(data) ? data : data?.insumos || [];
        setCatalogoApi(lista);
      })
      .catch((err) => {
        if (!activo) return;
        setErrorCarga(
          extraerMensajeError(
            err,
            "No se pudieron consultar los insumos disponibles."
          )
        );
      })
      .finally(() => {
        if (activo) setCargando(false);
      });

    return () => {
      activo = false;
    };
  }, [insumos, turno]);

  const catalogo = insumos != null ? insumos : catalogoApi;

  const handleCambiarCantidad = (insumoId, nuevaCantidad) => {
    setCantidades((prev) => ({
      ...prev,
      [insumoId]: nuevaCantidad,
    }));
  };

  const handleContinuar = () => {
    const seleccionados = catalogo
      .filter((item) => {
        const id = item.insumoId ?? item.id;
        return (cantidades[id] || 0) > 0;
      })
      .map((item) => {
        const id = item.insumoId ?? item.id;
        const out = {
          insumoId: id,
          nombre: item.nombre,
          cantidad: cantidades[id],
        };
        if (item.precioUnitario != null) {
          out.precioUnitario = item.precioUnitario;
        }
        return out;
      });

    onSiguiente({ insumos: seleccionados });
  };

  const cantidadTotalSeleccionada = useMemo(() => {
    return Object.values(cantidades).reduce((acc, c) => acc + (Number(c) || 0), 0);
  }, [cantidades]);

  // Chequeo si algún insumo seleccionado supera stock
  const tieneSinStock = useMemo(() => {
    return catalogo.some((item) => {
      const id = item.insumoId ?? item.id;
      const cant = cantidades[id] || 0;
      const stock = Number(item.cantidadAsignable ?? 0);
      return cant > stock;
    });
  }, [catalogo, cantidades]);

  return (
    <div className="space-y-5">
      {/* Tarjeta: Insumos a incorporar */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-start justify-between gap-4 px-8 pt-8">
          <div className="flex min-w-0 items-center gap-1.5">
            <h2 className="text-lg font-bold text-foreground">
              Insumos a incorporar
            </h2>
            <InfoTip>Opcionales. Quedan reservados para tu turno y se descuentan del stock al iniciar el alquiler</InfoTip>
          </div>
          {cantidadTotalSeleccionada > 0 && (
            <span className="inline-flex items-center rounded-full font-semibold px-2.5 py-1 text-xs bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)]">
              {cantidadTotalSeleccionada} {cantidadTotalSeleccionada === 1 ? "unidad" : "unidades"}
            </span>
          )}
        </div>

        <div className="px-8 pb-8 pt-6">
          <SelectorInsumos
            insumos={catalogo}
            cantidades={cantidades}
            onCambiarCantidad={handleCambiarCantidad}
            cargando={cargando}
            error={errorCarga}
          />

          {/* Banner de verificación de stock según mockup reserva-03-insumos.html L290 */}
          {!cargando && catalogo.length > 0 && (
            <div className="mt-4">
              {tieneSinStock ? (
                <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-error-bg)] text-[var(--estado-error-fg)]">
                  <AlertTriangle className="h-[18px] w-[18px] shrink-0" />
                  <p className="text-sm font-semibold">No se puede concretar el alquiler</p>
                </div>
              ) : (
                <div className="flex items-center gap-3 rounded-xl px-4 py-3 bg-[var(--estado-ok-bg)] text-[var(--estado-ok-fg)]">
                  <CheckCircle2 className="h-[18px] w-[18px] shrink-0" />
                  <p className="text-sm font-semibold">Stock verificado</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Acciones de navegación */}
      <div className="flex items-center justify-between pt-4 border-t border-border">
        <button
          type="button"
          role="button"
          onClick={onVolver}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors border border-input bg-background hover:bg-muted px-8 py-3 cursor-pointer"
        >
          <ArrowLeft className="h-[18px] w-[18px] shrink-0" />
          <span>Volver</span>
        </button>

        <button
          type="button"
          role="button"
          disabled={tieneSinStock}
          onClick={handleContinuar}
          className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-[15px] font-bold transition-colors bg-primary text-primary-foreground hover:brightness-95 px-8 py-3 cursor-pointer shadow-xs disabled:pointer-events-none disabled:opacity-40"
        >
          <span>Continuar</span>
          <ArrowRight className="h-[18px] w-[18px] shrink-0" />
        </button>
      </div>
    </div>
  );
}

export default PasoInsumos;
