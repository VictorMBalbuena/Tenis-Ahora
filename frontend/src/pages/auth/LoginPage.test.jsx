import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import LoginPage from "./LoginPage";
import axiosClient from "../../config/axiosClient";
import { useAuthStore } from "../../store/authStore";
import { obtenerMiPerfil } from "@/features/perfil/api/perfilApi";

vi.mock("../../config/axiosClient", () => ({ default: { post: vi.fn() } }));
vi.mock("jwt-decode", () => ({ jwtDecode: vi.fn(() => ({ sub: "3", rol: "SOCIO" })) }));
vi.mock("@/features/perfil/api/perfilApi", () => ({
  obtenerMiPerfil: vi.fn(() => Promise.reject(new Error("sin perfil por defecto"))),
}));

function renderLogin() {
  const vista = render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<p>Inicio del sistema</p>} />
      </Routes>
    </MemoryRouter>
  );
  const email = vista.container.querySelector('input[type="email"]');
  const password = vista.container.querySelector('input[type="password"]');
  const enviar = () => fireEvent.submit(email.closest("form"));
  return { ...vista, email, password, enviar };
}

beforeEach(() => {
  axiosClient.post.mockReset();
  vi.spyOn(console, "error").mockImplementation(() => {});
});

describe("LoginPage (CU71)", () => {
  it("email vacío: muestra el error y no llama al backend", () => {
    const { enviar } = renderLogin();

    enviar();

    expect(screen.getByText("El correo electrónico es obligatorio.")).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("email mal formado: muestra el error y no llama al backend", () => {
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    expect(screen.getByText("Ingresá un formato válido (ej: nombre@correo.com).")).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("login correcto: guarda la sesión con el rol del token y va al inicio", async () => {
    axiosClient.post.mockResolvedValue({ data: { accessToken: "access" } });
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    await waitFor(() => expect(screen.getByText("Inicio del sistema")).toBeInTheDocument());
    expect(axiosClient.post).toHaveBeenCalledWith("/auth/login", {
      email: "socio@tenisahora.com",
      password: "TenisAhora2026!",
    });
    const estado = useAuthStore.getState();
    expect(estado.token).toBe("access");
    // El refresh token no pasa por el JavaScript: va en una cookie httpOnly
    expect(estado).not.toHaveProperty("refreshToken");
    // El sub del token es el id de la persona: lo necesitan las pantallas que
    // filtran por "lo mio" y Mi perfil.
    expect(estado.user).toEqual({ id: 3, email: "socio@tenisahora.com", rol: "SOCIO" });
  });

  it("credenciales incorrectas o usuario deshabilitado (401): mensaje general (CU71 A1 y A2)", async () => {
    axiosClient.post.mockRejectedValue({ response: { status: 401 } });
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "mala" } });
    enviar();

    expect(await screen.findByText("Correo o contraseña incorrectos.")).toBeInTheDocument();
    expect(useAuthStore.getState().token).toBeNull();
  });

  it("cuenta bloqueada por intentos fallidos (429): muestra el mensaje del backend (CU71 A3)", async () => {
    axiosClient.post.mockRejectedValue({
      response: { status: 429, data: { mensaje: "Demasiados intentos fallidos. Esperá 15 minuto(s) antes de volver a intentar." } },
    });
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    expect(await screen.findByText("Demasiados intentos fallidos. Esperá 15 minuto(s) antes de volver a intentar.")).toBeInTheDocument();
    expect(useAuthStore.getState().token).toBeNull();
  });

  it("429 sin mensaje: aviso general de espera", async () => {
    axiosClient.post.mockRejectedValue({ response: { status: 429 } });
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "x" } });
    enviar();

    expect(await screen.findByText("Demasiados intentos. Esperá unos minutos antes de volver a intentar.")).toBeInTheDocument();
  });

  it("sin conexión con el servidor: mensaje de error de conexión", async () => {
    axiosClient.post.mockRejectedValue(new Error("Network Error"));
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    expect(await screen.findByText("No se pudo conectar con el servidor. Intentá nuevamente.")).toBeInTheDocument();
  });

  it("mientras espera la respuesta deshabilita el botón", async () => {
    let responder;
    axiosClient.post.mockReturnValue(new Promise((resolve) => { responder = resolve; }));
    const { email, password, enviar } = renderLogin();

    fireEvent.change(email, { target: { value: "socio@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    expect(await screen.findByRole("button", { name: "Ingresando..." })).toBeDisabled();
    responder({ data: { accessToken: "access" } });
    await waitFor(() => expect(screen.getByText("Inicio del sistema")).toBeInTheDocument());
  });

  it("el botón del ojo muestra y oculta la contraseña", () => {
    const { container, password } = renderLogin();
    const ojo = password.parentElement.querySelector('button[type="button"]');

    fireEvent.click(ojo);
    expect(container.querySelector('input[type="text"]')).toBe(password);

    fireEvent.click(ojo);
    expect(password).toHaveAttribute("type", "password");
  });

  it("no muestra la opción de reservar como invitado", () => {
    renderLogin();

    expect(screen.queryByText(/reservar como invitado/i)).not.toBeInTheDocument();
  });

  it("al iniciar sesión consulta el perfil para poblar nombre y foto de inmediato", async () => {
    axiosClient.post.mockResolvedValue({ data: { accessToken: "access" } });
    obtenerMiPerfil.mockResolvedValueOnce({
      nombre: "Mariano",
      apellido: "Zabaleta",
      fotoUrl: "https://foto.jpg",
    });

    const { email, password, enviar } = renderLogin();
    fireEvent.change(email, { target: { value: "mariano@tenisahora.com" } });
    fireEvent.change(password, { target: { value: "TenisAhora2026!" } });
    enviar();

    await waitFor(() => {
      const user = useAuthStore.getState().user;
      expect(user?.nombre).toBe("Mariano");
      expect(user?.fotoUrl).toBe("https://foto.jpg");
    });
  });
});
