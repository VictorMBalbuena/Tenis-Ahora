# 003: La liga es un tipo de competencia, no un módulo aparte

**Fecha:** 05/11/2026

**Contexto:** la E05 habla de "torneos y ligas", pero el sistema solo sabía de torneos. Antes de agregar
nada se revisó qué pide realmente el documento, y la respuesta cambia el alcance:

- La palabra **liga nunca aparece sola**. Siempre es "torneo / liga" o "torneos y ligas".
- **No existe ningún caso de uso de liga.** Los de competencia son CU45 a CU50, y los seis dicen
  "torneo".
- Solo tiene peso propio en tres lugares: **RF52 / CU52** ("Registrar cobro por inscripción a torneo /
  liga"), **RD14** ("los torneos y ligas se rigen por las reglas oficiales de la Asociación de Tenis")
  y **RD21** (descuentos a "equipos que participan en ligas regulares").
- **No hay ninguna mención de ranking ni de tabla de posiciones** en todo el documento. Se buscó
  *ranking*, *tabla de posiciones*, *posiciones*, *todos contra todos*, *round robin* y *puntaje*: el
  único resultado es "puntaje inválido", que es la validación de un set en CU49.

O sea: no hay un módulo de ligas para construir, pero sí un hueco real. El sistema no tenía forma de
representar una liga, y la pantalla ya decía "Torneos y Ligas" en el breadcrumb sin que se pudiera
crear una.

**Decisión:** la liga es un **tipo de competencia** dentro del módulo de torneos, no un módulo nuevo.

1. **`Torneo.tipo`: TORNEO o LIGA.** Comparten entidad, inscripción, arancel, cobro y resultados,
   porque la E05 las trata igual en todo eso. Lo único que cambia es cómo se juegan. El campo arranca
   en TORNEO, así que las competencias ya cargadas siguen comportándose igual.

2. **El tipo decide cómo se arma el fixture.** Un torneo es eliminación directa y necesita una
   potencia de 2. Una liga es todos contra todos (`CalendarioDeLiga`, método del círculo) y sirve con
   cualquier cantidad desde 2; con cantidad impar, uno descansa cada fecha.

3. **Al cargar un resultado, en una liga no avanza nadie.** El torneo pasa el ganador a la ronda
   siguiente; la liga solo suma a la tabla y se da por finalizada cuando se jugaron todas las fechas.

4. **La tabla de posiciones no inventa un sistema de puntos.** En tenis no hay empate, así que los
   partidos ganados *son* la puntuación. Desempatan la diferencia de sets y después la de games, que
   es como se lee una tabla de liga de tenis y sale entero de los resultados cargados. Una columna de
   "puntos" con una escala elegida a dedo habría sido un número que no se puede justificar con el
   documento.

5. **El tipo no se puede cambiar con gente ya inscripta.** Quien se anotó a una liga no se anotó para
   quedar eliminado en el primer partido.

**Consecuencias:**

- RF52, RD14 y RD21 pasan a tener algo concreto a lo que referirse, y el breadcrumb "Torneos y Ligas"
  deja de prometer lo que no existía.
- **El fixture de liga y la tabla de posiciones son alcance que la E05 no pide explícitamente.** Se
  asumen a conciencia: sin ellos, una "liga" sería un torneo con otra etiqueta, y eso no se sostiene
  frente a RD14. Si en la defensa se pregunta de dónde salen, la respuesta es ésta, no un RF.
- No se construyó ningún ranking global del club. No hay nada en el documento que lo pida, y la tabla
  de una liga cubre lo que esa idea intentaba resolver.
- Un torneo sigue funcionando exactamente igual que antes: misma validación de potencia de 2, mismo
  cuadro, misma pantalla. Verificado contra el servidor.
