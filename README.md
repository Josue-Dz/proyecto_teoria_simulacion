# DengueSim

Simulador de saturación hospitalaria por brote de dengue.

**IS-910 Teoría de la Simulación** · Universidad Nacional Autónoma de Honduras

---

## Qué hace

Responde una pregunta concreta: si comienza un brote de dengue en una ciudad, ¿en qué momento
el sistema hospitalario deja de dar abasto y qué medidas pueden evitarlo?

Para responderla encadena dos modelos. Un **SEIR-SEI de siete compartimentos**, integrado con
Runge-Kutta de cuarto orden, calcula cuánta gente enferma cada día. Un **modelo de colas** toma
esa incidencia y la enfrenta a un número finito de camas, donde esperar demasiado puede costar
la vida.

Incluye dos motores intercambiables, uno determinista y uno estocástico por eventos discretos,
análisis de incertidumbre por Monte Carlo, comparación de escenarios y persistencia de
experimentos reproducibles por semilla.

No es una herramienta de predicción. Sirve para comparar escenarios entre sí.

---

## Arranque rápido

### Requisitos

| Componente | Versión |
|---|---|
| JDK | 17 |
| Node.js con pnpm | reciente |
| PostgreSQL | 18 |

### Con base de datos

Crear la base una sola vez. Las tablas las genera el sistema al arrancar.

```bash
createdb -U postgres -h localhost denguesim
```

Backend, desde `backend/`. Si las credenciales no son las predeterminadas, se definen con las
variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD` sin tocar el archivo de configuración.

```bash
Ejecutar DengueSimApplication.java
```

Frontend, desde `frontend/`:

```bash
pnpm install
pnpm dev
```

La aplicación queda en `http://localhost:5173` y la API en `http://localhost:8080`.

---

## Estructura

```
backend/    API REST en Spring Boot: motores de simulación y persistencia
frontend/   Interfaz en React con Vite y Recharts
docs/       Documentación
```

---

## Documentación

### [Manual de usuario](docs/manual-usuario.md)

Descripción funcional completa del sistema.

| Sección | Contenido |
|---|---|
| [1. Introducción](docs/manual-usuario.md#1-introducción) | Qué es y qué no es |
| [2. Requisitos e instalación](docs/manual-usuario.md#2-requisitos-e-instalación) | Puesta en marcha |
| [3. Primeros pasos](docs/manual-usuario.md#3-primeros-pasos) | Recorrido inicial |
| [4. La interfaz](docs/manual-usuario.md#4-la-interfaz) | Zonas de la pantalla |
| [5. Escenario](docs/manual-usuario.md#5-vista-01--escenario) | Panel de control e intervenciones |
| [6. Simulación](docs/manual-usuario.md#6-vista-02--simulación) | Resultados y gráficas |
| [7. Hospital](docs/manual-usuario.md#7-vista-03--hospital) | Detalle operativo y teoría de colas |
| [8. Incertidumbre](docs/manual-usuario.md#8-vista-04--incertidumbre) | Monte Carlo y bandas de percentiles |
| [9. Comparar](docs/manual-usuario.md#9-vista-05--comparar) | Enfrentar escenarios |
| [10. Biblioteca](docs/manual-usuario.md#10-vista-06--biblioteca) | Persistencia e historial |
| [11. Modelo](docs/manual-usuario.md#11-vista-07--modelo) | Documentación técnica integrada |
| [12. Referencia de parámetros](docs/manual-usuario.md#12-referencia-de-parámetros) | Las 20 variables con rango y valor por defecto |
| [13. Referencia de indicadores](docs/manual-usuario.md#13-referencia-de-indicadores) | Definición de cada indicador |
| [14. Exportar datos](docs/manual-usuario.md#14-exportar-datos) | Salida en CSV |
| [15. Glosario](docs/manual-usuario.md#15-glosario) | Términos del modelo |
| [16. Solución de problemas](docs/manual-usuario.md#16-solución-de-problemas) | Fallos frecuentes |
| [17. Referencia de la API](docs/manual-usuario.md#17-referencia-de-la-api) | Endpoints REST |

---

## Tecnologías

**Backend**: Java 17, Spring Boot, Spring Data JPA, PostgreSQL, Maven.

**Frontend**: React, Vite, Recharts, KaTeX.
