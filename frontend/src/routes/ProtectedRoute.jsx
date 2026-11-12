import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { AppSidebar } from "../components/app-sidebar";
import {
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ThemeToggle } from "@/components/theme-toggle";

export default function ProtectedRoute({ allowedRoles }) {
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);
  const inicializado = useAuthStore((state) => state.inicializado);

  // Al abrir la página todavía se está recuperando la sesión
  if (!inicializado) {
    return <div className="p-10 text-muted-foreground">Cargando...</div>;
  }

  // Si no hay token, redirigir al login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // Si la ruta exige roles específicos y el usuario no lo tiene
  if (allowedRoles && (!user || !allowedRoles.includes(user.rol))) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <TooltipProvider>
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset className="min-w-0 bg-background flex flex-col h-svh overflow-hidden">
          <header className="sticky top-0 z-10 flex h-14 shrink-0 items-center gap-2 border-b border-border bg-background/80 px-4 sm:px-6 backdrop-blur-md transition-[width,height] ease-linear">
            <div className="flex items-center gap-2 min-w-0">
              <SidebarTrigger className="-ml-1" />
            </div>

            <div className="ml-auto flex items-center gap-2">
              <ThemeToggle />
            </div>
          </header>

          <main className="flex-1 min-h-0 p-6 md:p-8 lg:p-10 overflow-y-auto">
            <div className="flex min-h-full w-full flex-col [&>*]:w-full [&>*]:min-w-0">
              <Outlet />
            </div>
          </main>
        </SidebarInset>
      </SidebarProvider>
    </TooltipProvider>
  );
}