import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import ProtectedRoute from "./ProtectedRoute";
import { useAuthStore } from "../store/authStore";

function renderEn(ruta) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route path="/login" element={<p>Pantalla de login</p>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<p>Inicio del sistema</p>} />
        </Route>
        <Route element={<ProtectedRoute allowedRoles={["EMPLEADO", "ADMINISTRADOR"]} />}>
          <Route path="/canchas" element={<p>ABM de canchas</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  it("mientras se recupera la sesión no redirige: muestra que está cargando", () => {
    useAuthStore.setState({ token: null, user: null, inicializado: false });

    renderEn("/dashboard");

    expect(screen.getByText("Cargando...")).toBeInTheDocument();
    expect(screen.queryByText("Pantalla de login")).not.toBeInTheDocument();
  });

  it("sin token redirige al login", () => {
    useAuthStore.setState({ token: null, user: null, inicializado: true });

    renderEn("/dashboard");

    expect(screen.getByText("Pantalla de login")).toBeInTheDocument();
    expect(screen.queryByText("Inicio del sistema")).not.toBeInTheDocument();
  });

  it("con token muestra la pantalla y el menu lateral", () => {
    useAuthStore.setState({ token: "access", inicializado: true, user: { email: "socio@tenisahora.com", rol: "SOCIO" } });

    renderEn("/dashboard");

    expect(screen.getByText("Inicio del sistema")).toBeInTheDocument();
    expect(screen.getByText("socio@tenisahora.com")).toBeInTheDocument();
  });

  it("con un rol permitido muestra la pantalla", () => {
    useAuthStore.setState({ token: "access", inicializado: true, user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" } });

    renderEn("/canchas");

    expect(screen.getByText("ABM de canchas")).toBeInTheDocument();
  });

  it("con un rol que no corresponde redirige al inicio", () => {
    useAuthStore.setState({ token: "access", inicializado: true, user: { email: "socio@tenisahora.com", rol: "SOCIO" } });

    renderEn("/canchas");

    expect(screen.getByText("Inicio del sistema")).toBeInTheDocument();
    expect(screen.queryByText("ABM de canchas")).not.toBeInTheDocument();
  });

  it("con token pero sin usuario no deja entrar a una pantalla con roles", () => {
    useAuthStore.setState({ token: "access", inicializado: true, user: null });

    renderEn("/canchas");

    expect(screen.queryByText("ABM de canchas")).not.toBeInTheDocument();
  });
});
