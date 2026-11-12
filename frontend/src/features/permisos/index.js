/**
 * Puerta de entrada de la feature de roles y permisos.
 *
 *   import { RolesPage, CambiarRolDialog, EstadoCuentaDialog } from "@/features/permisos";
 *
 * Los dos diálogos los usa la pantalla de usuarios: trabajan sobre una cuenta,
 * no sobre un rol, pero las reglas son de este módulo (CU74 y CU76).
 */
export { RolesPage } from "./pages/RolesPage";
export { CambiarRolDialog } from "./components/CambiarRolDialog";
export { EstadoCuentaDialog } from "./components/EstadoCuentaDialog";
export { MatrizPermisos } from "./components/MatrizPermisos";
