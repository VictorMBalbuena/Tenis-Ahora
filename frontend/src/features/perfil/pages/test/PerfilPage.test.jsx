import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { actualizarMiPerfil, obtenerMiPerfil, quitarMiFoto, subirMiFoto } from "../../api/perfilApi";
import { PerfilPage } from "../PerfilPage";

vi.mock("../../api/perfilApi", () => ({
  obtenerMiPerfil: vi.fn(),
  actualizarMiPerfil: vi.fn(),
  subirMiFoto: vi.fn(),
  quitarMiFoto: vi.fn(),
}));

vi.mock("@/components/ui/date-field", () => ({
  DateField: ({ name, value, onChange, id }) => (
    <input id={id} aria-label="Fecha de nacimiento" name={name} value={value} onChange={onChange} />
  ),
}));

const miPerfil = {
  id: 12,
  nombre: "Ana",
  apellido: "Gómez",
  dni: "30123456",
  fechaNacimiento: "1990-05-17",
  calle: "Av. Siempreviva",
  numeroCalle: "742",
  codigoArea: "11",
  numeroTelefono: "22786604",
  email: "ana@correo.com",
  fotoUrl: null,
  rol: "SOCIO",
  activo: true,
  fechaAlta: "2026-03-01",
  numeroSocio: "S-0012",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/perfil"]}>
      <PerfilPage />
    </MemoryRouter>,
  );
}

async function entrarAEditar() {
  await userEvent.click(await screen.findByRole("button", { name: /Editar datos/ }));
  return screen.findByLabelText("Correo electrónico");
}

describe("PerfilPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { id: 12, email: "ana@correo.com", rol: "SOCIO" } });
  });

  it("muestra el resumen de los datos propios y los de la cuenta", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();

    expect(await screen.findByText("Ana Gómez")).toBeInTheDocument();
    expect(screen.getAllByText("ana@correo.com").length).toBeGreaterThan(0);
    expect(screen.getByText(/S-0012/)).toBeInTheDocument();
    expect(screen.getAllByText("Socio").length).toBeGreaterThan(0);
  });

  it("el DNI se muestra como dato y, al editar, no se puede tocar", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();

    await entrarAEditar();

    expect(screen.getByLabelText("DNI")).toHaveAttribute("readonly");
  });

  it("guarda solo los campos que se tocaron", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    actualizarMiPerfil.mockResolvedValueOnce({ ...miPerfil, numeroTelefono: "44556677" });

    renderPage();
    await entrarAEditar();

    const telefono = screen.getByLabelText("Teléfono");
    await userEvent.clear(telefono);
    await userEvent.type(telefono, "44556677");
    await userEvent.click(screen.getByRole("button", { name: /Guardar cambios/ }));

    await waitFor(() => expect(actualizarMiPerfil).toHaveBeenCalledWith({ numeroTelefono: "44556677" }));
    expect(await screen.findByText("Se guardaron tus datos.")).toBeInTheDocument();
  });

  it("al guardar, vuelve al resumen", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    actualizarMiPerfil.mockResolvedValueOnce({ ...miPerfil, numeroTelefono: "44556677" });

    renderPage();
    await entrarAEditar();

    const telefono = screen.getByLabelText("Teléfono");
    await userEvent.clear(telefono);
    await userEvent.type(telefono, "44556677");
    await userEvent.click(screen.getByRole("button", { name: /Guardar cambios/ }));

    await waitFor(() => expect(actualizarMiPerfil).toHaveBeenCalled());
    expect(await screen.findByRole("button", { name: /Editar datos/ })).toBeInTheDocument();
  });

  it("si no se cambió nada, no llama al backend", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();
    await entrarAEditar();

    await userEvent.click(screen.getByRole("button", { name: /Guardar cambios/ }));

    expect(await screen.findByText("No había nada para guardar.")).toBeInTheDocument();
    expect(actualizarMiPerfil).not.toHaveBeenCalled();
  });

  it("no guarda con un correo inválido", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();
    const email = await entrarAEditar();
    await userEvent.clear(email);
    await userEvent.type(email, "no-es-correo");
    await userEvent.click(screen.getByRole("button", { name: /Guardar cambios/ }));

    expect(await screen.findByText(/no tiene un formato válido/)).toBeInTheDocument();
    expect(actualizarMiPerfil).not.toHaveBeenCalled();
  });

  it("si el correo ya lo usa otra persona, muestra lo que dice el backend", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    actualizarMiPerfil.mockRejectedValueOnce({
      response: {
        status: 409,
        data: { mensaje: "Ese correo ya está en uso.", campos: { email: "Ya está registrado." } },
      },
    });

    renderPage();
    const email = await entrarAEditar();
    await userEvent.clear(email);
    await userEvent.type(email, "otra@correo.com");
    await userEvent.click(screen.getByRole("button", { name: /Guardar cambios/ }));

    expect(await screen.findByText("Ese correo ya está en uso.")).toBeInTheDocument();
    expect(screen.getByText("Ya está registrado.")).toBeInTheDocument();
  });

  it("descartar vuelve al resumen con los datos que estaban guardados", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();
    await entrarAEditar();

    const telefono = screen.getByLabelText("Teléfono");
    await userEvent.clear(telefono);
    await userEvent.type(telefono, "11112222");
    await userEvent.click(screen.getByRole("button", { name: "Descartar cambios" }));

    expect(await screen.findByRole("button", { name: /Editar datos/ })).toBeInTheDocument();
    expect(screen.getByText("(11) 22786604")).toBeInTheDocument();
  });

  it("con foto ofrece cambiarla y quitarla", async () => {
    obtenerMiPerfil.mockResolvedValueOnce({ ...miPerfil, fotoUrl: "https://club/foto.webp" });

    renderPage();

    expect(await screen.findByRole("button", { name: /Cambiar foto/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Quitar/ })).toBeInTheDocument();
  });

  it("sin foto solo ofrece subirla", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();

    expect(await screen.findByRole("button", { name: /Subir foto/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Quitar/ })).not.toBeInTheDocument();
  });

  it("sube la foto elegida y avisa", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    subirMiFoto.mockResolvedValueOnce({ fotoUrl: "https://club/foto.webp" });

    renderPage();

    const archivo = new File(["x"], "yo.png", { type: "image/png" });
    await userEvent.upload(await screen.findByLabelText("Elegir una foto de perfil"), archivo);

    await waitFor(() => expect(subirMiFoto).toHaveBeenCalledWith(archivo));
    expect(await screen.findByText("Se actualizó tu foto de perfil.")).toBeInTheDocument();
  });

  it("rechaza una foto que no es imagen sin pedirle nada al backend", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);

    renderPage();

    const archivo = new File(["x"], "contrato.pdf", { type: "application/pdf" });
    await userEvent.upload(await screen.findByLabelText("Elegir una foto de perfil"), archivo);

    expect(await screen.findByText(/JPG, PNG o WebP/)).toBeInTheDocument();
    expect(subirMiFoto).not.toHaveBeenCalled();
  });

  it("quita la foto y avisa", async () => {
    obtenerMiPerfil.mockResolvedValueOnce({ ...miPerfil, fotoUrl: "https://club/foto.webp" });
    quitarMiFoto.mockResolvedValueOnce(undefined);

    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: /Quitar/ }));

    await waitFor(() => expect(quitarMiFoto).toHaveBeenCalled());
    expect(await screen.findByText("Se quitó tu foto de perfil.")).toBeInTheDocument();
  });

  it("deja el nombre y la foto en la sesión, para que los muestre el menú lateral", async () => {
    obtenerMiPerfil.mockResolvedValueOnce({ ...miPerfil, fotoUrl: "https://club/foto.webp" });

    renderPage();

    await screen.findByText("Ana Gómez");
    await waitFor(() =>
      expect(useAuthStore.getState().user).toMatchObject({
        nombre: "Ana",
        apellido: "Gómez",
        fotoUrl: "https://club/foto.webp",
      }),
    );
  });

  it("si no se pudo cargar el perfil, ofrece volver a intentar", async () => {
    obtenerMiPerfil.mockRejectedValueOnce({ response: undefined });

    renderPage();

    expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();

    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    await userEvent.click(screen.getByRole("button", { name: /Volver a intentar/ }));

    expect(await screen.findByText("Ana Gómez")).toBeInTheDocument();
  });

  it("aplica padding inferior para separar los controles del borde de pantalla", async () => {
    obtenerMiPerfil.mockResolvedValueOnce(miPerfil);
    const { container } = renderPage();
    await screen.findByText("Ana Gómez");
    const wrapper = container.firstChild;
    expect(wrapper.className).toMatch(/pb-1[02]/);
  });
});
