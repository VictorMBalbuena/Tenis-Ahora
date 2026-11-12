import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  BarChart2,
  Calendar,
  ClipboardList,
  Coins,
  Dumbbell,
  GraduationCap,
  Home,
  LayoutGrid,
  Package,
  Tag,
  Trophy,
  ShieldUser,
  UserCog,
  Users,
} from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { NavMain } from "@/components/nav-main";
import { NavUser } from "@/components/nav-user";
import { PelotaTenisIcon } from "@/components/ui/PelotaTenisIcon";
import { useAuthStore } from "@/store/authStore";

export function AppSidebar(props) {
  const user = useAuthStore((state) => state.user);

  // Construcción modular de navegación por rol con etiquetas OneWord
  const gruposNavegacion = useMemo(() => {
    if (!user) return [];

    const rol = user.rol;
    const esAdmin = rol === "ADMINISTRADOR";
    const esPersonal = rol === "EMPLEADO" || esAdmin;
    const esInstructor = rol === "PROFESOR" || rol === "ENTRENADOR" || esAdmin;
    const esSocio = rol === "SOCIO";

    const grupos = [];

    // Flujo del Socio: Grupo único OneWord "Club"
    if (esSocio) {
      grupos.push({
        label: "Club",
        items: [
          {
            title: "Inicio",
            url: "/dashboard",
            icon: <Home className="size-4" />,
          },
          {
            title: "Mis Reservas",
            url: "/reservas",
            icon: <Calendar className="size-4" />,
            subitems: [{ title: "Nueva reserva", url: "/reservas/nuevo" }],
          },
          {
            title: "Clases",
            url: "/clases",
            icon: <GraduationCap className="size-4" />,
            subitems: [
              { title: "Mis clases", url: "/clases/mias" },
              { title: "Disponibles", url: "/clases/disponibles" },
            ],
          },
          {
            title: "Entrenamientos",
            url: "/entrenamientos",
            icon: <Dumbbell className="size-4" />,
            subitems: [
              { title: "Mis entrenamientos", url: "/entrenamientos/mios" },
              { title: "Disponibles", url: "/entrenamientos/disponibles" },
            ],
          },
          {
            title: "Torneos",
            url: "/torneos",
            icon: <Trophy className="size-4" />,
            subitems: [
              { title: "Abiertos", url: "/torneos/abiertos" },
              { title: "Mis torneos", url: "/torneos/mios" },
            ],
          },
          {
            title: "Mis pagos",
            url: "/mis-pagos",
            icon: <Coins className="size-4" />,
          },
        ],
      });
      return grupos;
    }

    // Para Personal, Instructores y Admin: grupo base OneWord "General"
    grupos.push({
      label: "General",
      items: [
        {
          title: "Inicio",
          url: "/dashboard",
          icon: <Home className="size-4" />,
        },
      ],
    });

    if (esPersonal) {
      grupos.push({
        label: "Operación",
        items: [
          {
            title: "Reservas",
            url: "/reservas",
            icon: <Calendar className="size-4" />,
            subitems: [{ title: "Nueva reserva", url: "/reservas/nuevo" }],
          },
          {
            title: "Canchas",
            url: "/canchas",
            icon: <LayoutGrid className="size-4" />,
            subitems: [
              { title: "Nueva cancha", url: "/canchas/nuevo" },
              { title: "Disponibilidad", url: "/disponibilidad" },
            ],
          },
          {
            title: "Insumos",
            url: "/insumos",
            icon: <Package className="size-4" />,
            subitems: [
              { title: "Nuevo insumo", url: "/insumos/nuevo" },
              { title: "Categorías", url: "/insumos/categorias" },
            ],
          },
          {
            title: "Cobros",
            url: "/cobros",
            icon: <Coins className="size-4" />,
            subitems: [
              { title: "Registrar cobro", url: "/cobros/nuevo" },
              { title: "Recibos", url: "/recibos" },
            ],
          },
        ],
      });

      grupos.push({
        label: "Gestión",
        items: [
          {
            title: "Usuarios",
            url: "/usuarios",
            icon: <Users className="size-4" />,
          },
          {
            title: "Profesores",
            url: "/profesores",
            icon: <ClipboardList className="size-4" />,
            subitems: [{ title: "Nuevo profesor", url: "/profesores/nuevo" }],
          },
          {
            title: "Entrenadores",
            url: "/entrenadores",
            icon: <UserCog className="size-4" />,
            subitems: [{ title: "Nuevo entrenador", url: "/entrenadores/nuevo" }],
          },
          {
            title: "Torneos",
            url: "/torneos",
            icon: <Trophy className="size-4" />,
            subitems: [
              { title: "Abiertos", url: "/torneos/abiertos" },
              { title: "Nuevo torneo", url: "/torneos/nuevo" },
            ],
          },
        ],
      });
    }

    if (esInstructor || esPersonal) {
      const itemsDeporte = [];
      if (rol !== "ENTRENADOR") {
        itemsDeporte.push({
          title: "Gestión de Clases",
          url: "/clases",
          icon: <GraduationCap className="size-4" />,
          subitems: esPersonal
            ? [{ title: "Nueva clase", url: "/clases/nueva" }]
            : [{ title: "Mis clases", url: "/clases" }],
        });
      }
      if (rol !== "PROFESOR") {
        itemsDeporte.push({
          title: "Entrenamientos",
          url: "/entrenamientos",
          icon: <Dumbbell className="size-4" />,
          subitems: esPersonal
            ? [{ title: "Nuevo entrenamiento", url: "/entrenamientos/nuevo" }]
            : [{ title: "Mis entrenamientos", url: "/entrenamientos" }],
        });
      }

      grupos.push({
        label: "Deportes",
        items: itemsDeporte,
      });
    }

    if (esAdmin) {
      grupos.push({
        label: "Administración",
        items: [
          {
            title: "Reportes",
            url: "/reportes",
            icon: <BarChart2 className="size-4" />,
            subitems: [
              { title: "Ingresos", url: "/reportes/ingresos" },
              { title: "Asistencias", url: "/reportes/asistencias" },
              { title: "Reservas", url: "/reportes/reservas" },
            ],
          },
          {
            title: "Descuentos",
            url: "/descuentos",
            icon: <Tag className="size-4" />,
          },
          {
            title: "Roles y permisos",
            url: "/roles",
            icon: <ShieldUser className="size-4" />,
          },
        ],
      });
    }

    return grupos;
  }, [user]);

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader className="p-2.5 group-data-[collapsible=icon]:!p-2">
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              size="lg"
              className="h-14 px-2.5 rounded-xl hover:bg-sidebar-accent transition-colors gap-3 group-data-[collapsible=icon]:!h-10 group-data-[collapsible=icon]:!w-10 group-data-[collapsible=icon]:!p-0 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:!mx-auto"
              render={<Link to="/dashboard" aria-label="Ir al panel principal de Tenis Ahora" />}
            >
              <PelotaTenisIcon className="!size-9 shrink-0 group-data-[collapsible=icon]:!size-8 transition-transform hover:scale-105 duration-200" />
              <div className="grid flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:hidden">
                <span className="truncate font-bold tracking-tight text-foreground font-sans text-sm">Tenis Ahora</span>
                <span className="truncate text-[11px] font-medium text-muted-foreground">Club & Sistema Deportivo</span>
              </div>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>

      <SidebarContent>
        <NavMain groups={gruposNavegacion} />
      </SidebarContent>

      <SidebarFooter className="p-3 sm:p-3.5 border-t border-sidebar-border group-data-[collapsible=icon]:p-1.5">
        <NavUser user={user} />
      </SidebarFooter>
    </Sidebar>
  );
}
