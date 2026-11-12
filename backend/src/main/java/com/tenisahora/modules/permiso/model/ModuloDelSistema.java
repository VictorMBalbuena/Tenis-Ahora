package com.tenisahora.modules.permiso.model;

/**
 * Los módulos sobre los que se marcan permisos (CU75). Son los mismos módulos del documento de diseño (DIS05,
 * M01 a M13), más el de permisos, que también se administra.
 *
 * <p>La ruta es la que usa el control central para saber a qué módulo pertenece un pedido.
 */
public enum ModuloDelSistema {

    USUARIOS("Usuarios", "/api/socios", "/api/empleados", "/api/administradores"),
    CANCHAS("Canchas y disponibilidad", "/api/canchas"),
    RESERVAS("Reservas", "/api/reservas"),
    INSUMOS("Insumos y stock", "/api/insumos", "/api/categorias-insumo"),
    PROFESORES("Profesores", "/api/profesores"),
    ENTRENADORES("Entrenadores", "/api/entrenadores"),
    CLASES("Clases", "/api/clases"),
    ENTRENAMIENTOS("Entrenamientos", "/api/entrenamientos"),
    ACTIVIDADES("Asistencia e instructores", "/api/actividades"),
    TORNEOS("Torneos", "/api/torneos"),
    COBROS("Cobros", "/api/cobros"),
    RECIBOS("Recibos de pago", "/api/recibos"),
    DESCUENTOS("Descuentos", "/api/descuentos"),
    REPORTES("Reportes", "/api/reportes"),
    PERMISOS("Roles y permisos", "/api/roles", "/api/cuentas", "/api/permisos");

    private final String etiqueta;
    private final String[] rutas;

    ModuloDelSistema(String etiqueta, String... rutas) {
        this.etiqueta = etiqueta;
        this.rutas = rutas;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Si el pedido cae dentro de este módulo. */
    /**
     * Rutas que caen bajo el prefijo de un módulo pero no le pertenecen.
     *
     * Validar un código de descuento es parte de reservar, no de administrar descuentos: si lo cubriera
     * DESCUENTOS, un socio no podría usar su propio código porque no tiene permiso sobre ese módulo. Igual no
     * queda abierta: el controlador exige sesión y rol.
     */
    private static final String[] AJENAS = {"/api/descuentos/codigo"};

    public boolean cubre(String ruta) {
        if (empiezaPorAlguna(ruta, AJENAS)) {
            return false;
        }
        // Pagar es pagar, cuelgue de donde cuelgue.
        if (esDePago(ruta)) {
            return this == COBROS;
        }
        return empiezaPorAlguna(ruta, rutas);
    }

    /**
     * Si esa ruta es la de registrar o consultar un cobro.
     *
     * Las tres formas de pagar cuelgan de otro modulo: `/api/reservas/{id}/cobros`,
     * `/api/actividades/inscripciones/{id}/cobros` y `/api/torneos/inscripciones/{id}/cobros`. Si el modulo
     * saliera solo del prefijo, apagar "Cobros" en la matriz no impediria ningun pago y para dejar pagar una
     * clase habria que tocar "Actividades", que no es lo que nadie espera al mirar la pantalla.
     */
    private static boolean esDePago(String ruta) {
        return ruta.endsWith("/cobros") || ruta.contains("/cobros/");
    }

    private static boolean empiezaPorAlguna(String ruta, String[] prefijos) {
        for (String prefijo : prefijos) {
            if (ruta.equals(prefijo) || ruta.startsWith(prefijo + "/")) {
                return true;
            }
        }
        return false;
    }
}
