# DengueSim · frontend

Interfaz React (Vite) del simulador. Consume la API del backend Spring Boot.

```bash
pnpm install
pnpm dev      # http://localhost:5173
pnpm lint
pnpm build
```

El puerto **5173 importa**: está en la lista de orígenes permitidos por CORS en
`CorsConfig` del backend. La URL del backend se configura en `.env` (`VITE_API_URL`).

Las instrucciones completas de puesta en marcha (base de datos incluida) están en el
[README de la raíz](../README.md).

## Organización

```
src/
  api/client.js       único punto de acceso a la API
  state/
    SimulationContext.jsx   provider del estado global de la corrida
    useSimulation.js        el contexto y su hook (aparte, por el Fast Refresh)
  views/              una vista por pestaña de la barra lateral
  components/         Charts · Field · Telemetry · InterventionEditor · Common · Math
  lib/
    fields.js         metadata de cada variable: genera sola los controles del panel
    colors.js         paleta de las gráficas
    r0.js             R₀ en el cliente, para la vista previa en vivo
    format.js         formateadores es-HN
  index.css           sistema de diseño (variables CSS y clases compartidas)
```

`lib/fields.js` es la fuente única de los controles: agregar una variable al modelo
es agregar una entrada ahí y el campo aparece en Escenario y en el glosario de Modelo.

Las ecuaciones se componen con **KaTeX** desde `components/Math.jsx` (`<Eq>` en bloque,
`<Tex>` dentro de un párrafo). La vista Modelo se carga con `React.lazy` porque es la
única que necesita KaTeX y sus fuentes: así no pesan en el arranque de las otras seis.
