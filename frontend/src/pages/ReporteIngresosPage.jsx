import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Printer, Download, BarChart2, AlertCircle, FileSearch } from "lucide-react";
import axiosClient from "@/config/axiosClient";
import { finDelMes, inicioDelMes } from "@/lib/fechas";
import { KpiCard } from "@/components/ui/kpi-card";

export default function ReporteIngresosPage() {
  const [periodo, setPeriodo] = useState("Este mes");
  const [desde, setDesde] = useState(inicioDelMes);
  const [hasta, setHasta] = useState(finDelMes);
  const [concepto, setConcepto] = useState("Todos");

  const [reporte, setReporte] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");
  const [sinDatos, setSinDatos] = useState(false);

  const formatearMoneda = (valor) =>
    `$ ${Number(valor || 0).toLocaleString("es-AR")}`;

  const obtenerReporte = async (fechaDesde, fechaHasta, conceptoFiltro) => {
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
      const { data } = await axiosClient.get("/reportes/ingresos", {
        params: { desde: fechaDesde, hasta: fechaHasta, concepto: conceptoFiltro },
      });

      if (
        !data ||
        data.ingresosTotales === 0 ||
        (Array.isArray(data.evolucionDiaria) && data.evolucionDiaria.length === 0)
      ) {
        setSinDatos(true);
        setReporte(null);
      } else {
        setSinDatos(false);
        setReporte(data);
      }
    } catch (err) {
      setError(err.response?.data?.mensaje || "No se pudo obtener el reporte de ingresos.");
    } finally {
      setCargando(false);
    }
  };

  const handleGenerar = (e) => {
    e.preventDefault();
    obtenerReporte(desde, hasta, concepto);
  };

  const handleLimpiar = () => {
    setPeriodo("Este mes");
    setDesde(inicioDelMes());
    setHasta(finDelMes());
    setConcepto("Todos");
    setError("");
    setSinDatos(false);
    setReporte(null);
  };

  const maxEvolucion =
    reporte?.evolucionDiaria?.reduce((max, item) => Math.max(max, item.monto), 0) || 1;

  return (
    <div className="w-full font-sans">
      <div className="flex justify-between items-center mb-6 text-sm text-muted-foreground print:hidden">
        <div>
          Administracion /{" "}
          <Link to="/reportes" className="hover:underline">
            Reportes
          </Link>{" "}
          / <span className="font-medium text-foreground">Ingresos</span>
        </div>
      </div>

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-right text-[28px] font-bold text-foreground tracking-tight">
          Reporte de ingresos
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
            <label className="block text-xs font-semibold text-foreground mb-2">
              Periodo
            </label>
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
            <label htmlFor="ingresos-desde" className="block text-xs font-semibold text-foreground mb-2">
              Desde
            </label>
            <input
              id="ingresos-desde"
              type="date"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="ingresos-hasta" className="block text-xs font-semibold text-foreground mb-2">
              Hasta
            </label>
            <input
              id="ingresos-hasta"
              type="date"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="ingresos-concepto" className="block text-xs font-semibold text-foreground mb-2">
              Concepto
            </label>
            <select
              id="ingresos-concepto"
              value={concepto}
              onChange={(e) => setConcepto(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            >
              <option value="Todos">Todos</option>
              <option value="Alquiler de canchas">Alquiler de canchas</option>
              <option value="Inscripciones a torneos">Inscripciones a torneos</option>
              <option value="Clases y entrenamientos">Clases y entrenamientos</option>
              <option value="Penalidades por cancelacion">Penalidades por cancelacion</option>
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
            No se registraron ingresos ni cobros entre las fechas indicadas. Probá ajustando el rango o el filtro de concepto.
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
            <KpiCard etiqueta="Ingresos totales" valor={formatearMoneda(reporte.ingresosTotales)} detalle={reporte.variacionMesPrevio} detalleClase="font-medium text-primary" />

            <KpiCard etiqueta="Senas (50%)" valor={formatearMoneda(reporte.senas)} detalle={reporte.porcentajeSenas} />

            <KpiCard etiqueta="Saldos finales" valor={formatearMoneda(reporte.saldosFinales)} detalle={reporte.porcentajeSaldos} />

            <KpiCard etiqueta="Bonificado por descuentos" valor={formatearMoneda(reporte.bonificadoDescuentos)} detalle={reporte.porcentajeBonificado} />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            <div className="lg:col-span-2 bg-card border border-border rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <h2 className="text-base font-bold text-foreground">Evolucion diaria</h2>
                <p className="text-xs text-muted-foreground mb-6">Ingresos cobrados por dia</p>
              </div>

              <div className="h-44 flex items-end gap-1.5 pt-4 border-b border-border pb-2">
                {reporte.evolucionDiaria?.map((item) => {
                  const alturaPorcentaje = Math.max(12, Math.round((item.monto / maxEvolucion) * 100));
                  return (
                    <div key={item.dia} className="flex-1 flex flex-col items-center h-full justify-end">
                      <div
                        title={`Día ${item.dia}: ${formatearMoneda(item.monto)}`}
                        style={{ height: `${alturaPorcentaje}%` }}
                        className="w-full max-w-[18px] bg-primary rounded-t-full transition-all"
                      />
                    </div>
                  );
                })}
              </div>

              <div className="flex justify-between text-[11px] text-muted-foreground mt-2 px-1">
                <span>1</span><span>5</span><span>9</span><span>13</span><span>17</span><span>21</span>
              </div>

              <p className="text-xs text-muted-foreground mt-4">{reporte.picoTexto}</p>
            </div>

            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground">Por medio de pago</h2>
              <p className="text-xs text-muted-foreground mb-6">medios contemplados</p>

              <div className="space-y-5">
                {reporte.porMedioPago?.map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-xs mb-1.5">
                      <span className="text-foreground font-medium">{item.medio}</span>
                      <span className="font-semibold text-foreground">{formatearMoneda(item.monto)}</span>
                    </div>
                    <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                      <div style={{ width: `${item.porcentaje}%` }} className="h-full bg-primary rounded-full" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground mb-6">Ingresos por concepto</h2>
              <div className="space-y-5">
                {reporte.porConcepto?.map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-xs mb-1.5">
                      <span className="text-foreground font-medium">{item.concepto}</span>
                      <span className="font-semibold text-foreground">{formatearMoneda(item.monto)}</span>
                    </div>
                    <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                      <div style={{ width: `${item.porcentaje}%` }} className="h-full bg-primary rounded-full" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground">Descuentos aplicados</h2>
              <p className="text-xs text-muted-foreground mb-4">/</p>

              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-border text-[11px] font-semibold text-muted-foreground uppercase">
                    <th className="py-2.5">Descuento</th>
                    <th className="py-2.5 text-right">Aplicaciones</th>
                    <th className="py-2.5 text-right">Bonificado</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border text-xs">
                  {reporte.descuentosAplicados?.map((desc, idx) => (
                    <tr key={idx}>
                      <td className="py-3.5 text-foreground font-medium">{desc.descuento}</td>
                      <td className="py-3.5 text-right text-muted-foreground">{desc.aplicaciones}</td>
                      <td className="py-3.5 text-right font-semibold text-foreground">
                        {formatearMoneda(desc.bonificado)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}