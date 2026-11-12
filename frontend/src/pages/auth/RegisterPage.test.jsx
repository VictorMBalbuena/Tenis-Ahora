import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RegisterPage from "./RegisterPage";
import axiosClient from "../../config/axiosClient";

vi.mock("../../config/axiosClient", () => ({ default: { post: vi.fn() } }));

function renderRegistro() {
  const vista = render(
    <MemoryRouter initialEntries={["/registro"]}>
      <Routes>
        <Route path="/registro" element={<RegisterPage />} />
        <Route path="/login" element={<p>Pantalla de login</p>} />
      </Routes>
    </MemoryRouter>
  );
  const c = vista.container;
  const campos = {
    nombre: screen.getByLabelText('Nombre'),
    apellido: screen.getByLabelText('Apellido'),
    dni: screen.getByLabelText('DNI'),
    email: screen.getByLabelText('Correo electrónico'),
    codigoArea: screen.getByLabelText('Cód. de área'),
    numeroTelefono: screen.getByLabelText('Número', { selector: '#reg-telefono' }),
    calle: screen.getByLabelText('Calle'),
    numeroCalle: screen.getByLabelText('Número', { selector: '#reg-numero-calle' }),
    password: c.querySelectorAll('input[type="password"]')[0],
    repetir: c.querySelectorAll('input[type="password"]')[1],
  };
  const completar = (valores) =>
    Object.entries(valores).forEach(([campo, valor]) => fireEvent.change(campos[campo], { target: { value: valor } }));
  const enviar = () => fireEvent.submit(campos.email.closest("form"));
  return { ...vista, campos, completar, enviar };
}

const DATOS_VALIDOS = {
  nombre: "Ana",
  apellido: "Gomez",
  dni: "30123456",
  codigoArea: "11",
  numeroTelefono: "12345678",
  calle: "Av. Corrientes",
  numeroCalle: "1234",
  email: "ana@tenisahora.com",
  password: "Clave123",
  repetir: "Clave123",
};

beforeEach(() => {
  axiosClient.post.mockReset();
  vi.spyOn(console, "error").mockImplementation(() => {});
});

describe("RegisterPage (CU74)", () => {
  it("las contraseñas no coinciden: muestra el error y no llama al backend", () => {
    const { completar, enviar } = renderRegistro();

    completar({ ...DATOS_VALIDOS, repetir: "OtraClave" });
    enviar();

    expect(screen.getByText("Las contraseñas no coinciden.")).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("contraseña de menos de 8 caracteres: muestra el error y no llama al backend", () => {
    const { completar, enviar } = renderRegistro();

    completar({ ...DATOS_VALIDOS, password: "abc1234", repetir: "abc1234" });
    enviar();

    expect(screen.getByText("La contraseña debe tener entre 8 y 72 caracteres.")).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("DNI con menos de 7 números: muestra el error y no llama al backend", () => {
    const { completar, enviar } = renderRegistro();

    completar({ ...DATOS_VALIDOS, dni: "123456" });
    enviar();

    expect(screen.getByText("El DNI debe tener 7 u 8 números.")).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });

  it("el DNI solo acepta números", () => {
    const { campos, completar } = renderRegistro();

    completar({ dni: "30.123-456abc" });

    expect(campos.dni).toHaveValue("30123456");
  });

  it("datos válidos: envía el registro con DNI, teléfono y domicilio atómicos a /auth/register y vuelve al login", async () => {
    axiosClient.post.mockResolvedValue({});
    const { completar, enviar } = renderRegistro();

    completar(DATOS_VALIDOS);
    enviar();

    await waitFor(() => expect(screen.getByText("Pantalla de login")).toBeInTheDocument());
    expect(axiosClient.post).toHaveBeenCalledWith("/auth/register", {
      nombre: "Ana",
      apellido: "Gomez",
      dni: "30123456",
      calle: "Av. Corrientes",
      numeroCalle: "1234",
      codigoArea: "11",
      numeroTelefono: "12345678",
      email: "ana@tenisahora.com",
      password: "Clave123",
    });
  });

  it("DNI o correo ya registrados (409): avisa cuál es el problema (CU73 A1)", async () => {
    axiosClient.post.mockRejectedValue({ response: { status: 409 } });
    const { completar, enviar } = renderRegistro();

    completar(DATOS_VALIDOS);
    enviar();

    expect(await screen.findByText("Ya existe una cuenta con ese DNI o correo electrónico.")).toBeInTheDocument();
    expect(screen.queryByText("Pantalla de login")).not.toBeInTheDocument();
  });

  it("si el backend rechaza un dato (400), muestra lo que explica el backend", async () => {
    axiosClient.post.mockRejectedValue({
      response: { status: 400, data: { campos: { email: "El correo electrónico no tiene un formato válido" } } },
    });
    const { completar, enviar } = renderRegistro();

    completar(DATOS_VALIDOS);
    enviar();

    expect(await screen.findByText("El correo electrónico no tiene un formato válido")).toBeInTheDocument();
  });

  it("demasiados pedidos desde la misma IP (429): muestra el aviso del backend", async () => {
    axiosClient.post.mockRejectedValue({
      response: { status: 429, data: { mensaje: "Hiciste demasiados pedidos. Esperá 60 minuto(s) antes de volver a intentar." } },
    });
    const { completar, enviar } = renderRegistro();

    completar(DATOS_VALIDOS);
    enviar();

    expect(await screen.findByText("Hiciste demasiados pedidos. Esperá 60 minuto(s) antes de volver a intentar.")).toBeInTheDocument();
  });

  it("cualquier otro error: mensaje general", async () => {
    axiosClient.post.mockRejectedValue(new Error("Network Error"));
    const { completar, enviar } = renderRegistro();

    completar(DATOS_VALIDOS);
    enviar();

    expect(await screen.findByText("Ocurrió un error al intentar crear la cuenta.")).toBeInTheDocument();
  });
});
