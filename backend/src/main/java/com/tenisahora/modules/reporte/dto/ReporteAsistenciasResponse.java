package com.tenisahora.modules.reporte.dto;

import java.util.List;

public record ReporteAsistenciasResponse(
        long asistenciasRegistradas,
        String variacionAsistencias,
        String tasaAsistencia,
        String variacionTasa,
        long ausencias,
        long alumnosActivos,
        List<Porcentaje> porActividad,
        List<Porcentaje> porProfesor,
        List<DetalleAlumno> detalleAlumnos) {

    public record Porcentaje(String nombre, int porcentaje) {
    }

    public record DetalleAlumno(String iniciales, String alumno, String actividad, long clases, long presentes,
                                long ausentes, String tasa) {
    }
}
