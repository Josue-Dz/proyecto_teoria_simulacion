# Manual de usuario

**DengueSim**, simulador de saturación hospitalaria por brote de dengue.

IS-910 Teoría de la Simulación · Universidad Nacional Autónoma de Honduras

---

## Contenido

1. [Introducción](#1-introducción)
2. [Requisitos e instalación](#2-requisitos-e-instalación)
3. [Primeros pasos](#3-primeros-pasos)
4. [La interfaz](#4-la-interfaz)
5. [Vista 01 · Escenario](#5-vista-01--escenario)
6. [Vista 02 · Simulación](#6-vista-02--simulación)
7. [Vista 03 · Hospital](#7-vista-03--hospital)
8. [Vista 04 · Incertidumbre](#8-vista-04--incertidumbre)
9. [Vista 05 · Comparar](#9-vista-05--comparar)
10. [Vista 06 · Biblioteca](#10-vista-06--biblioteca)
11. [Vista 07 · Modelo](#11-vista-07--modelo)
12. [Referencia de parámetros](#12-referencia-de-parámetros)
13. [Referencia de indicadores](#13-referencia-de-indicadores)
14. [Exportar datos](#14-exportar-datos)
15. [Glosario](#15-glosario)
16. [Solución de problemas](#16-solución-de-problemas)
17. [Referencia de la API](#17-referencia-de-la-api)

---

## 1. Introducción

### Qué es

DengueSim es una herramienta de simulación que responde una pregunta concreta: **si comienza
un brote de dengue en una ciudad, ¿en qué momento el sistema hospitalario deja de dar abasto
y qué medidas pueden evitarlo?**

### Cómo funciona

El simulador encadena dos modelos que se alimentan uno al otro.

El **modelo epidemiológico** es un sistema SEIR-SEI de siete compartimentos. Reparte a la
población humana en susceptibles, expuestos, infecciosos y recuperados, y a la población de
mosquitos en susceptibles, expuestos e infecciosos. Se integra con Runge-Kutta de cuarto
orden y produce cuánta gente enferma cada día.

El **modelo de colas** toma esa incidencia diaria y la enfrenta a un número finito de camas.
Las camas son los servidores y los pacientes forman la fila. La diferencia con una cola
corriente es que aquí esperar tiene consecuencias: un paciente vulnerable que espera
demasiado se agrava y puede morir sin haber recibido atención.

El sistema ofrece dos motores intercambiables:

| Motor | Cómo calcula | Cuándo usarlo |
|---|---|---|
| Determinista | Flujos promedio, resultado exacto y repetible | Explorar parámetros y comparar escenarios |
| Estocástico | Eventos discretos paciente por paciente, con llegadas aleatorias | Estimar probabilidades y medir incertidumbre |

### Qué no es

**No es una herramienta de predicción.** Los parámetros están calibrados con literatura
publicada, pero el propósito es comparar escenarios entre sí, no pronosticar cifras
concretas. Un resultado del tipo «con 300 camas adicionales la saturación se evita» es
válido; uno del tipo «morirán 56 personas el próximo año» no lo es.

---

## 2. Requisitos e instalación

### Software necesario

| Componente | Versión | Para qué |
|---|---|---|
| JDK | 17 | Ejecutar el backend |
| Node.js con pnpm | reciente | Ejecutar el frontend |
| PostgreSQL | 18 | Guardar escenarios e historial |

### Base de datos

Antes del primer arranque hay que crear la base:

```
createdb -U postgres -h localhost denguesim
```

Las tablas las crea el propio sistema al arrancar, ejecutando
`db/schema_postgresql.sql`. No hay que crearlas a mano.

Si las credenciales de PostgreSQL no son las predeterminadas, se definen mediante variables
de entorno y **no editando el archivo de configuración**:

```
DB_URL, DB_USER, DB_PASSWORD
```

### Arranque

Backend, desde la carpeta `backend`:

```
Ejecutar DengueSimApplication.java
```

Queda escuchando en `http://localhost:8080`.

Frontend, desde la carpeta `frontend`:

```
pnpm install
pnpm dev
```

Queda disponible en `http://localhost:5173`.

---

## 3. Primeros pasos

La primera vez conviene seguir este recorrido, que corresponde al orden numerado del menú:

1. Abrir **Escenario**. Los valores por defecto ya representan un brote realista, así que no
   es necesario cambiar nada todavía.
2. Pulsar **Ejecutar simulación**. El sistema calcula y salta solo a la vista de resultados.
3. En **Simulación**, leer los ocho indicadores de la parte superior y observar dónde la
   curva de camas ocupadas toca la línea de capacidad.
4. Mover el control deslizante de la barra inferior, o pulsar **ir a saturación**, para
   recorrer la epidemia día por día.
5. Entrar a **Hospital** y volver a mover el control: la rejilla de camas se llena a la vista.
6. Volver a **Escenario**, agregar una intervención y ejecutar de nuevo para comparar.

---

## 4. La interfaz

La pantalla tiene cuatro zonas fijas.

### Barra superior

Siempre visible, muestra tres cosas:

- **R₀** recalculado en vivo con los parámetros actuales, sin necesidad de ejecutar nada
- **Población** de la zona simulada
- **Insignia de saturación**, que avisa si el escenario actual desborda el hospital y en qué día

A la derecha está el botón de **sol o luna**, que alterna entre modo claro y oscuro. La
preferencia se recuerda entre sesiones y, si nunca se ha elegido, el sistema sigue la
configuración del sistema operativo.

### Menú lateral

Las siete vistas, numeradas del 01 al 07 en el orden recomendado de uso.

### Área de trabajo

El contenido de la vista activa.

### Barra inferior (scrubber)

Aparece únicamente cuando ya existe una corrida. Es una línea de tiempo que recorre todo el
horizonte de la simulación.

| Control | Función |
|---|---|
| ▶ | Anima la epidemia día por día |
| ⏮ | Vuelve al día 1 |
| Deslizador | Selecciona un día concreto |
| ir a saturación | Salta directo al primer día con pacientes sin cama |

**Todas las vistas responden al día seleccionado.** Es el mecanismo que conecta la vista
general con el detalle operativo.

---

## 5. Vista 01 · Escenario

El panel de control. Aquí se define la configuración completa y desde aquí se ejecuta.

### Tarjetas

| Tarjeta | Contenido |
|---|---|
| Modelo epidemiológico | 12 variables del brote |
| Capacidad hospitalaria | 8 variables del hospital |
| Corrida | Horizonte, modo determinista o estocástico, y semilla |
| Intervenciones | Medidas aplicadas en fechas concretas |

### Los controles

Cada variable tiene un deslizador, una caja numérica editable y una línea de ayuda. Ambos
controles están sincronizados y respetan los límites del parámetro: si se escribe un valor
fuera de rango, al salir del campo se ajusta automáticamente al límite más cercano.

**No es necesario modificar las veinte variables.** Los valores por defecto están calibrados
con literatura publicada para una ciudad del tamaño del Distrito Central.

### Modo de corrida

**Determinista** calcula los flujos promedio. La misma configuración produce siempre el mismo
resultado.

**Estocástico** simula paciente por paciente con llegadas aleatorias. Cada ejecución da un
resultado distinto salvo que se fije la semilla. La casilla de semilla solo se habilita en
este modo; si se deja vacía, cada corrida usa una semilla aleatoria.

### Intervenciones

Se agregan con **+ Agregar intervención**. Cada una tiene tres campos: tipo, día de inicio y
magnitud.

| Tipo | Magnitud | Rango | Efecto |
|---|---|---|---|
| Fumigación | Fracción eliminada | 0 a 1 | Elimina ese porcentaje de mosquitos **ese día** |
| Camas extra | Camas añadidas | 0 a 2000 | Suma camas **desde ese día en adelante** |
| Reducción de transmisión | Reducción | 0 a 1 | Reduce la transmisión **desde ese día en adelante** |

El significado de la magnitud cambia según el tipo, de modo que al cambiar el tipo el valor
se reinicia al predeterminado de ese tipo.

Tres advertencias importantes sobre el comportamiento de las intervenciones:

**La fumigación es un golpe transitorio.** Elimina mosquitos adultos pero no destruye los
criaderos, así que la población se repuebla en unas dos semanas. Por eso **el momento importa
más que la intensidad**: fumigar al 60 % justo antes del pico es más eficaz que fumigar al
100 % dos meses antes.

**Las camas extra solo cuentan desde su día de inicio.** Agregarlas después de la saturación
reduce las muertes posteriores, pero no cambia el día en que el sistema se saturó.

**Varias reducciones de transmisión se componen de forma multiplicativa.** Dos medidas del
30 % no equivalen a una del 60 %, sino a una del 51 %, porque 0,7 × 0,7 = 0,49.

Una intervención con día de inicio posterior al horizonte no se aplicaría nunca, así que el
sistema la rechaza y marca el campo en rojo.

---

## 6. Vista 02 · Simulación

Los resultados de la corrida.

### Indicadores

Ocho indicadores repartidos en dos filas. La primera contiene los cuatro críticos: R₀, día de
saturación, presión pico y muertes por saturación. La segunda añade tasa de ataque,
hospitalizaciones, espera media y déficit máximo de camas. La definición de cada uno está en
la [sección 13](#13-referencia-de-indicadores).

Los indicadores se colorean cuando cruzan un umbral preocupante.

### Gráficas

**Ocupación de camas frente a capacidad.** La curva rellena son las camas ocupadas y la línea
roja discontinua es el techo del sistema. El punto donde se tocan es la saturación, marcado
además con una línea vertical.

**Pacientes sin cama.** La cola de espera. Comparte el eje de días con la gráfica anterior, de
modo que ambas se leen alineadas verticalmente: primero se llena el hospital, después empieza
a crecer la fila.

**Curva epidémica.** Tres series: casos nuevos por día, infecciosos activos y expuestos.

**Estado en el día N.** La lectura numérica del día que marca el scrubber, con ocho valores
puntuales incluido el R efectivo.

---

## 7. Vista 03 · Hospital

El detalle operativo del día seleccionado.

### Camas · día N

Una rejilla donde cada cuadro representa una cama. Es la forma más directa de percibir la
saturación: al mover el scrubber, la sala se llena a la vista.

### Diagnóstico de colas

Los indicadores de teoría de colas M/M/c calculados para ese día concreto.

| Indicador | Símbolo | Significado |
|---|---|---|
| Carga ofrecida | a | Llegadas por día multiplicado por la estancia media, en Erlangs |
| Utilización | ρ | Carga dividida entre capacidad |
| Probabilidad de esperar | C(c,a) | Fórmula de Erlang-C |
| Espera media | W_q | Tiempo medio en la cola |

**La lectura clave es ρ.** Por debajo de 1 la cola se estabiliza; por encima crece sin
límite y el sistema no se recupera solo. Cuando ρ supera 1 aparece un aviso explícito y la
espera media se muestra como SATURADO, porque en régimen inestable no existe un valor
estacionario.

### Gráficas

| Gráfica | Contenido |
|---|---|
| Cola de espera | Pacientes aguardando cama a lo largo del horizonte |
| Utilización del sistema | Curva de ρ con la línea crítica en 1 |
| Movimiento de camas | Ingresos y altas, el hospital funcionando |
| Salidas sin atención | Muertes en cola y quienes abandonan sin recibir cama |

### Las cuatro salidas de la cola

Un paciente que llega buscando cama termina siempre en uno de cuatro estados:

1. **Ingresa.** Se libera una cama y entra. Tienen prioridad los que llevan más tiempo esperando.
2. **Muere esperando.** Solo puede ocurrirle a un paciente vulnerable que ya se agravó.
3. **Abandona sin cama.** Superó la espera máxima. Si era un caso moderado, se recupera por su cuenta.
4. **Sigue en cola.** La simulación terminó mientras aún esperaba.

---

## 8. Vista 04 · Incertidumbre

Una sola corrida estocástica es una anécdota. Esta vista repite la simulación muchas veces
con semillas distintas y resume el conjunto.

### Controles

**Número de corridas**: 20, 50, 100, 200 o 500. Más corridas dan bandas más suaves pero tardan más.

**Variabilidad de la transmisión**: sin ruido, ±5 %, ±10 % o ±20 %. Representa cuánta
incertidumbre se supone en el parámetro beta. Con «sin ruido» la trayectoria epidémica es
idéntica en todas las corridas y lo único que varía es la cola hospitalaria, lo que permite
separar qué parte del riesgo viene del contagio y qué parte del hospital.

### Cómo leer las bandas

Cada gráfica tiene dos elementos:

**La línea** es la mediana, el percentil 50. En la mitad de las corridas el valor quedó por
encima y en la mitad por debajo. Es el escenario típico.

**La franja** va del percentil 5 al 95 y contiene el 90 % central de los resultados. Su
lectura correcta es «en 9 de cada 10 corridas el valor cayó aquí dentro».

**El ancho de la franja es la incertidumbre.** Estrecha significa resultado robusto; ancha
significa que dos brotes idénticos sobre el papel pueden terminar muy distinto.

Tres advertencias de interpretación:

**La franja no es la trayectoria de ninguna corrida.** Es una envolvente calculada día a día,
así que su borde superior puede aplanarse antes que la mediana sin que eso sea contradictorio.

**Una mediana en cero con un p95 alto no es un error.** Significa que en más de la mitad de
las corridas no ocurrió nada, pero en el peor 5 % ocurrió mucho. Es exactamente el tipo de
riesgo que una sola corrida oculta.

**Las muertes acumuladas nunca decrecen**, porque son un total corrido. Lo informativo es su
pendiente: sube mientras muere gente y se aplana cuando dejan de morir. El punto donde se
acuesta marca el fin de la crisis.

### Probabilidad de saturación

Es el dato más útil de la vista. No afirma si el hospital colapsa, sino en qué fracción de las
corridas colapsó. Un 72 % significa que en casi 3 de cada 4 futuros posibles el sistema se
desborda. Eso es una afirmación de riesgo, que es lo que se le lleva a quien tiene que
decidir, en lugar de un número único que aparenta certeza.

---

## 9. Vista 05 · Comparar

Enfrenta hasta tres versiones del mismo brote en una sola gráfica.

### Flujo recomendado

1. Configurar un escenario en **Escenario**
2. Volver aquí y pulsar **+ Capturar configuración actual**
3. Pulsar **Duplicar** sobre esa captura
4. Cambiar **una sola variable** en la copia
5. Pulsar **Comparar**

Cambiar una variable a la vez es la única forma limpia de aislar el efecto de una medida. Si
se cambian dos, el resultado no dice cuál de las dos lo produjo.

Cada escenario puede renombrarse, y desde la propia tarjeta se pueden ajustar las camas
iniciales y las intervenciones sin volver al panel principal.

---

## 10. Vista 06 · Biblioteca

Persistencia de escenarios e historial de corridas.

### Guardar la configuración actual

Se le asigna un nombre, obligatorio, y una descripción opcional donde conviene anotar qué
hipótesis se está probando. El escenario se guarda en la base de datos con todos sus
parámetros e intervenciones.

### Escenarios guardados

Cada entrada ofrece tres acciones:

| Acción | Efecto |
|---|---|
| cargar | Trae sus parámetros al panel Escenario |
| ejecutar | Lo corre directamente y registra la corrida |
| borrar | Lo elimina de forma permanente |

### Historial de corridas

Todas las simulaciones ejecutadas, con sus indicadores y su semilla.

**Un escenario guardado más su semilla reproduce la corrida exacta.** Esa es la propiedad que
hace repetible el experimento, y es lo que permite defender un resultado ante terceros.

---

## 11. Vista 07 · Modelo

La documentación técnica incorporada a la propia aplicación. Contiene:

- El **diagrama de compartimentos**, con los mismos colores que las curvas de Simulación
- La fórmula de **R₀**, recalculada en vivo con los parámetros actuales
- Una guía de **cómo leer las ecuaciones** para quien no trabaja con ecuaciones diferenciales
- Las **ecuaciones diferenciales** de la dinámica humana y la del vector, cada una con su
  lectura en lenguaje llano
- El **modelo de colas** hospitalario y sus cuatro salidas
- Un **glosario** de todas las variables con símbolo, significado y valor actual

---

## 12. Referencia de parámetros

### Modelo epidemiológico

| Parámetro | Símbolo | Unidad | Rango | Por defecto |
|---|---|---|---|---|
| Población humana | N_h | personas | 10.000 a 3.000.000 | 1.342.329 |
| Mosquitos por humano | m | × | 0,5 a 4 | 1,5 |
| Tasa de picadura | b | 1/día | 0,1 a 1,5 | 0,65 |
| Transmisión vector→humano | β_h | adimensional | 0,05 a 1 | 0,40 |
| Transmisión humano→vector | β_v | adimensional | 0,05 a 1 | 0,40 |
| Incubación humana | 1/ν_h | días | 2 a 12 | 5 |
| Incubación en mosquito | 1/ν_v | días | 4 a 20 | 10 |
| Periodo infeccioso | 1/γ_h | días | 2 a 12 | 6 |
| Esperanza de vida | 1/μ_h | días | 7.300 a 36.500 | 25.550 |
| Vida del mosquito | 1/μ_v | días | 5 a 40 | 14 |
| Infectados iniciales | I_h(0) | personas | 1 a 500 | 10 |
| Inmunidad previa | σ | adimensional | 0 a 0,95 | 0,45 |

### Capacidad hospitalaria

| Parámetro | Símbolo | Unidad | Rango | Por defecto |
|---|---|---|---|---|
| Camas iniciales | c₀ | camas | 10 a 2000 | 150 |
| Fracción hospitalizada | p_h | adimensional | 0,0005 a 0,05 | 0,005 |
| Retardo de ingreso | τ | días | 0 a 10 | 4 |
| Estancia media | LOS | días | 1 a 15 | 6 |
| Umbral de deterioro | θ | días | 0,5 a 7 | 2 |
| Fracción vulnerable | f_v | adimensional | 0 a 0,6 | 0,27 |
| Mortalidad de grave | δ | 1/día | 0 a 0,1 | 0,013 |
| Espera máxima | W_max | días | 1 a 30 | 14 |

### Los cuatro parámetros más influyentes

Si el tiempo de exploración es limitado, estos son los que más mueven el resultado:

**Inmunidad previa (σ).** El más sensible de todos. Existe un umbral de inmunidad de grupo en
σ = 1 − 1/R₀²; por encima de ese valor la epidemia sencillamente no arranca.

**Camas iniciales (c₀).** Determina el punto de quiebre del hospital.

**Fracción hospitalizada (p_h).** Multiplica directamente la demanda de camas.

**Mosquitos por humano (m).** Escala la población de vectores y con ella el R₀.

### Restricciones entre parámetros

Dos condiciones se validan en conjunto y producen un mensaje de error si no se cumplen:

- La **espera máxima** debe superar al **umbral de deterioro**. En caso contrario ningún
  paciente llegaría a agravarse antes de abandonar la cola.
- El **día de inicio de una intervención** debe caer dentro del horizonte.

---

## 13. Referencia de indicadores

| Indicador | Definición |
|---|---|
| **R₀** | Número reproductivo básico. A cuántas personas contagia en promedio un enfermo en una población totalmente susceptible. Por encima de 1 el brote crece |
| **Día de saturación** | Primer día con al menos un paciente sin cama |
| **Presión pico** | Máximo de (camas ocupadas + cola) dividido entre la capacidad. Un valor de 3,3× indica que el hospital llegó a recibir más de tres veces su capacidad |
| **Muertes por saturación** | Muertes atribuibles a no haber conseguido cama. **No incluye** la mortalidad del dengue en pacientes atendidos |
| **Tasa de ataque final** | Porcentaje de la población que se infectó durante todo el brote |
| **Hospitalizaciones** | Total acumulado de casos que requirieron cama |
| **Espera media por cama** | Días que aguardó en promedio quien finalmente ingresó. No cuenta a quienes nunca ingresaron |
| **Déficit máximo de camas** | Mayor número de personas esperando de forma simultánea |
| **R efectivo, R_e(t)** | Número reproductivo del día t, que incorpora el agotamiento de susceptibles. Cuando cae por debajo de 1, la epidemia empieza a apagarse |

---

## 14. Exportar datos

En la vista Simulación, el botón **↓ Exportar CSV** descarga la serie diaria completa: una
fila por día con 24 variables, incluidos los compartimentos del modelo, el bloque hospitalario
y el diagnóstico de colas.

El archivo se abre directamente en Excel, R o Python y sirve para análisis que la interfaz no
cubre.

---

## 15. Glosario

**Compartimento.** Cada uno de los grupos en que el modelo divide a la población: susceptible,
expuesto, infeccioso, recuperado.

**Determinista.** Modelo sin azar: la misma entrada produce siempre la misma salida.

**Erlang.** Unidad de carga ofrecida a un sistema de colas. Equivale a las llegadas por unidad
de tiempo multiplicadas por la duración media del servicio.

**Estocástico.** Modelo con azar: cada ejecución produce un resultado distinto salvo que se
fije la semilla.

**Horizonte.** Duración total de la simulación, en días.

**Incidencia.** Casos nuevos que aparecen en un día. No confundir con los casos activos, que
es el acumulado de quienes siguen enfermos.

**Percentil.** Valor por debajo del cual queda un porcentaje dado de las observaciones. El
percentil 95 deja por debajo al 95 % de las corridas.

**R₀.** Número reproductivo básico, en población totalmente susceptible.

**R_e(t).** Número reproductivo efectivo en el día t, ya descontada la población inmune.

**Rho (ρ).** Utilización del sistema de colas. Por encima de 1 la cola crece sin límite.

**Saturación.** Situación en la que la demanda de camas supera la capacidad y se forma cola.

**Semilla.** Número que inicializa el generador de aleatorios. Fijarla hace reproducible una
corrida estocástica.

**Stock y flujo.** Un stock es una cantidad presente en un instante, como la gente en cola. Un
flujo es una cantidad por unidad de tiempo, como los ingresos por día. No se comparan en la
misma escala.

**Tasa de ataque.** Proporción de la población que resulta infectada a lo largo del brote.

---

## 16. Solución de problemas

**El backend no arranca y menciona autenticación.** Las credenciales de PostgreSQL no
coinciden. Se definen con las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`. Si se
arranca desde un IDE, hay que definirlas en su configuración de ejecución, porque una variable
definida en una terminal no llega al IDE.

**El backend no arranca y menciona JAVA_HOME.** La variable apunta a una ruta inexistente o a
una versión distinta de la 17.

**El frontend carga pero las vistas aparecen vacías.** El backend no está respondiendo. Se
comprueba abriendo `http://localhost:8080/api/model/defaults` en el navegador.

**Aparece un error de parámetros al ejecutar.** El mensaje indica exactamente qué restricción
se incumplió. Los casos más frecuentes son una espera máxima menor que el umbral de deterioro,
o una intervención con día de inicio posterior al horizonte.

**La simulación tarda demasiado.** El análisis de incertidumbre con 500 corridas es la
operación más costosa del sistema. Conviene empezar con 50.

**Los escenarios guardados desaparecen al reiniciar.** El backend está corriendo con el perfil
de pruebas, que usa una base en memoria.

---

## 17. Referencia de la API

El backend expone una API REST en `http://localhost:8080`. Documentada por si se desea
integrar el simulador con otra herramienta o automatizar experimentos.

### Simulación

| Método | Ruta | Función |
|---|---|---|
| POST | `/api/simulations/run` | Ejecuta una corrida |
| POST | `/api/simulations/uncertainty` | Ejecuta N corridas y devuelve bandas de percentiles |
| POST | `/api/simulations/compare` | Compara varios escenarios |
| GET | `/api/simulations/{id}` | Recupera una corrida ya ejecutada |
| GET | `/api/simulations/{id}/export` | Descarga la serie diaria en CSV |

### Escenarios

| Método | Ruta | Función |
|---|---|---|
| POST | `/api/scenarios` | Guarda un escenario |
| GET | `/api/scenarios` | Lista los escenarios guardados |
| GET | `/api/scenarios/{id}` | Recupera un escenario |
| DELETE | `/api/scenarios/{id}` | Elimina un escenario |
| POST | `/api/scenarios/{id}/run` | Ejecuta un escenario guardado |

### Consulta

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/model/defaults` | Devuelve los parámetros por defecto |
| GET | `/api/runs` | Historial completo de corridas |
| GET | `/api/runs/scenario/{id}` | Corridas de un escenario concreto |

Todos los campos del cuerpo de la petición son opcionales: los que se omitan se completan con
los valores por defecto. Ejecutar una corrida con la configuración predeterminada requiere
enviar únicamente un objeto vacío.
