# 001: Estructura por módulos con capas adentro

**Fecha:** 23/09/2026

**Contexto:** son 13 módulos y 5 personas trabajando en paralelo. Con una carpeta única por capa
costaba encontrar el código de un módulo y había más conflictos en Git. Una primera versión con un
paquete `common/` para todo lo compartido terminó mezclando cosas que dependían de los módulos
(tareas programadas), lo que generaba dependencias circulares.

**Decisión:**
- Un paquete por módulo en `modules/`, cada uno con sus capas (controller, service, repository,
  entity, dto, mapper) y, si hace falta, `job/` para sus tareas programadas.
- `shared/` e `infrastructure/` no conocen a ningún módulo.
- `config/`, `security/` y `middleware/` separados por responsabilidad.

**Consecuencias:**
- Las dependencias van en un solo sentido: los módulos usan shared e infrastructure, nunca al revés.
- Un módulo usa el service de otro, nunca su repository.
- Cada integrante trabaja en la carpeta de su módulo.
