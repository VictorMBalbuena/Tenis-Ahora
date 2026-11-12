/**
 * Un paquete por módulo, cada uno con sus capas: controller (presentación), service (negocio),
 * repository (datos), entity (modelo y enums del módulo), dto y mapper.
 * Reglas: el controller llama al service y el service al repository; un módulo usa el service de
 * otro, nunca su repository; los módulos pueden usar shared e infrastructure.
 */
package com.tenisahora.modules;
