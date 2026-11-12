import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { Badge } from "@/components/ui/badge";
import { ChevronsUpDownIcon, LogOutIcon, SettingsIcon, UserRoundIcon } from "lucide-react";
import axiosClient from "../config/axiosClient";
import { useAuthStore } from "../store/authStore";
import { obtenerMiPerfil } from "../features/perfil/api/perfilApi";

export function NavUser({ user }) {
  const { isMobile } = useSidebar();
  const navigate = useNavigate();
  const logout = useAuthStore((state) => state.logout);
  const setUsuario = useAuthStore((state) => state.setUsuario);
  const email = user?.email;

  useEffect(() => {
    if (!email) return undefined;
    let activo = true;
    Promise.resolve(obtenerMiPerfil())
      .then((perfil) => {
        if (activo && perfil) {
          setUsuario({ nombre: perfil.nombre, apellido: perfil.apellido, fotoUrl: perfil.fotoUrl });
        }
      })
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, [email, setUsuario]);

  if (!user) return null;

  const inicial = user.email ? user.email.charAt(0).toUpperCase() : "U";
  const nombreCorto = user.nombre
    ? `${user.nombre} ${user.apellido ?? ""}`.trim()
    : user.email
      ? user.email.split("@")[0]
      : "Usuario";

  const handleLogout = async () => {
    try {
      await axiosClient.post("/auth/logout", {});
    } catch (err) {
      console.error("No se pudo revocar la sesión en el servidor:", err);
    } finally {
      logout();
      navigate("/login");
    }
  };

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <SidebarMenuButton
                size="lg"
                className="h-14 px-3.5 py-2.5 rounded-xl border border-sidebar-border/70 bg-sidebar-accent/30 hover:bg-sidebar-accent hover:border-sidebar-border transition-all gap-3 shadow-2xs group-data-[collapsible=icon]:!h-9 group-data-[collapsible=icon]:!w-9 group-data-[collapsible=icon]:!p-0 group-data-[collapsible=icon]:!mx-auto"
                aria-label={`Menú de usuario (${user.email})`}
              />
            }
          >
            <Avatar className="size-9 shrink-0 rounded-lg group-data-[collapsible=icon]:!flex">
              {user.fotoUrl && <AvatarImage src={user.fotoUrl} alt="" className="rounded-lg" />}
              <AvatarFallback className="bg-primary/20 text-foreground font-bold text-xs rounded-lg">
                {inicial}
              </AvatarFallback>
            </Avatar>
            <div className="flex flex-col min-w-0 flex-1 text-left justify-center leading-normal group-data-[collapsible=icon]:hidden">
              <span className="truncate font-semibold text-sm text-foreground leading-tight">
                {nombreCorto}
              </span>
              <span className="truncate text-xs text-muted-foreground leading-tight mt-0.5">
                {user.email}
              </span>
            </div>
            <ChevronsUpDownIcon className="ml-auto size-4 shrink-0 text-muted-foreground/70 group-data-[collapsible=icon]:hidden" />
          </DropdownMenuTrigger>

          <DropdownMenuContent
            className="w-64 p-2 rounded-xl shadow-lg border border-border"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={8}
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-3 p-2 text-left text-sm">
                  <Avatar className="size-9 rounded-lg">
                    {user.fotoUrl && <AvatarImage src={user.fotoUrl} alt="" className="rounded-lg" />}
                    <AvatarFallback className="bg-primary/20 text-foreground font-bold text-xs rounded-lg">
                      {inicial}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col min-w-0 flex-1 leading-tight">
                    <span className="truncate font-semibold text-foreground">{nombreCorto}</span>
                    <span className="truncate text-xs text-muted-foreground mt-0.5">{user.email}</span>
                  </div>
                  {user.rol && (
                    <Badge variant="outline" className="text-[10px] font-medium uppercase shrink-0">
                      {user.rol}
                    </Badge>
                  )}
                </div>
              </DropdownMenuLabel>
            </DropdownMenuGroup>

            <DropdownMenuSeparator className="my-1.5" />

            <DropdownMenuGroup>
              <DropdownMenuItem
                onClick={() => navigate("/perfil")}
                className="cursor-pointer py-2 px-2.5 rounded-lg gap-2"
              >
                <UserRoundIcon className="size-4" />
                <span className="font-medium text-sm">Mi perfil</span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => navigate("/configuracion")}
                className="cursor-pointer py-2 px-2.5 rounded-lg gap-2"
              >
                <SettingsIcon className="size-4" />
                <span className="font-medium text-sm">Configuración</span>
              </DropdownMenuItem>
            </DropdownMenuGroup>

            <DropdownMenuSeparator className="my-1.5" />

            <DropdownMenuItem
              onClick={handleLogout}
              className="text-destructive focus:bg-destructive/10 focus:text-destructive cursor-pointer py-2 px-2.5 rounded-lg gap-2"
            >
              <LogOutIcon className="size-4" />
              <span className="font-medium text-sm">Cerrar Sesión</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
