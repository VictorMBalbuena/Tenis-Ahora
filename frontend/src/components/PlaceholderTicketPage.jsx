import { Link, useLocation } from "react-router-dom";
import {
  ExternalLink,
  ArrowLeft,
  Calendar,
  LayoutGrid,
  Package,
  Coins,
  GraduationCap,
  ClipboardList,
  Trophy,
  Shield,
  Dumbbell,
  Clock,
} from "lucide-react";
import {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
  EmptyContent,
} from "@/components/ui/empty";
import { buttonVariants } from "@/components/ui/button";

export const MODULOS_JIRA = {
  "/canchas": {
    ticket: "SCRUM-32",
    titulo: "Canchas y Disponibilidad",
    resumen: "FRONT 04 — Pantallas Canchas y Disponibilidad",
    estado: "In Review",
    rama: "feature/TN-F04-Canchas",
    responsable: "Victor Manuel Balbuena",
    icono: LayoutGrid,
    mensaje: "Falta mergear branch a develop",
  },
  "/insumos": {
    ticket: "SCRUM-33",
    titulo: "Insumos",
    resumen: "FRONT 05 — Pantallas Insumos / Stock",
    estado: "In Review",
    rama: "feature/TN-F05-PantallasInsumos",
    responsable: "Lautaro Aguirre",
    icono: Package,
    mensaje: "Falta mergear branch a develop",
  },
  "/reservas": {
    ticket: "SCRUM-34",
    titulo: "Gestión de Reservas",
    resumen: "FRONT 06 — Flujo de Reserva",
    estado: "To Do",
    rama: "feature/TN-B06-Reservas",
    responsable: "Franco Herrera",
    icono: Calendar,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/cobros": {
    ticket: "SCRUM-35",
    titulo: "Caja y Cobranzas",
    resumen: "FRONT 07 — Pantalla de Cobro (seña 50%) y FRONT 13 — Recibos",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Victor Manuel Balbuena",
    icono: Coins,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/profesores": {
    ticket: "SCRUM-36",
    titulo: "Profesores y Staff",
    resumen: "FRONT 08 — Pantallas Profesores",
    estado: "In Progress",
    rama: "feature/TN-F08-PantallasProfesores",
    responsable: "Lautaro Aguirre",
    icono: ClipboardList,
    mensaje: "En desarrollo / falta mergear branch",
  },
  "/entrenadores": {
    ticket: "SCRUM-37",
    titulo: "Entrenadores",
    resumen: "FRONT 09 — Pantallas Entrenadores y certificación",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Franco Herrera",
    icono: Dumbbell,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/clases": {
    ticket: "SCRUM-38",
    titulo: "Gestión de Clases",
    resumen: "FRONT 10 — Pantallas Clases",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Lautaro Aguirre",
    icono: GraduationCap,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/entrenamientos": {
    ticket: "SCRUM-39",
    titulo: "Entrenamientos y Asistencia",
    resumen: "FRONT 11 — Pantallas Entrenamientos y asistencia",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Lautaro Aguirre",
    icono: Dumbbell,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/torneos": {
    ticket: "SCRUM-40",
    titulo: "Torneos y Ligas",
    resumen: "FRONT 12 — Pantallas Torneos",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Franco Herrera",
    icono: Trophy,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
  "/admin": {
    ticket: "SCRUM-43",
    titulo: "Panel de Administración",
    resumen: "FRONT 15 — Pantallas Reportes",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Victor Manuel Balbuena",
    icono: Shield,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  },
};

export default function PlaceholderTicketPage({ moduloKey }) {
  const location = useLocation();
  const rutaActual = moduloKey || location.pathname;
  const config = MODULOS_JIRA[rutaActual] || {
    ticket: "SCRUM",
    titulo: "Módulo en Construcción",
    resumen: "Módulo del Sistema Deportivo Tenis Ahora",
    estado: "To Do",
    rama: "Pendiente",
    responsable: "Equipo Tenis Ahora",
    icono: Clock,
    mensaje: "Ticket pendiente de desarrollo en frontend",
  };

  const Icono = config.icono || Clock;
  const urlJira = `https://tenisahora2026.atlassian.net/browse/${config.ticket}`;

  return (
    <div className="mx-auto w-full max-w-xl py-8 sm:py-14 text-center">
      {/* Título accesible para WCAG */}
      <h1 className="sr-only">
        {config.titulo} - {config.ticket}
      </h1>

      <Empty className="p-8 sm:p-12 border-border bg-card shadow-2xs">
        <EmptyHeader className="max-w-md">
          <EmptyMedia variant="icon" className="mb-2">
            <Icono className="size-5" />
          </EmptyMedia>

          <div className="inline-flex items-center gap-2 mb-1">
            <span className="font-mono text-xs font-semibold px-2 py-0.5 rounded-md bg-muted border border-border text-foreground">
              {config.ticket}
            </span>
            <span className="text-[11px] font-medium px-2 py-0.5 rounded-md border border-border text-muted-foreground">
              {config.estado}
            </span>
          </div>

          <EmptyTitle className="text-xl font-bold tracking-tight text-foreground font-sans">
            {config.titulo}
          </EmptyTitle>

          <EmptyDescription className="mt-1 space-y-1.5 text-xs sm:text-sm text-muted-foreground">
            <span className="block font-medium text-foreground/80">
              {config.resumen}
            </span>
            <span className="flex flex-wrap items-center justify-center gap-1.5 text-xs">
              <span>{config.mensaje}</span>
              <span className="text-muted-foreground/40">•</span>
              <span className="font-mono text-foreground/90 font-medium">{config.rama}</span>
              <span className="text-muted-foreground/40">•</span>
              <span className="text-foreground/90">{config.responsable}</span>
            </span>
          </EmptyDescription>
        </EmptyHeader>

        <EmptyContent className="mt-6 flex items-center justify-center gap-3">
          <a
            href={urlJira}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`Ver ticket ${config.ticket} en Jira`}
            className={buttonVariants({ variant: "outline", className: "rounded-lg shadow-2xs gap-2" })}
          >
            <ExternalLink className="size-3.5" />
            <span>Ver en Jira</span>
          </a>

          <Link
            to="/dashboard"
            aria-label="Volver al inicio"
            className={buttonVariants({ variant: "default", className: "rounded-lg shadow-2xs gap-2" })}
          >
            <ArrowLeft className="size-3.5" />
            <span>Volver al Inicio</span>
          </Link>
        </EmptyContent>
      </Empty>
    </div>
  );
}
