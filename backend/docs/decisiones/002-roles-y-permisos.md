# 002: Roles y permisos sobre el modelo de cinco tablas

**Fecha:** 04/11/2026

**Contexto:** faltaban cuatro casos de uso de la entrega — CU74 (asignar rol), CU75 (configurar
permisos por rol), CU76 (habilitar y deshabilitar cuentas) y CU85 (crear rol) — y el módulo
`permiso/` estaba vacío.

El problema es que el modelo de datos no los contemplaba. `Persona` es un `@MappedSuperclass`: cada
rol tiene su propia tabla, y `Rol` es un `enum` fijo cuyo comentario decía textualmente *"no hay
tabla Rol ni permisos configurables"*. Dicho de otra forma: el rol de una cuenta **es** la tabla en
la que está guardada. Eso choca de frente con "cambiarle el rol a alguien" y con "crear un rol
nuevo".

Las alternativas eran rehacer el modelo (una sola tabla `persona` con un rol como columna) o
construir los cuatro casos de uso encima del modelo que ya existe. Rehacerlo tocaba las cinco
tablas, los cinco repositorios, el alta, la baja, la autenticación y los 1044 tests, a cinco días de
la entrega.

**Decisión:** construir encima, sin tocar el modelo de personas.

1. **Un rol creado se apoya en un rol base.** La tabla `rol_del_sistema` guarda los cinco roles base
   (sembrados al arrancar, no se borran) y los que crea el administrador. Un rol creado no inventa
   una tabla: define un conjunto de permisos más chico sobre uno de los cinco. "Recepción" se apoya
   en `EMPLEADO`, y el rol base es además su techo — no puede habilitar nada que el empleado no
   tenga.

2. **La matriz se guarda como códigos, no como una fila por casilla.** `rol_permiso` guarda
   `"COBROS:VER"`, `"ESPECIAL:ANULAR_COBROS"`. La pantalla siempre manda la matriz entera, así que
   no hay que calcular diferencias, y dos administradores editando a la vez no terminan con una
   mezcla de las dos configuraciones. Un código que quedó de una versión anterior se ignora en vez
   de romper la pantalla.

3. **Cambiar de rol base muda la fila de tabla, y solo si no hay historial.** Se leen los datos de
   `Persona`, se borra la fila vieja y se crea en la tabla destino con la misma contraseña. Si la
   persona tiene reservas, inscripciones o asistencias, el borrado falla por integridad referencial
   y el cambio se rechaza con `PERSONA_CON_HISTORIAL`. **Es a propósito**: mover esa fila dejaría el
   historial apuntando a una tabla que ya no la tiene. El camino en ese caso es deshabilitarla
   (CU76) y crear la cuenta nueva.

   Como efecto, el id de la persona cambia. No rompe nada justamente porque el paso anterior
   garantiza que nadie la estaba referenciando, y el historial de la cuenta se traslada al id nuevo
   para que siga siendo uno solo.

4. **El control de permisos se suma a las reglas por ruta, no las reemplaza.** `SecurityConfig` y
   los `@PreAuthorize` quedaron intactos. `ControlDePermisosFilter` corre después y solo puede
   **negar** lo que la matriz no permite: nunca habilita algo que la ruta ya cortaba.

   La matriz con la que arrancan los cinco roles base (`PermisosPorDefecto`) reproduce exactamente
   lo que cada rol ya podía hacer. Por eso, el día que se activó, ningún rol ganó ni perdió acceso;
   recién cambia algo cuando un administrador destilda una casilla. Está verificado contra el
   servidor corriendo: la tabla de respuestas por rol y endpoint es idéntica antes y después.

5. **Deshabilitar avisa, no llama.** `CuentaDeshabilitadaEvent` lo escuchan auth (cierra las
   sesiones) y reservas (cancela los turnos futuros si se pidió), igual que ya hacía
   `PersonaEliminadaEvent`. El módulo de permisos no conoce a ninguno de los dos.

**Consecuencias:**

- Los cuatro casos de uso quedan cubiertos sin tocar el modelo de personas ni la seguridad que ya
  andaba.
- **Limitación conocida:** no se le puede cambiar el rol a alguien que ya operó en el sistema. Es
  una consecuencia directa del modelo de cinco tablas, y la pantalla lo dice antes de intentarlo.
- **Limitación conocida:** un rol creado nunca puede dar más permisos que su rol base. Para un rol
  con permisos que ningún rol base tiene, habría que rehacer el modelo.
- El filtro de permisos y el filtro JWT dependen del módulo `permiso`, pero de forma opcional
  (`ObjectProvider`): sin ese módulo la seguridad sigue funcionando con las reglas por ruta. Eso es
  lo que deja que las pruebas de un controlador suelto no tengan que levantarlo.
