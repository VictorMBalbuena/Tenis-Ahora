import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Printer, Download, BarChart2, AlertCircle, FileSearch } from "lucide-react";
import axiosClient from "@/config/axiosClient";
import { finDelMes, inicioDelMes } from "@/lib/fechas";
import { KpiCard } from "@/components/ui/kpi-card";

export default function ReporteReservasPage() {
  const [periodo, setPeriodo] = useState("Este mes");
  const [desde, setDesde] = useState(inicioDelMes);
  const [hasta, setHasta] = useState(finDelMes);
  const [cancha, setCancha] = useState("Todas");

  const [reporte, setReporte] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");
  const [sinDatos, setSinDatos] = useState(false);

  const formatearMoneda = (valor) => `$ ${Number(valor || 0).toLocaleString("es-AR")}`;

  const obtenerReporte = async (fechaDesde, fechaHasta, canchaFiltro) => {
    setError("");

    if (!fechaDesde || !fechaHasta) {
      setError("Debe seleccionar las fechas 'Desde' y 'Hasta' para generar el reporte.");
      return;
    }

    if (new Date(fechaDesde) > new Date(fechaHasta)) {
      setError("La fecha 'Desde' no puede ser posterior a la fecha 'Hasta'.");
      return;
    }

    setCargando(true);
    try {
      const { data } = await axiosClient.get("/reportes/reservas", {
        params: { desde: fechaDesde, hasta: fechaHasta, cancha: canchaFiltro },
      });

      if (
        !data ||
        data.reservasRegistradas === 0 ||
        (Array.isArray(data.porCancha) && data.porCancha.length === 0)
      ) {
        setSinDatos(true);
        setReporte(null);
      } else {
        setSinDatos(false);
        setReporte(data);
      }
    } catch (err) {
      setError(err.response?.data?.mensaje || "No se pudo obtener el reporte de reservas.");
    } finally {
      setCargando(false);
    }
  };

  const handleGenerar = (e) => {
    e.preventDefault();
    obtenerReporte(desde, hasta, cancha);
  };

  const handleLimpiar = () => {
    setPeriodo("Este mes");
    setDesde(inicioDelMes());
    setHasta(finDelMes());
    setCancha("Todas");
    setError("");
    setSinDatos(false);
    setReporte(null);
  };

  return (
    <div className="w-full font-sans">
      <div className="flex justify-between items-center mb-6 text-sm text-muted-foreground print:hidden">
        <div>
          Administracion /{" "}
          <Link to="/reportes" className="hover:underline">
            Reportes
          </Link>{" "}
          / <span className="font-medium text-foreground">Reservas</span>
        </div>
      </div>

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-right text-[28px] font-bold text-foreground tracking-tight">
          Reporte de reservas
        </h1>
        <div className="flex items-center gap-3 print:hidden">
          <button
            type="button"
            onClick={() => window.print()}
            className="flex items-center gap-2 text-sm font-medium bg-card border border-border hover:bg-muted/50 px-4 py-2 rounded-full transition-colors"
          >
            <Printer size={16} />
            Imprimir
          </button>
          <button
            type="button"
            onClick={() => window.print()}
            className="flex items-center gap-2 text-sm font-medium bg-card border border-border hover:bg-muted/50 px-4 py-2 rounded-full transition-colors"
          >
            <Download size={16} />
            Exportar
          </button>
        </div>
      </div>

      <form
        onSubmit={handleGenerar}
        className="bg-card border border-border rounded-2xl p-6 mb-6 print:hidden"
      >
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 pb-5 border-b border-border">
          <div>
            <label className="block text-xs font-semibold text-foreground mb-2">Periodo</label>
            <select
              value={periodo}
              onChange={(e) => setPeriodo(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            >
              <option value="Este mes">Este mes</option>
              <option value="Mes pasado">Mes pasado</option>
              <option value="Personalizado">Personalizado</option>
            </select>
          </div>

          <div>
            <label htmlFor="reservas-desde" className="block text-xs font-semibold text-foreground mb-2">
              Desde
            </label>
            <input
              id="reservas-desde"
              type="date"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="reservas-hasta" className="block text-xs font-semibold text-foreground mb-2">
              Hasta
            </label>
            <input
              id="reservas-hasta"
              type="date"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="reservas-cancha" className="block text-xs font-semibold text-foreground mb-2">
              Cancha
            </label>
            <select
              id="reservas-cancha"
              value={cancha}
              onChange={(e) => setCancha(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            >
              <option value="Todas">Todas</option>
              <option value="Cancha 1 - Ladrillo">Cancha 1 - Ladrillo</option>
              <option value="Cancha 2 - Ladrillo">Cancha 2 - Ladrillo</option>
              <option value="Cancha 3 - Cemento">Cancha 3 - Cemento</option>
              <option value="Cancha 4 - Cemento">Cancha 4 - Cemento</option>
              <option value="Cancha 5 - Pasto">Cancha 5 - Pasto</option>
              <option value="Cancha 6 - Pasto">Cancha 6 - Pasto</option>
            </select>
          </div>
        </div>

        <div className="flex justify-end items-center gap-4 pt-4">
          <button
            type="button"
            onClick={handleLimpiar}
            className="text-sm font-medium text-foreground hover:text-foreground px-3 py-2"
          >
            Limpiar
          </button>
          <button
            type="submit"
            disabled={cargando}
            className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-primary-foreground text-sm font-medium px-5 py-2.5 rounded-full transition-colors"
          >
            <BarChart2 size={16} />
            {cargando ? "Generando..." : "Generar reporte"}
          </button>
        </div>
      </form>

      {error && (
        <div role="alert" className="flex items-center gap-3 bg-[var(--estado-error-bg)] border border-[var(--estado-error-border)] text-[var(--estado-error-fg)] px-4 py-3 rounded-xl mb-6 text-sm">
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {sinDatos && !cargando && !error && (
        <div className="bg-card border border-border rounded-2xl p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-muted text-muted-foreground flex items-center justify-center mx-auto mb-4">
            <FileSearch size={24} />
          </div>
          <h2 className="text-lg font-bold text-foreground mb-1">
            Sin datos para el período seleccionado
          </h2>
          <p className="text-sm text-muted-foreground">
            No se encontraron reservas registradas para el rango de fechas y cancha seleccionados.
          </p>
        </div>
      )}

      {!reporte && !sinDatos && !cargando && !error && (
        <div className="bg-card border border-border rounded-2xl p-12 text-center">
          <p className="text-muted-foreground">Seleccione los filtros y presione "Generar reporte" para visualizar los datos.</p>
        </div>
      )}

      {reporte && !cargando && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
            <KpiCard etiqueta="Reservas registradas" valor={reporte.reservasRegistradas} detalle={reporte.variacionReservas} detalleClase="font-medium text-primary" />

            <KpiCard etiqueta="Confirmadas" valor={reporte.confirmadas} detalle={reporte.porcentajeConfirmadas} />

            <KpiCard etiqueta="Canceladas" valor={reporte.canceladas} detalle={reporte.detalleCanceladas} />

            <KpiCard etiqueta="Ocupacion media" valor={reporte.ocupacionMedia} detalle={reporte.variacionOcupacion} detalleClase="font-medium text-primary" />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground mb-5">Reservas por cancha</h2>
              <div className="space-y-4">
                {reporte.porCancha?.map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-xs mb-1.5">
                      <span className="text-foreground font-medium">{item.cancha}</span>
                      <span className="font-semibold text-foreground">{item.cantidad}</span>
                    </div>
                    <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                      <div style={{ width: `${item.porcentaje}%` }} className="h-full bg-primary rounded-full" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-card border border-border rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <h2 className="text-base font-bold text-foreground">Ocupacion por franja horaria</h2>
                <p className="text-xs text-muted-foreground mb-6">Promedio del periodo</p>
              </div>

              <div className="h-48 flex items-end justify-between gap-3 pt-6 pb-2 border-b border-border">
                {reporte.ocupacionHoraria?.map((franja, idx) => (
                  <div key={idx} className="flex-1 flex flex-col items-center h-full justify-end">
                    <span className="text-[11px] text-muted-foreground mb-1.5">{franja.porcentaje}%</span>
                    {/* La franja pico va en el verde pleno y el resto atenuado: antes era al reves y la pico destacaba menos. */}
                    <div
                      style={{ height: `${franja.porcentaje}%` }}
                      className={`w-full rounded-t-xl transition-all ${franja.pico ? "bg-primary" : "bg-primary/45"}`}
                    />
                  </div>
                ))}
              </div>

              <div className="flex justify-between text-[11px] text-muted-foreground mt-2 px-2">
                {reporte.ocupacionHoraria?.map((franja, idx) => (
                  <span key={idx} className="flex-1 text-center">{franja.hora}</span>
                ))}
              </div>

              <p className="text-xs text-muted-foreground mt-4">{reporte.textoFranjaPico}</p>
            </div>
          </div>

          <div className="bg-card border border-border rounded-2xl p-6">
            <h2 className="text-base font-bold text-foreground">Cancelaciones</h2>
            <p className="text-xs text-muted-foreground mb-5">dentro y fuera del plazo de 6 horas</p>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <KpiCard etiqueta="Dentro del plazo" valor={reporte.cancelacionesDetalle?.dentroPlazo} detalle="Reembolso total o parcial" />

              <KpiCard etiqueta="Fuera del plazo" valor={reporte.cancelacionesDetalle?.fueraPlazo} detalle="Con cargo por penalidad" />

              <KpiCard etiqueta="Monto retenido" valor={formatearMoneda(reporte.cancelacionesDetalle?.montoRetenido)} />
            </div>
          </div>
        </>
      )}
    </div>
  );
}