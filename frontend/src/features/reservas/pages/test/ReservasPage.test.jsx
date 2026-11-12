import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { ReservasPage } from "../ReservasPage";
import { useAuthStore } from "@/store/authStore";
import axiosClient from "@/config/axiosClient";
import * as reservasApi from "../../api/reservasApi";

const mockedNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

vi.mock("@/store/authStore");
vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));
vi.mock("../../api/reservasApi", () => ({
  obtenerMisReservas: vi.fn(),
  buscarReservas: vi.fn(),
  obtenerReserva: vi.fn(),
  previsualizarCancelacion: vi.fn(),
  confirmarCancelacion: vi.fn(),
  modificarReserva: vi.fn(),
  obtenerReglasDeReserva: vi.fn().mockResolvedValue(null),
}));

describe("ReservasPage", () => {
  const mockReservasSocio = [
    {
      id: 101,
      numero: "R-000101",
      codigo: "RES-101",
      canchaId: 1,
      cancha: "Cancha 1 - Polvo de ladrillo",
      fecha: "2026-10-30",
      horaInicio: "18:00:00",
      horaFin: "19:00:00",
      estado: "CONFIRMADA",
      montoTotal: 8000,
      montoSenia: 4000,
    },
    {
      id: 102,
      numero: "R-000102",
      codigo: "RES-102",
      canchaId: 2,
      cancha: "Cancha 2 - Rápida",
      fecha: "2026-11-01",
      horaInicio: "10:00:00",
      horaFin: "11:00:00",
      estado: "PENDIENTE_PAGO",
      montoTotal: 8000,
      montoSenia: 4000,
    },
  ];

  const mockReservasGestion = [
    {
      id: 201,
      numero: "R-000201",
      codigo: "RES-201",
      canchaId: 1,
      cancha: "Cancha 1",
      fecha: "2026-10-29",
      horaInicio: "14:00:00",
      horaFin: "16:00:00",
      estado: "CONFIRMADA",
      montoTotal: 16000,
      montoSenia: 8000,
      jugadores: [{ titular: true, nombre: "Carlos", apellido: "Tevez", documento: "30123456" }],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    axiosClient.get.mockImplementation((ruta) => {
      if (typeof ruta === "string" && ruta.startsWith("/canchas")) {
        return Promise.resolve({
          data: {
            contenido: [
              { id: 1, identificador: "Cancha 1", tipoCanchaNombre: "Polvo de ladrillo", activa: true },
              { id: 2, identificador: "Cancha 2", tipoCanchaNombre: "Rápida", activa: true },
            ],
          },
        });
      }
      return Promise.resolve({ data: {} });
    });
    reservasApi.obtenerMisReservas.mockResolvedValue({
      contenido: mockReservasSocio,
      totalPaginas: 1,
    });
    reservasApi.buscarReservas.mockResolvedValue({
      contenido: mockReservasGestion,
      totalPaginas: 1,
    });
    reservasApi.obtenerReserva.mockResolvedValue(mockReservasSocio[0]);
    reservasApi.previsualizarCancelacion.mockResolvedValue({
      aTiempo: true,
      horasRestantes: 28,
      penalidadAplicable: 0,
      reembolsoEstimado: 4000,
    });
  });

  describe("Rol SOCIO", () => {
    it("muestra 'Mis reservas' cuando el usuario tiene rol SOCIO (mock directo)", async () => {
      useAuthStore.mockReturnValue({ rol: "SOCIO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      expect(screen.getByRole("heading", { name: /mis reservas/i })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /reservar cancha/i })).toBeInTheDocument();
      await waitFor(() => expect(reservasApi.obtenerMisReservas).toHaveBeenCalled());
    });

    it("funciona con auth store estándar user.rol = SOCIO", async () => {
      useAuthStore.mockReturnValue({ user: { email: "socio@tenis.com", rol: "SOCIO" } });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      expect(screen.getByRole("heading", { name: /mis reservas/i })).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /reservar cancha/i })).toBeInTheDocument();
      await waitFor(() => expect(reservasApi.obtenerMisReservas).toHaveBeenCalled());
    });

    it("navega a /reservas/nuevo al hacer click en 'Reservar cancha'", async () => {
      useAuthStore.mockReturnValue({ rol: "SOCIO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      fireEvent.click(screen.getByRole("button", { name: /reservar cancha/i }));
      expect(mockedNavigate).toHaveBeenCalledWith("/reservas/nuevo");
      await waitFor(() => expect(reservasApi.obtenerMisReservas).toHaveBeenCalled());
    });

    it("renderiza la lista de reservas cargadas de la API y contiene clases responsivas de CSS puro", async () => {
      const { container } = render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(reservasApi.obtenerMisReservas).toHaveBeenCalled();
        expect(screen.getAllByText(/R-000101|RES-101/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/Confirmada/i).length).toBeGreaterThan(0);
      });

      expect(container.querySelector(".hidden.md\\:block")).toBeInTheDocument();
      expect(container.querySelector(".md\\:hidden")).toBeInTheDocument();
    });

    it("navega a /reservas/:id al hacer click en 'Ver detalle'", async () => {
      useAuthStore.mockReturnValue({ rol: "SOCIO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000101|RES-101/i).length).toBeGreaterThan(0);
      });

      const botonesDetalle = screen.getAllByRole("button", { name: /ver detalle|ver/i });
      fireEvent.click(botonesDetalle[0]);

      expect(mockedNavigate).toHaveBeenCalledWith("/reservas/101");
    });

    it("abre el modal de cancelación al hacer click en 'Cancelar'", async () => {
      useAuthStore.mockReturnValue({ rol: "SOCIO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000101|RES-101/i).length).toBeGreaterThan(0);
      });

      const botonesCancelar = screen.getAllByRole("button", { name: /cancelar/i });
      fireEvent.click(botonesCancelar[0]);

      await waitFor(() => {
        expect(screen.getByRole("heading", { name: /cancelar reserva/i })).toBeInTheDocument();
      });
    });

    it("muestra estado vacío cuando no tiene reservas registradas", async () => {
      reservasApi.obtenerMisReservas.mockResolvedValueOnce({ contenido: [], totalPaginas: 0 });
      useAuthStore.mockReturnValue({ rol: "SOCIO" });

      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/no tenés reservas/i)).toBeInTheDocument();
      });
    });
  });

  describe("Rol EMPLEADO y ADMINISTRADOR", () => {
    it("muestra consola operativa de búsqueda cuando el usuario es EMPLEADO", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      expect(screen.getByRole("heading", { name: /gestión de reservas/i })).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/buscar por c[oó]digo o dni/i)).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /nueva reserva/i })).toBeInTheDocument();
      await waitFor(() => expect(reservasApi.buscarReservas).toHaveBeenCalled());
    });

    it("muestra consola operativa cuando el usuario es ADMINISTRADOR", async () => {
      useAuthStore.mockReturnValue({ user: { email: "admin@tenis.com", rol: "ADMINISTRADOR" } });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      expect(screen.getByRole("heading", { name: /gestión de reservas/i })).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/buscar por c[oó]digo o dni/i)).toBeInTheDocument();
      await waitFor(() => expect(reservasApi.buscarReservas).toHaveBeenCalled());
    });

    it("navega a /reservas/nuevo al hacer click en 'Nueva reserva'", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      fireEvent.click(screen.getByRole("button", { name: /nueva reserva/i }));
      expect(mockedNavigate).toHaveBeenCalledWith("/reservas/nuevo");
      await waitFor(() => expect(reservasApi.buscarReservas).toHaveBeenCalled());
    });

    it("renderiza la tabla y tarjetas con responsividad pura en CSS", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      const { container } = render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalled();
        expect(screen.getAllByText(/R-000201|RES-201/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/carlos tevez/i).length).toBeGreaterThan(0);
      });

      expect(container.querySelector(".hidden.md\\:block")).toBeInTheDocument();
      expect(container.querySelector(".md\\:hidden")).toBeInTheDocument();
    });

    it("navega a /reservas/:id al hacer click en 'Ver detalle'", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000201|RES-201/i).length).toBeGreaterThan(0);
      });

      const btnDetalle = screen.getAllByRole("button", { name: /ver detalle/i })[0];
      fireEvent.click(btnDetalle);

      expect(mockedNavigate).toHaveBeenCalledWith("/reservas/201");
    });

    it("navega a /reservas/editar/:id al hacer click en 'Editar'", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000201|RES-201/i).length).toBeGreaterThan(0);
      });

      const btnEditar = screen.getAllByRole("button", { name: /editar/i })[0];
      fireEvent.click(btnEditar);

      expect(mockedNavigate).toHaveBeenCalledWith("/reservas/editar/201");
    });

    it("abre EliminarReservaDialog al hacer click en 'Eliminar' y llama a confirmarCancelacion", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000201|RES-201/i).length).toBeGreaterThan(0);
      });

      const btnEliminar = screen.getAllByRole("button", { name: /eliminar/i })[0];
      fireEvent.click(btnEliminar);

      expect(await screen.findByText(/¿Eliminar reserva\?/i)).toBeInTheDocument();

      const btnConfirmar = screen.getByRole("button", { name: /^eliminar$/i });
      fireEvent.click(btnConfirmar);

      await waitFor(() => {
        expect(reservasApi.confirmarCancelacion).toHaveBeenCalledWith(201);
      });
    });

    it("renderiza controles de paginación y permite navegar entre páginas", async () => {
      reservasApi.buscarReservas.mockResolvedValue({
        contenido: mockReservasGestion,
        totalPaginas: 2,
      });
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });

      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getAllByText(/R-000201|RES-201/i).length).toBeGreaterThan(0);
      });

      const btnAnterior = screen.getByRole("button", { name: /anterior/i });
      const btnSiguiente = screen.getByRole("button", { name: /siguiente/i });

      expect(btnAnterior).toBeDisabled();
      expect(btnSiguiente).not.toBeDisabled();
      expect(screen.getByText("Página 1 de 2")).toBeInTheDocument();

      fireEvent.click(btnSiguiente);

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalledWith(
          expect.objectContaining({ page: 1 })
        );
      });
    });

    it("renderiza el selector de canchas con opción por defecto 'Cancha: Todas'", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );
      await waitFor(() => {
        expect(screen.getByText("Cancha: Todas")).toBeInTheDocument();
      });
    });

    it("llama a buscarReservas con canchaId al seleccionar una cancha del filtro", async () => {
      const user = userEvent.setup();
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText("Cancha: Todas")).toBeInTheDocument();
      });

      await user.click(screen.getByText("Cancha: Todas"));
      const opcion = await screen.findByRole("option", { name: "Cancha 1" });
      expect(opcion).toHaveTextContent("Cancha 1");
      await user.click(opcion);

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalledWith(
          expect.objectContaining({ canchaId: 1 })
        );
      });
    });

    it("inicializa canchaId y fecha desde los parámetros de la URL", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter initialEntries={["/reservas?canchaId=2&fecha=2026-10-22"]}>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalledWith(
          expect.objectContaining({
            canchaId: 2,
            fecha: "2026-10-22",
          })
        );
      });
    });

    it("el botón 'Limpiar' resetea el filtro de cancha a TODOS", async () => {
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });
      render(
        <MemoryRouter initialEntries={["/reservas?canchaId=1"]}>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalledWith(
          expect.objectContaining({ canchaId: 1 })
        );
      });

      const btnLimpiar = await screen.findByRole("button", { name: /limpiar/i });
      fireEvent.click(btnLimpiar);

      await waitFor(() => {
        expect(screen.getByText("Cancha: Todas")).toBeInTheDocument();
        expect(reservasApi.buscarReservas).toHaveBeenLastCalledWith(
          expect.not.objectContaining({ canchaId: expect.anything() })
        );
      });
    });

    it("recupera filtros guardados en sessionStorage al volver a montar la vista", async () => {
      sessionStorage.setItem(
        "filtros_gestion_reservas",
        JSON.stringify({
          canchaId: "2",
          fecha: "2026-11-15",
          estado: "CONFIRMADA",
          terminoBuscado: "30123456",
          busqueda: "30123456",
          filtroRapido: "TODOS",
          pagina: 0,
        })
      );
      useAuthStore.mockReturnValue({ rol: "EMPLEADO" });

      render(
        <MemoryRouter>
          <ReservasPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(reservasApi.buscarReservas).toHaveBeenCalledWith(
          expect.objectContaining({
            canchaId: 2,
            fecha: "2026-11-15",
            estado: "CONFIRMADA",
            documento: "30123456",
          })
        );
      });
    });
  });
});
