TurnoFácil - Sistema de Gestión de Turnos para Profesionales Independientes

Trabajo Final Integrador (TFI) — Tecnicatura Universitaria en Programación (UTN)
Asignaturas Integradas: Gestión de Desarrollo de Software / Metodología de Sistemas / Programación y Base de Datos
📋 Tabla de Contenidos

Integrantes y Enfoque de Trabajo
Descripción del Proyecto y Visión de Negocio
Análisis de Situación y Justificación (Problemática)
Definición de Objetivos (Metodología S.M.A.R.T.)
Actores del Sistema (Casos de Uso Macro)
Gestión del Alcance, Módulos del MVP y Fronteras
Historias de Usuario y Criterios de Aceptación (BDD)
Análisis de Riesgos, Supuestos y Restricciones
Modelo Conceptual y Lógico de Base de Datos (PostgreSQL)
Arquitectura del Sistema y Stack Tecnológico
Roadmap de Escalabilidad (Evolución Futura del Producto)
1. Integrantes y Enfoque de Trabajo

El equipo de desarrollo está conformado por Gabriel Carbajal, Cristian Paolucci y Lautaro Pez. Tutora: Sofia Raia
Para la ejecución de este proyecto, se adopta un enfoque de ingeniería basado en buenas prácticas de la industria y metodologías ágiles. Las responsabilidades técnicas y funcionales se gestionan de manera transversal, promoviendo la colaboración integral en todas las capas del software (Frontend, Backend y Base de Datos) mediante control de versiones en Git, estrategias de ramificación y revisiones cruzadas de código (Pull Requests).
2. Descripción del Proyecto y Visión de Negocio

TurnoFácil es una solución de software web centralizada, diseñada para optimizar, agilizar y automatizar la gestión de agendas de profesionales independientes (médicos, psicólogos, nutricionistas, kinesiólogos, profesores particulares, entre otros). El sistema trasciende la simple digitalización de una agenda, proveyendo un entorno robusto, seguro y escalable que equilibra la operatividad del prestador con una experiencia de usuario fluida para el cliente final.
3. Análisis de Situación y Justificación (Problemática)

Estado Actual: Los profesionales independientes gestionan masivamente sus turnos mediante herramientas informales como la mensajería instantánea (WhatsApp) o agendas físicas de papel.
Puntos de Dolor Identificados:
Superposición de Horarios: Fallas humanas en el control manual de disponibilidad que generan conflictos de citas.
Falta de Trazabilidad: Pérdida de solicitudes y mensajes clave en bandejas de entrada saturadas.
Fricción Operativa: Procesos lentos y manuales para reprogramar o cancelar turnos, con alto costo de tiempo administrativo.
Inexistencia de Historial: Ausencia de un registro centralizado y seguro del historial de interacciones con los clientes.
Estado Deseado (Solución): Implementar una plataforma digital automatizada que centralice la gestión de turnos 24/7, elimine el error humano por superposición y libere tiempo operativo al profesional.
4. Definición de Objetivos (Metodología S.M.A.R.T.)

Objetivo General

Desarrollar, desplegar y documentar un Producto Mínimo Viable (MVP) funcional de software web durante el ciclo del TFI, logrando una arquitectura desacoplada y robusta que resuelva la problemática de gestión de turnos de profesionales independientes.
Objetivos Específicos

Seguridad y Autenticación: Implementar un sistema de autenticación basado en roles y tokens seguros (Spring Security) en el backend durante el primer trimestre de desarrollo.
Consistencia de Datos: Diseñar y normalizar una base de datos relacional (PostgreSQL) que asegure la integridad referencial y evite cruces de agendas mediante validaciones de transaccionalidad.
Experiencia de Usuario (UI/UX): Construir una interfaz web interactiva y responsiva (React, Vite, Tailwind CSS) optimizada tanto para dispositivos móviles como de escritorio.
5. Actores del Sistema (Casos de Uso Macro)

Cliente / Paciente: Usuario final que accede a la plataforma para registrarse, buscar profesionales por especialidad, visualizar disponibilidad en tiempo real, solicitar, reprogramar o cancelar sus turnos.
Profesional / Administrador: Prestador del servicio que gestiona su propio perfil, configura sus días y franjas horarias de atención, visualiza su agenda general en un panel de control (Dashboard) y administra el estado de los turnos.
6. Gestión del Alcance, Módulos del MVP y Fronteras

Desglose de Módulos del MVP (Alcance Funcional Inicial)

Módulo de Usuarios:
Registro de nuevos usuarios.
Inicio de sesión seguro.
Recuperación de contraseña (opcional).
Módulo de Profesionales:
Alta, baja y modificación de perfiles profesionales.
Configuración personalizada de horarios de atención y días disponibles.
Módulo de Turnos:
Solicitud de turnos.
Consulta de disponibilidad horaria en tiempo real.
Reprogramación de citas.
Cancelación de turnos.
Módulo Administrativo:
Vista general de turnos (Dashboard).
Control y gestión general de usuarios.
Estadísticas básicas de uso de la agenda.
Exclusiones del MVP (Delimitación de Fronteras para mitigar Scope Creep)

Integración con pasarelas de pago electrónicas (Mercado Pago, Stripe).
Facturación electrónica automatizada y sincronización con organismos fiscales (AFIP).
Envío masivo automatizado de notificaciones vía pasarelas de mensajería externa (SMS o API de WhatsApp de pago). Nota: Estas características quedan catalogadas formalmente para fases posteriores de escalabilidad.
Requerimientos No Funcionales

Seguridad: Cifrado de contraseñas mediante algoritmos de hashing seguros (BCrypt) y protección de rutas en la API REST.
Rendimiento: Tiempos de respuesta inferiores a los 2 segundos en consultas estándar de disponibilidad de agenda.
Mantenibilidad: Aplicación de principios de diseño modular, separación de responsabilidades y Clean Code en el código fuente de Java/Spring Boot.
7. Historias de Usuario y Criterios de Aceptación (BDD)

HU01: Registro de Nuevos Usuarios
Como cliente, quiero registrarme en la plataforma con mi correo electrónico y contraseña, para acceder a las funcionalidades del sistema.
Criterio de Aceptación (Given-When-Then):
Given que el usuario se encuentra en el formulario de registro,
When ingresa credenciales válidas y no existentes en el sistema,
Then la base de datos almacena la información de forma segura (contraseña encriptada) y habilita el inicio de sesión.
HU02: Solicitud de Turno
Como cliente autenticado, quiero seleccionar un profesional y un espacio horario disponible, para reservar una cita de manera autónoma.
Criterio de Aceptación (Given-When-Then):
Given que el usuario visualiza la agenda libre de un profesional en una fecha específica,
When selecciona un horario disponible y confirma la operación,
Then el sistema genera un registro de turno en estado "Pendiente/Confirmado" y bloquea dicho slot horario de manera inmediata para evitar solapamientos.
HU03: Configuración de Disponibilidad Horaria (Profesional)
Como profesional autenticado, quiero configurar mis días y franjas horarias de atención, para que los clientes puedan solicitar citas únicamente dentro de mi disponibilidad real.
Criterio de Aceptación (Given-When-Then):
Given que el profesional accede a su panel de gestión de agenda,
When define los días de la semana y los rangos horarios de atención,
Then el sistema actualiza la base de datos y habilita dichos bloques horarios en la vista pública para los clientes.
HU04: Visualización del Panel de Control (Dashboard Administrador)
Como administrador o profesional, quiero visualizar un panel de control con el estado general de los turnos, para realizar un seguimiento operativo de la jornada.
Criterio de Aceptación (Given-When-Then):
Given que el profesional inicia sesión con rol administrativo,
When accede a la sección de Dashboard,
Then el sistema muestra de forma estructurada los turnos pendientes, confirmados y cancelados del día actual.
8. Análisis de Riesgos, Supuestos y Restricciones

Supuestos

El equipo mantendrá una dedicación horaria constante y canales de comunicación fluidos durante todo el desarrollo.
Las plataformas de infraestructura cloud elegidas (Render, Vercel, Supabase) mantendrán estabilidad operativa para las pruebas y entrega final.
Riesgos y Mitigación

Riesgo 1: Desfasaje en la integración de contratos de API entre el backend de Spring Boot y el frontend de React.Mitigación: Definición estricta temprana de los endpoints y uso de flujos de trabajo basados en Pull Requests en GitHub.
Riesgo 2: Limitaciones de tiempo en la curva de aprendizaje de despliegue en la nube.Mitigación: Realizar despliegues incrementales y pruebas de conectividad en entornos cloud desde las primeras fases del proyecto.
Restricciones

Temporal: Plazos académicos estipulados por el cronograma de la cátedra universitaria.
Tecnológica: Uso obligatorio de control de versiones y patrones arquitectónicos en capas para el desarrollo en Java.
9. Modelo Conceptual y Lógico de Base de Datos (PostgreSQL)

El sistema se apoya en un esquema relacional normalizado para garantizar la integridad transaccional:
Tabla usuarios: id (PK), nombre, apellido, email, password, rol
Tabla profesionales: id (PK), usuario_id (FK a usuarios), especialidad, telefono
Tabla horarios: id (PK), profesional_id (FK a profesionales), dia_semana, hora_inicio, hora_fin
Tabla turnos: id (PK), usuario_id (FK a usuarios), profesional_id (FK a profesionales), fecha, hora, estado (Pendiente, Confirmado, Cancelado, Finalizado)
Diagrama Entidad-Relación (MER)

Modelo conceptual / lógico:
[ USUARIOS ] 1 ---- * [ PROFESIONALES ]
| |
| 1 | 1
| |
* *
[ TURNOS ] * ------------ 1 [ HORARIOS ]

10. Arquitectura del Sistema y Stack Tecnológico

El proyecto adopta una arquitectura desacoplada de tres capas lógicas (Presentación, Lógica de Negocio/API REST y Persistencia de Datos):
Capa de Presentación (Frontend):
React, Vite, Tailwind CSS, JavaScript / TypeScript.
Hosting: Vercel.
Capa de Lógica de Negocio y API (Backend):
Java, Spring Boot, Spring Data JPA (Hibernate), Spring Security.
Hosting: Render.
Capa de Persistencia (Base de Datos):
PostgreSQL (Relacional).
Hosting: Supabase.
Control de Versiones y Gestión:
Git y GitHub (Flujo basado en ramas, code review y control de entregas).
Contratos de API REST (Endpoints Preliminares del MVP)

Autenticación y Usuarios:
POST /api/auth/register — Registro de nuevos usuarios en el sistema.
POST /api/auth/login — Autenticación y generación de token seguro (JWT).
Gestión de Profesionales y Agendas:
GET /api/professionals — Listado general de profesionales disponibles.
GET /api/professionals/{id}/availability — Consulta de horarios libres de un profesional en una fecha dada.
POST /api/professionals/availability — Configuración de franjas horarias por el profesional.
Gestión de Turnos:
POST /api/appointments — Solicitud y bloqueo de un nuevo turno.
GET /api/appointments/user/{id} — Historial de turnos de un cliente.
PUT /api/appointments/{id}/cancel — Cancelación lógica de un turno.
11. Roadmap de Escalabilidad (Evolución Futura del Producto)

Como parte del ciclo de vida del desarrollo de software, la evolución del sistema se proyecta en etapas sucesivas:
Fase 1 (MVP - Versión Inicial): Desarrollo, validación y despliegue del Producto Mínimo Viable funcional (Autenticación con Spring Security, gestión básica de perfiles profesionales, configuración de agendas, selección de turnos en tiempo real y panel de control con base de datos PostgreSQL).
Fase 2 (Notificaciones Automatizadas): Incorporación de alertas automatizadas por correo electrónico o APIs oficiales de mensajería (ej. recordatorios de turnos 24 horas antes).
Fase 3 (Monetización): Integración de pasarelas de pago seguras para seña o abonado previo de consultas.
Fase 4 (Analítica Avanzada): Reportes estadísticos complejos y métricas de rendimiento para los profesionales. 
