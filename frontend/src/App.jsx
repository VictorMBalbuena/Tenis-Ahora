import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import ProtectedRoute from "./routes/ProtectedRoute";
import { restaurarSesion } from "./config/sesion";
import { UsuariosPage } from "./features/usuarios";
import { ReservasPage, NuevaReservaPage, ReservaDetallePage, ReservaFormPage } from "./features/reservas";
import { InsumosPage, NuevoInsumoPage, EditarInsumoPage, InsumoDetallePage, CategoriasInsumoPage } from "./features/insumos";
import { TorneosPage, TorneoFormPage, TorneoDetallePage, TorneoFixturePage } from "./features/torneos";
import { ProfesoresPage, NuevoProfesorPage, EditarProfesorPage, ProfesorDetallePage } from "./features/profesores";
import { EntrenadoresPage, NuevoEntrenadorPage, EditarEntrenadorPage, EntrenadorDetallePage } from "./features/entrenadores";
import { CobrosPage, MisPagosPage, NuevoCobroPage, CobroDetallePage } from "./features/cobros";
import { RecibosPage, ReciboDetallePage } from "./features/recibos";
import { PerfilPage, ConfiguracionPage } from "./features/perfil";
import { DescuentosPage } from "./features/descuentos";
import { RolesPage } from "./features/permisos";
import { ClasesPage, NuevaClasePage, EditarClasePage, ClaseDetallePage } from "./features/clases";
import { EntrenamientosPage, NuevoEntrenamientoPage, EditarEntrenamientoPage, EntrenamientoDetallePage } from "./features/entrenamientos";
import { PlanillaAsistenciaPage } from "./features/asistencia";
import DashboardPage from "./pages/DashboardPage";
import CanchasPage from "./pages/CanchasPage";
import NuevaCanchaPage from "./pages/NuevaCanchaPage";
import TiposCanchaPage from "./pages/TiposCanchaPage";
import NuevoTipoCanchaPage from "./pages/NuevoTipoCanchaPage";
import EditarTipoCanchaPage from "./pages/EditarTipoCanchaPage";
import TipoCanchaDetallePage from "./pages/TipoCanchaDetallePage";
import DisponibilidadPage from "./pages/DisponibilidadPage";
import CanchaDetallePage from "./pages/CanchaDetallePage";
import EditarCanchaPage from "./pages/EditarCanchaPage";
import CobroPage from "./pages/CobroPage";

// Importaciones de los Reportes (FRONT 15)
import ReportesPage from "./pages/ReportesPage";
import ReporteIngresosPage from "./pages/ReporteIngresosPage";
import ReporteAsistenciasPage from "./pages/ReporteAsistenciasPage";
import ReporteReservasPage from "./pages/ReporteReservasPage";



function App() {
  // Al abrir o recargar la página se recupera la sesión con la cookie del refresh token
  useEffect(() => {
    restaurarSesion();
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        {/* Rutas Públicas */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />


        {/* Rutas Protegidas Generales (Cualquier usuario logueado) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/reservas" element={<ReservasPage />} />
          <Route path="/reservas/nuevo" element={<NuevaReservaPage />} />
          <Route path="/reservas/nueva" element={<Navigate to="/reservas/nuevo" replace />} />
          <Route path="/reservas/:id" element={<ReservaDetallePage />} />
          <Route path="/reservas/:idReserva/cobro" element={<CobroPage />} />
          <Route path="/inscripciones/:idInscripcion/cobro" element={<CobroPage origen="actividad" />} />
          <Route path="/torneos/inscripciones/:idInscripcion/cobro" element={<CobroPage origen="torneo" />} />
          <Route path="/torneos" element={<TorneosPage />} />
          <Route path="/torneos/abiertos" element={<TorneosPage vista="abiertos" />} />
          <Route path="/torneos/mios" element={<TorneosPage vista="mios" />} />
          <Route path="/torneos/:id" element={<TorneoDetallePage />} />
          <Route path="/torneos/:id/fixture" element={<TorneoFixturePage />} />
          {/* El perfil propio es de cualquiera que inicie sesion: un socio no puede
              editarse desde /usuarios, que exige EMPLEADO o ADMINISTRADOR. */}
          <Route path="/perfil" element={<PerfilPage />} />
          <Route path="/configuracion" element={<ConfiguracionPage />} />
        </Route>

        <Route element={<ProtectedRoute allowedRoles={["SOCIO"]} />}>
          <Route path="/mis-pagos" element={<MisPagosPage />} />
        </Route>

        {/* Rutas Protegidas de Gestión y Operaciones (Empleado y Admin) */}
        <Route element={<ProtectedRoute allowedRoles={["EMPLEADO", "ADMINISTRADOR"]} />}>
          <Route path="/reservas/editar/:id" element={<ReservaFormPage />} />
          <Route path="/usuarios" element={<UsuariosPage />} />
          <Route path="/torneos/nuevo" element={<TorneoFormPage />} />
          <Route path="/torneos/editar/:id" element={<TorneoFormPage />} />
          <Route path="/canchas" element={<CanchasPage />} />
          <Route path="/canchas/tipos" element={<TiposCanchaPage />} />
          <Route path="/canchas/tipos/nuevo" element={<NuevoTipoCanchaPage />} />
          <Route path="/canchas/tipos/editar/:id" element={<EditarTipoCanchaPage />} />
          <Route path="/canchas/tipos/:id" element={<TipoCanchaDetallePage />} />
          <Route path="/canchas/nuevo" element={<NuevaCanchaPage />} />
          <Route path="/canchas/editar/:id" element={<EditarCanchaPage />} />
          <Route path="/canchas/:id" element={<CanchaDetallePage />} />
          <Route path="/disponibilidad" element={<DisponibilidadPage />} />
          <Route path="/insumos" element={<InsumosPage />} />
          <Route path="/insumos/nuevo" element={<NuevoInsumoPage />} />
          <Route path="/insumos/editar/:id" element={<EditarInsumoPage />} />
          <Route path="/insumos/categorias" element={<CategoriasInsumoPage />} />
          <Route path="/insumos/:id" element={<InsumoDetallePage />} />
          <Route path="/cobros" element={<CobrosPage />} />
          <Route path="/cobros/nuevo" element={<NuevoCobroPage />} />
          <Route path="/cobros/:id" element={<CobroDetallePage />} />
          <Route path="/recibos" element={<RecibosPage />} />
          <Route path="/recibos/:id" element={<ReciboDetallePage />} />
          <Route path="/profesores" element={<ProfesoresPage />} />
          <Route path="/profesores/nuevo" element={<NuevoProfesorPage />} />
          <Route path="/profesores/editar/:id" element={<EditarProfesorPage />} />
          <Route path="/profesores/:id" element={<ProfesorDetallePage />} />
          <Route path="/entrenadores" element={<EntrenadoresPage />} />
          <Route path="/entrenadores/nuevo" element={<NuevoEntrenadorPage />} />
          <Route path="/entrenadores/editar/:id" element={<EditarEntrenadorPage />} />
          <Route path="/entrenadores/:id" element={<EntrenadorDetallePage />} />
        </Route>

        {/* Rutas de Clases (aisladas del Entrenador) */}
        <Route element={<ProtectedRoute allowedRoles={["SOCIO", "EMPLEADO", "PROFESOR", "ADMINISTRADOR"]} />}>
          <Route path="/clases" element={<ClasesPage />} />
          <Route path="/clases/mias" element={<ClasesPage vista="mias" />} />
          <Route path="/clases/disponibles" element={<ClasesPage vista="disponibles" />} />
          <Route path="/clases/:id" element={<ClaseDetallePage />} />
        </Route>

        {/* Rutas de Entrenamientos (aisladas del Profesor) */}
        <Route element={<ProtectedRoute allowedRoles={["SOCIO", "EMPLEADO", "ENTRENADOR", "ADMINISTRADOR"]} />}>
          <Route path="/entrenamientos" element={<EntrenamientosPage />} />
          <Route path="/entrenamientos/mios" element={<EntrenamientosPage vista="mias" />} />
          <Route path="/entrenamientos/disponibles" element={<EntrenamientosPage vista="disponibles" />} />
          <Route path="/entrenamientos/:id" element={<EntrenamientoDetallePage />} />
        </Route>

        {/* Rutas Protegidas de Staff de Clases (Profesor, Empleado y Admin) */}
        <Route element={<ProtectedRoute allowedRoles={["EMPLEADO", "PROFESOR", "ADMINISTRADOR"]} />}>
          <Route path="/clases/nueva" element={<NuevaClasePage />} />
          <Route path="/clases/editar/:id" element={<EditarClasePage />} />
          <Route path="/clases/:id/asistencia" element={<PlanillaAsistenciaPage tipo="clase" />} />
        </Route>

        {/* Rutas Protegidas de Staff de Entrenamientos (Entrenador, Empleado y Admin) */}
        <Route element={<ProtectedRoute allowedRoles={["EMPLEADO", "ENTRENADOR", "ADMINISTRADOR"]} />}>
          <Route path="/entrenamientos/nuevo" element={<NuevoEntrenamientoPage />} />
          <Route path="/entrenamientos/editar/:id" element={<EditarEntrenamientoPage />} />
          <Route path="/entrenamientos/:id/asistencia" element={<PlanillaAsistenciaPage tipo="entrenamiento" />} />
        </Route>

        {/* Rutas Protegidas de Alta Dirección (Solo Administrador) */}
        <Route element={<ProtectedRoute allowedRoles={["ADMINISTRADOR"]} />}>
          <Route path="/descuentos" element={<DescuentosPage />} />
          {/* Roles y permisos (CU75, CU85) */}
          <Route path="/roles" element={<RolesPage />} />
          
          {/* Módulo de Reportes */}
          <Route path="/reportes" element={<ReportesPage />} />
          <Route path="/reportes/ingresos" element={<ReporteIngresosPage />} />
          <Route path="/reportes/asistencias" element={<ReporteAsistenciasPage />} />
          <Route path="/reportes/reservas" element={<ReporteReservasPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;