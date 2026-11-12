import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ChevronDown } from "lucide-react";
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
} from "@/components/ui/sidebar";
import { cn } from "cn";

const esActivo = (url, pathname) =>
  url === "/dashboard" ? pathname === "/dashboard" : pathname.startsWith(url);

// Entre varios subitems, "/canchas" es prefijo de "/canchas/nuevo": sin esto, los dos
// quedaban marcados activos a la vez. Nos quedamos solo con la URL más específica.
const subitemMasEspecifico = (subitems, pathname) =>
  subitems
    .filter((sub) => esActivo(sub.url, pathname))
    .sort((a, b) => b.url.length - a.url.length)[0]?.url;

export function NavMain({ items, groups }) {
  const location = useLocation();

  // Si se envían grupos modulares los usamos; si viene un array plano lo normalizamos
  const gruposNavegacion =
    groups || (items ? [{ label: null, items }] : []);

  // Un item con subitems arranca desplegado si la ruta actual cae en él o en alguno de sus hijos
  const [desplegados, setDesplegados] = useState(() => {
    const iniciales = new Set();
    gruposNavegacion.forEach((grupo) => {
      grupo.items?.forEach((item) => {
        if (item.subitems?.some((sub) => esActivo(sub.url, location.pathname))) {
          iniciales.add(item.title);
        }
      });
    });
    return iniciales;
  });

  const alternarDespliegue = (titulo) => {
    setDesplegados((prev) => {
      const siguiente = new Set(prev);
      if (siguiente.has(titulo)) {
        siguiente.delete(titulo);
      } else {
        siguiente.add(titulo);
      }
      return siguiente;
    });
  };

  if (gruposNavegacion.length === 0) return null;

  return (
    <div className="space-y-1 py-1">
      {gruposNavegacion.map((grupo, indice) => {
        if (!grupo.items || grupo.items.length === 0) return null;

        return (
          <SidebarGroup
            key={grupo.label || indice}
            className="px-2 py-1 group-data-[collapsible=icon]:p-0"
          >
            {grupo.label && (
              <SidebarGroupLabel className="px-2.5 h-7 text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70 group-data-[collapsible=icon]:hidden">
                {grupo.label}
              </SidebarGroupLabel>
            )}
            <SidebarMenu className="gap-0.5">
              {grupo.items.map((item) => {
                const tieneSubitems = item.subitems && item.subitems.length > 0;
                // El padre queda resaltado tanto en su propia URL como en la de cualquiera de sus
                // subitems (ej. "Disponibilidad" no cuelga de /canchas en la URL, pero es de la misma sección)
                const activo =
                  (item.url && esActivo(item.url, location.pathname)) ||
                  (tieneSubitems && item.subitems.some((sub) => esActivo(sub.url, location.pathname)));
                const abierto = desplegados.has(item.title);

                return (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      tooltip={item.title}
                      isActive={activo}
                      className={cn(
                        "h-9 px-2.5 rounded-full font-medium transition-colors gap-3",
                        activo
                          ? "!bg-primary !text-primary-foreground font-semibold"
                          : "text-sidebar-foreground/80 hover:bg-muted hover:text-foreground"
                      )}
                      render={<Link to={item.url} />}
                    >
                      {item.icon}
                      <span className="truncate flex-1">{item.title}</span>
                      {tieneSubitems && (
                        <span
                          role="button"
                          tabIndex={0}
                          aria-label={abierto ? `Ocultar opciones de ${item.title}` : `Mostrar opciones de ${item.title}`}
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            alternarDespliegue(item.title);
                          }}
                          onKeyDown={(e) => {
                            if (e.key === "Enter" || e.key === " ") {
                              e.preventDefault();
                              e.stopPropagation();
                              alternarDespliegue(item.title);
                            }
                          }}
                          className="shrink-0 rounded p-0.5 hover:bg-black/10"
                        >
                          <ChevronDown
                            size={14}
                            className={cn("transition-transform", abierto && "rotate-180")}
                          />
                        </span>
                      )}
                    </SidebarMenuButton>

                    {tieneSubitems && abierto && (
                      <SidebarMenuSub>
                        {(() => {
                          const urlActivo = subitemMasEspecifico(item.subitems, location.pathname);
                          return item.subitems.map((sub) => {
                            const subActivo = sub.url === urlActivo;
                            return (
                            <SidebarMenuSubItem key={sub.title}>
                              <SidebarMenuSubButton
                                isActive={subActivo}
                                className={
                                  subActivo
                                    ? "!bg-muted font-semibold text-primary hover:!bg-muted hover:text-primary"
                                    : "hover:bg-muted hover:text-foreground"
                                }
                                render={<Link to={sub.url} />}
                              >
                                <span className="truncate">{sub.title}</span>
                              </SidebarMenuSubButton>
                            </SidebarMenuSubItem>
                            );
                          });
                        })()}
                      </SidebarMenuSub>
                    )}
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroup>
        );
      })}
    </div>
  );
}
