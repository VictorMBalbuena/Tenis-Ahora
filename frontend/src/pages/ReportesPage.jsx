import React from "react";
import { useNavigate } from "react-router-dom";
import { CreditCard, Users, Calendar, Printer, ChevronRight, Loader2, AlertCircle } from "lucide-react";
import { LISTADOS } from "@/features/reportes/model/listados";
import { useImpresionDeListado } from "@/features/reportes/hooks/useImpresionDeListado";
import { useAuthStore } from "@/store/authStore";

export default function ReportesPage() {
  const navigate = useNavigate();
  const usuario = useAuthStore((estado) => estado.user);
  const { imprimir, trayendo, error, limpiarError } = useImpresionDeListado({ emitidoPor: usuario?.email });

  const reportesPrincipales = [
    {
      id: "ingresos",
      titulo: "Reporte de Ingresos",
      descripcion:
        "Cobros por alquiler, inscripciones y otros servicios, con apertura por medio de pago y descuentos aplicados.",
      icono: CreditCard,
      ruta: "/reportes/ingresos",
      destacado: false,
    },
    {
      id: "asistencias",
      titulo: "Reporte de Asistencias",
      descripcion:
        "Asistencia de usuarios a clases y entrenamientos, por profesor, actividad y periodo.",
      icono: Users,
      ruta: "/reportes/asistencias",
      destacado: true,
    },
    {
      id: "reservas",
      titulo: "Reporte de Reservas",
      descripcion:
        "Reservas por cancha, estado, ocupacion horaria y tasa de cancelacion.",
      icono: Calendar,
      ruta: "/reportes/reservas",
      destacado: false,
    },
  ];

  return (
    <div className="w-full font-sans">
      <div className="flex justify-between items-center mb-6 text-sm text-muted-foreground">
        <div>
          Administracion / <span className="font-medium text-foreground">Reportes</span>
        </div>
      </div>

      <div className="flex justify-between items-center mb-8">
        <h1 className="text-[28px] font-bold text-foreground tracking-tight">Reportes</h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {reportesPrincipales.map((rep) => {
          const IconComponent = rep.icono;
          return (
            <div
              key={rep.id}
              onClick={() => navigate(rep.ruta)}
              className={`bg-card rounded-2xl p-6 cursor-pointer transition-all flex flex-col justify-between ${
                rep.destacado
                  ? "border border-primary shadow-sm"
                  : "border border-border hover:border-primary"
              }`}
            >
              <div>
                <div className="w-11 h-11 rounded-xl bg-[var(--estado-brand-bg)] text-[var(--estado-brand-fg)] flex items-center justify-center mb-5">
                  <IconComponent size={20} />
                </div>
                <h2 className="text-lg font-bold text-foreground mb-2">{rep.titulo}</h2>
                <p className="text-sm text-muted-foreground leading-relaxed mb-6">
                  {rep.descripcion}
                </p>
              </div>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(rep.ruta);
                }}
                className="flex items-center gap-1 text-sm font-semibold text-primary hover:underline w-fit"
              >
                Generar <ChevronRight size={16} />
              </button>
            </div>
          );
        })}
      </div>

      <div className="bg-card border border-border rounded-2xl p-6">
        <h3 className="text-base font-bold text-foreground mb-1">
          Reportes adicionales del sistema
        </h3>
        <p className="text-sm text-muted-foreground mb-6">
          Salidas de impresion que exigen los casos de uso de cada modulo
        </p>

        {error && (
          <div
            role="alert"
            className="flex items-center gap-3 bg-[var(--estado-error-bg)] border border-[var(--estado-error-border)] text-[var(--estado-error-fg)] px-4 py-3 rounded-xl mb-4 text-sm"
          >
            <AlertCircle size={18} />
            <span className="flex-1">{error}</span>
            <button type="button" onClick={limpiarError} className="font-semibold hover:underline">
              Cerrar
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {LISTADOS.map((listado) => {
            const seEstaTrayendo = trayendo === listado.id;
            return (
              <div
                key={listado.id}
                className="flex items-center justify-between gap-3 border border-border rounded-xl px-4 py-3.5 hover:bg-muted/50 transition-colors"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium text-foreground">{listado.titulo}</p>
                  <span className="text-xs text-muted-foreground">{listado.descripcion}</span>
                </div>
                <button
                  type="button"
                  aria-label={`Imprimir ${listado.titulo}`}
                  onClick={() => imprimir(listado.id)}
                  disabled={trayendo !== null}
                  className="shrink-0 text-muted-foreground hover:text-foreground p-1.5 rounded-lg hover:bg-muted transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {seEstaTrayendo ? (
                    <Loader2 size={18} className="animate-spin" aria-hidden="true" />
                  ) : (
                    <Printer size={18} aria-hidden="true" />
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}