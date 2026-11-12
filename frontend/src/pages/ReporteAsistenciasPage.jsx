import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Printer, Download, BarChart2, AlertCircle, FileSearch } from "lucide-react";
import axiosClient from "@/config/axiosClient";
import { finDelMes, inicioDelMes } from "@/lib/fechas";
import { KpiCard } from "@/components/ui/kpi-card";

export default function ReporteAsistenciasPage() {
  const [periodo, setPeriodo] = useState("Este mes");
  const [desde, setDesde] = useState(inicioDelMes);
  const [hasta, setHasta] = useState(finDelMes);
  const [actividad, setActividad] = useState("Todas");

  const [reporte, setReporte] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");
  const [sinDatos, setSinDatos] = useState(false);

  const formatearNumero = (valor) => Number(valor || 0).toLocaleString("es-AR");

  const obtenerReporte = async (fechaDesde, fechaHasta, actividadFiltro) => {
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
      const { data } = await axiosClient.get("/reportes/asistencias", {
        params: { desde: fechaDesde, hasta: fechaHasta, actividad: actividadFiltro },
      });

      if (
        !data ||
        data.asistenciasRegistradas === 0 ||
        (Array.isArray(data.detalleAlumnos) && data.detalleAlumnos.length === 0)
      ) {
        setSinDatos(true);
        setReporte(null);
      } else {
        setSinDatos(false);
        setReporte(data);
      }
    } catch (err) {
      setError(err.response?.data?.mensaje || "No se pudo obtener el reporte de asistencias.");
    } finally {
      setCargando(false);
    }
  };

  const handleGenerar = (e) => {
    e.preventDefault();
    obtenerReporte(desde, hasta, actividad);
  };

  const handleLimpiar = () => {
    setPeriodo("Este mes");
    setDesde(inicioDelMes());
    setHasta(finDelMes());
    setActividad("Todas");
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
          / <span className="font-medium text-foreground">Asistencias</span>
        </div>
      </div>

      <div className="flex justify-between items-center mb-6">
        <h1 className="text-right text-[28px] font-bold text-foreground tracking-tight">
          Reporte de asistencias
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
            <label htmlFor="asistencias-desde" className="block text-xs font-semibold text-foreground mb-2">
              Desde
            </label>
            <input
              id="asistencias-desde"
              type="date"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="asistencias-hasta" className="block text-xs font-semibold text-foreground mb-2">
              Hasta
            </label>
            <input
              id="asistencias-hasta"
              type="date"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            />
          </div>

          <div>
            <label htmlFor="asistencias-actividad" className="block text-xs font-semibold text-foreground mb-2">
              Actividad
            </label>
            <select
              id="asistencias-actividad"
              value={actividad}
              onChange={(e) => setActividad(e.target.value)}
              className="w-full border border-input rounded-full px-4 py-2 text-sm bg-background text-foreground focus:outline-none focus:border-ring"
            >
              <option value="Todas">Todas</option>
              <option value="Iniciacion adultos">Iniciacion adultos</option>
              <option value="Infantiles 8-12">Infantiles 8-12</option>
              <option value="Intermedio">Intermedio</option>
              <option value="Competencia">Competencia</option>
              <option value="Fisico competitivo">Fisico competitivo</option>
              <option value="Pretemporada">Pretemporada</option>
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
            No se registraron asistencias a clases ni entrenamientos entre las fechas indicadas.
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
            <KpiCard etiqueta="Asistencias registradas" valor={formatearNumero(reporte.asistenciasRegistradas)} detalle={reporte.variacionAsistencias} detalleClase="font-medium text-primary" />

            <KpiCard etiqueta="Tasa de asistencia" valor={reporte.tasaAsistencia} detalle={reporte.variacionTasa} detalleClase="font-medium text-primary" />

            <KpiCard etiqueta="Ausencias" valor={formatearNumero(reporte.ausencias)} />

            <KpiCard etiqueta="Alumnos activos" valor={formatearNumero(reporte.alumnosActivos)} />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground mb-5">Asistencia por actividad</h2>
              <div className="space-y-4">
                {reporte.porActividad?.map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-xs mb-1.5">
                      <span className="text-foreground font-medium">{item.nombre}</span>
                      <span className="font-semibold text-foreground">{item.porcentaje}%</span>
                    </div>
                    <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                      <div style={{ width: `${item.porcentaje}%` }} className="h-full bg-primary rounded-full" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-card border border-border rounded-2xl p-6">
              <h2 className="text-base font-bold text-foreground mb-5">Asistencia por profesor / entrenador</h2>
              <div className="space-y-4">
                {reporte.porProfesor?.map((item, idx) => (
                  <div key={idx}>
                    <div className="flex justify-between text-xs mb-1.5">
                      <span className="text-foreground font-medium">{item.nombre}</span>
                      <span className="font-semibold text-foreground">{item.porcentaje}%</span>
                    </div>
                    <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                      <div style={{ width: `${item.porcentaje}%` }} className="h-full bg-primary rounded-full" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="bg-card border border-border rounded-2xl p-6">
            <h2 className="text-base font-bold text-foreground">Detalle por alumno</h2>
            <p className="text-xs text-muted-foreground mb-5">Top 6 por cantidad de asistencias</p>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-border text-[11px] font-semibold text-muted-foreground uppercase">
                    <th className="py-3">Alumno</th>
                    <th className="py-3">Actividad</th>
                    <th className="py-3 text-right">Clases</th>
                    <th className="py-3 text-right">Presentes</th>
                    <th className="py-3 text-right">Ausentes</th>
                    <th className="py-3 text-right">Tasa</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border text-xs">
                  {reporte.detalleAlumnos?.map((fila, idx) => (
                    <tr key={idx}>
                      <td className="py-3.5">
                        <div className="flex items-center gap-3">
                          <span className="w-8 h-8 rounded-full bg-muted text-foreground font-semibold text-[11px] flex items-center justify-center">
                            {fila.iniciales}
                          </span>
                          <span className="font-semibold text-foreground">{fila.alumno}</span>
                        </div>
                      </td>
                      <td className="py-3.5 text-muted-foreground">{fila.actividad}</td>
                      <td className="py-3.5 text-right text-foreground">{fila.clases}</td>
                      <td className="py-3.5 text-right text-primary font-medium">{fila.presentes}</td>
                      <td className={`py-3.5 text-right font-medium ${fila.ausentes > 0 ? "text-destructive" : "text-muted-foreground"}`}>
                        {fila.ausentes}
                      </td>
                      <td className="py-3.5 text-right font-semibold text-foreground">{fila.tasa}</td>
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