import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppSidebar } from "./app-sidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
import axiosClient from "../config/axiosClient";
import { useAuthStore } from "../store/authStore";

vi.mock("../config/axiosClient", () => ({ default: { post: vi.fn() } }));

function renderSidebar() {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <SidebarProvider>
        <AppSidebar />
      </SidebarProvider>
    </MemoryRouter>
  );
}

function loguearComo(rol) {
  useAuthStore.setState({
    token: "access",
    inicializado: true,
    user: { email: "usuario@tenisahora.com", rol },
  });
}

beforeEach(() => {
  axiosClient.post.mockReset();
});

describe("AppSidebar", () => {
  it("muestra el branding y el email del usuario logueado", () => {
    loguearComo("SOCIO");

    renderSidebar();

    expect(screen.getByText("Tenis Ahora")).toBeInTheDocument();
    expect(screen.getAllByText("usuario@tenisahora.com")[0]).toBeInTheDocument();
  });

  it("el socio ve Mis Reservas y no ve las opciones de administrador", () => {
    loguearComo("SOCIO");

    renderSidebar();

    expect(screen.getByRole("link", { name: /mis reservas/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /mis pagos/i })).toHaveAttribute("href", "/mis-pagos");
    expect(screen.queryByRole("link", { name: /panel admin/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /usuarios/i })).not.toBeInTheDocument();
  });

  it("el administrador ve Inicio, Gestión de Clases y Usuarios", () => {
    loguearComo("ADMINISTRADOR");

    renderSidebar();

    expect(screen.getByRole("link", { name: "Inicio" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /gestión de clases/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /usuarios/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /mis reservas/i })).not.toBeInTheDocument();
  });

  it("el empleado ve Usuarios pero no Reportes", () => {
    loguearComo("EMPLEADO");

    renderSidebar();

    expect(screen.getByRole("link", { name: /usuarios/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /reportes/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /mis reservas/i })).not.toBeInTheDocument();
  });

  it("el PROFESOR ve Gestión de Clases pero NO Entrenamientos", () => {
    loguearComo("PROFESOR");

    renderSidebar();

    expect(screen.getByRole("link", { name: /gestión de clases/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /entrenamientos/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /reportes/i })).not.toBeInTheDocument();
  });

  it("el ENTRENADOR ve Entrenamientos pero NO Gestión de Clases", () => {
    loguearComo("ENTRENADOR");

    renderSidebar();

    expect(screen.getByRole("link", { name: /entrenamientos/i })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /gestión de clases/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /reportes/i })).not.toBeInTheDocument();
  });
});
