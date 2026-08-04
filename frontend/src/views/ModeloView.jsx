import { useSimulation } from '../state/useSimulation.js'
import { CardHead } from '../components/Common.jsx'
import { Eq, Tex } from '../components/Math.jsx'
import CompartmentDiagram from '../components/CompartmentDiagram.jsx'
import { MODEL_FIELDS, HOSPITAL_FIELDS } from '../lib/fields.js'
import { computeR0 } from '../lib/r0.js'
import { dec2, int } from '../lib/format.js'

function fmtValue(f, v) {
  if (v == null || Number.isNaN(v)) return '—'
  if (f.key === 'populationHuman' || f.key === 'lifeExpectancyDaysHuman') return int(v)
  if (f.step >= 1) return int(v)
  return dec2(v)
}


function FilaGlosario({ f, valor }) {
  return (
    <tr>
      <td><Tex>{f.tex}</Tex></td>
      <td>{f.label}<div className="help">{f.help}</div></td>
      <td className="num">{fmtValue(f, valor)} {f.unit}</td>
    </tr>
  )
}



const SALIDAS_COLA = [
  {
    color: 'var(--teal)',
    titulo: 'Ingresa',
    texto: 'Se libera una cama y entra. Tienen prioridad los que llevan más tiempo esperando.',
  },
  {
    color: 'var(--red)',
    titulo: 'Muere esperando',
    texto: 'Solo puede pasarle a un paciente vulnerable que ya se agravó y sigue sin cama.',
  },
  {
    color: 'var(--amber)',
    titulo: 'Abandona sin cama',
    texto: 'Superó la espera máxima. Si era un caso moderado, se recupera por su cuenta.',
  },
  {
    color: 'var(--ink-3)',
    titulo: 'Sigue en cola',
    texto: 'La simulación terminó mientras aún esperaba.',
  },
]

export default function ModeloView({ goTo }) {
  const { model, hospital } = useSimulation()
  const r0 = computeR0(model)

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">07 · Marco teórico</span>
        <h1>Modelo</h1>
        <p>
          DengueSim encadena dos modelos: uno <b>epidemiológico</b>, que reparte a la población en
          compartimentos y calcula cuánta gente enferma cada día, y uno <b>de colas</b>, que toma esos
          enfermos y los enfrenta a un número finito de camas. Esta página documenta ambos: qué
          significa cada ecuación y qué representa cada variable que puedes mover en Escenario.
        </p>
      </div>

      
      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Cómo se mueve la epidemia"
          sub="7 compartimentos · los colores son los mismos de las curvas en Simulación" />
        <div className="card-pad">
          <div className="diagrama-wrap">
            <CompartmentDiagram />
          </div>
          <p className="prose" style={{ maxWidth: 'none', marginTop: 16, marginBottom: 0 }}>
            Cada caja es un grupo de personas o de mosquitos, y cada flecha es el paso de un grupo al
            siguiente. Lo importante del dengue está en las dos flechas rojas: <b>nadie contagia a
            nadie directamente</b>. Una persona sana solo se enferma si la pica un mosquito infectado,
            y un mosquito solo se infecta si pica a una persona enferma. Por eso hay dos cadenas y no
            una, y por eso fumigar y vacunar atacan el mismo problema por lados distintos.
            Los mosquitos no tienen casilla de «recuperados»: viven unas dos semanas, así que el que
            se infecta muere infeccioso.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Número reproductivo básico · R₀"
          sub="matriz de próxima generación · se recalcula con tus parámetros actuales" />
        <div className="card-pad">
          <div className="r0-layout">
            <div>
              <div className="mono" style={{ fontSize: 44, fontWeight: 600, lineHeight: 1, color: r0 >= 1 ? 'var(--red-ink)' : 'var(--teal-ink)' }}>
                {dec2(r0)}
              </div>
              <div className="help" style={{ marginTop: 4 }}>
                {r0 >= 1
                  ? 'R₀ ≥ 1: cada caso genera más de uno, así que el brote crece.'
                  : 'R₀ < 1: el brote no se sostiene y se extingue.'}
              </div>
            </div>
            <Eq note="La raíz cuadrada está porque el contagio necesita dos pasos: de persona a mosquito y de mosquito a persona. R₀ mide el ciclo completo.">
              {String.raw`R_0 = \sqrt{\frac{b^{2}\,\beta_h\,\beta_v\,\nu_h\,\nu_v\,N_v}
                {(\nu_h+\mu_h)\,(\gamma_h+\mu_h)\,(\nu_v+\mu_v)\,\mu_v\,N_h}}`}
            </Eq>
          </div>
          <p className="help" style={{ marginTop: 14, marginBottom: 0 }}>
            <b>En una frase:</b> a cuántas personas contagia, en promedio, un enfermo en una población
            donde nadie es inmune todavía. Por encima de 1 el brote crece solo, por debajo se apaga.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 18 }}>
        <CardHead title="Cómo leer las ecuaciones" sub="para quien no trabaja con ecuaciones diferenciales" />
        <div className="card-pad">
          <p className="prose" style={{ maxWidth: 'none', margin: 0 }}>
            Cada ecuación describe <b>una sola caja del diagrama</b> y responde siempre a la misma
            pregunta: cuánta gente entra y cuánta sale de esa caja hoy. La parte de la izquierda,{' '}
            <Tex>{'dS_h/dt'}</Tex>, se lee «lo que cambia el grupo <Tex>{'S_h'}</Tex> por día». A la
            derecha, los términos que <b>suman</b> son flechas que entran y los que <b>restan</b> son
            flechas que salen. Nada más. Un símbolo como <Tex>{'\\nu_h'}</Tex> es una velocidad: si la
            incubación dura 5 días, entonces <Tex>{'\\nu_h = 1/5'}</Tex>, o sea que cada día pasa a
            infeccioso una quinta parte de los que están incubando.
          </p>
        </div>
      </div>

      <div className="grid cols-2">
        <div className="card">
          <CardHead title="Dinámica humana" sub="SEIR · susceptible → expuesto → infeccioso → recuperado" />
          <div className="card-pad">
            <Eq note="Nacen personas sanas y salen dos grupos: los que pica un mosquito infectado y los que mueren por causas naturales.">
              {String.raw`\frac{dS_h}{dt} = \mu_h N_h - \frac{b\,\beta_h}{N_h}\,S_h I_v - \mu_h S_h`}
            </Eq>
            <Eq note="Los recién contagiados esperan aquí mientras incuban: ya tienen el virus, pero todavía no contagian.">
              {String.raw`\frac{dE_h}{dt} = \frac{b\,\beta_h}{N_h}\,S_h I_v - (\nu_h + \mu_h)\,E_h`}
            </Eq>
            <Eq note="Al terminar la incubación pasan a infecciosos. Son los que alimentan la demanda de camas.">
              {String.raw`\frac{dI_h}{dt} = \nu_h E_h - (\gamma_h + \mu_h)\,I_h`}
            </Eq>
            <Eq note="Quien se recupera queda inmune y ya no vuelve a entrar al ciclo.">
              {String.raw`\frac{dR_h}{dt} = \gamma_h I_h - \mu_h R_h`}
            </Eq>
            <p className="help" style={{ marginTop: 10 }}>
              Fíjate en que el contagio depende de <Tex>{'I_v'}</Tex>, los mosquitos infecciosos, y no
              de <Tex>{'I_h'}</Tex>: por eso el dengue no se contagia de persona a persona.
            </p>
          </div>
        </div>

        <div className="card">
          <CardHead title="Dinámica del vector" sub="SEI · el mosquito no se recupera (muere infeccioso)" />
          <div className="card-pad">
            <Eq note="Nacen mosquitos sanos y se infectan al picar a alguien enfermo. La población se repone sola: por eso fumigar da un respiro, no una solución.">
              {String.raw`\frac{dS_v}{dt} = \mu_v N_v - \frac{b\,\beta_v}{N_h}\,S_v I_h - \mu_v S_v`}
            </Eq>
            <Eq note="El virus incuba dentro del mosquito. Como su vida es corta, muchos mueren antes de llegar a contagiar.">
              {String.raw`\frac{dE_v}{dt} = \frac{b\,\beta_v}{N_h}\,S_v I_h - (\nu_v + \mu_v)\,E_v`}
            </Eq>
            <Eq note="Un mosquito que llega a infeccioso lo será el resto de su vida: la única salida es la muerte.">
              {String.raw`\frac{dI_v}{dt} = \nu_v E_v - \mu_v I_v`}
            </Eq>
            <p className="help" style={{ marginTop: 10 }}>
              No hay compartimento <Tex>{'R_v'}</Tex> porque el mosquito vive unas dos semanas: se
              asume que muere antes de dejar de ser infeccioso. El sistema se resuelve con Runge-Kutta
              de 4.º orden, avanzando de <Tex>{'0{,}1'}</Tex> en <Tex>{'0{,}1'}</Tex> días.
            </p>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 18 }}>
        <CardHead title="Modelo de colas hospitalario" sub="M/M/c con deterioro · Erlang-C como validación analítica" />
        <div className="card-pad">
          <div className="prose" style={{ maxWidth: 'none' }}>
            <p>
              Aquí el hospital se modela como cualquier sistema de colas, una fila de banco o un
              peaje, con una diferencia incómoda: esperar puede matar. Las <b>camas son los servidores</b> y
              los pacientes son la fila. De todos los infecciosos, una fracción <Tex>{'p_h'}</Tex>{' '}
              necesita cama, y llega <Tex>{'\\tau'}</Tex> días después de enfermarse. Cada paciente
              ocupa su cama <Tex>{'\\mathrm{LOS}'}</Tex> días en promedio.
            </p>
          </div>

          <div className="grid cols-3" style={{ marginTop: 12 }}>
            <Eq note="Cuánto trabajo llega al día, medido en camas: llegadas × días que ocupa cada una.">
              {String.raw`a = \lambda \cdot \mathrm{LOS}`}
            </Eq>
            <Eq note="Qué tan lleno está el sistema. Por encima de 1, llega más gente de la que puede salir.">
              {String.raw`\rho = \frac{a}{c}`}
            </Eq>
            <Eq note="Cuánto espera en promedio quien no encuentra cama libre.">
              {String.raw`W_q = \frac{C(c,\,a)}{c\,\mu_s - \lambda}`}
            </Eq>
          </div>

          <p className="help" style={{ marginTop: 14 }}>
            <Tex>{'C(c,\\,a)'}</Tex> es la fórmula de <b>Erlang-C</b>: la probabilidad de que alguien
            que llega tenga que esperar. Estas tres fórmulas son teoría de colas clásica y se calculan
            en paralelo a la simulación, como forma de <b>contrastar</b> que el simulador da lo que la
            teoría predice. Cuando <Tex>{'\\rho \\geq 1'}</Tex> dejan de tener validez, porque la cola
            ya no se estabiliza: ahí manda la simulación.
          </p>

          <div className="group-title" style={{ marginTop: 24 }}>
            Las cuatro salidas de la cola
          </div>
          <div className="salidas">
            {SALIDAS_COLA.map((s) => (
              <div className="salida" key={s.titulo} style={{ borderLeftColor: s.color }}>
                <div className="salida-titulo">{s.titulo}</div>
                <div className="help">{s.texto}</div>
              </div>
            ))}
          </div>

          <p className="help" style={{ marginTop: 14, marginBottom: 0 }}>
            No todos los que esperan corren el mismo riesgo. Solo la fracción vulnerable{' '}
            <Tex>{'f_v'}</Tex> puede morir: tras <Tex>{'\\theta'}</Tex> días de espera se agrava, y a
            partir de ahí tiene una probabilidad <Tex>{'\\delta'}</Tex> de morir cada día que siga sin
            cama. El resto son casos moderados que salen adelante con manejo básico aunque nunca
            reciban una. Ese es el mecanismo que convierte la falta de camas en muertes evitables, y
            los dos motores del simulador, el determinista y el de eventos discretos, lo implementan
            igual, así que sus resultados deben coincidir.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginTop: 18 }}>
        <CardHead title="Glosario de variables" sub="valores actuales · edítalos en Escenario" />
        <div className="card-pad">
          <div className="group-title">Modelo epidemiológico</div>
          <table className="data glosario">
            <thead>
              <tr><th>Símbolo</th><th>Variable</th><th className="num">Valor actual</th></tr>
            </thead>
            <tbody>
              {MODEL_FIELDS.map((f) => (
                <FilaGlosario key={f.key} f={f} valor={model[f.key]} />
              ))}
            </tbody>
          </table>

          <div className="group-title" style={{ marginTop: 22 }}>Sistema hospitalario</div>
          <table className="data glosario">
            <thead>
              <tr><th>Símbolo</th><th>Variable</th><th className="num">Valor actual</th></tr>
            </thead>
            <tbody>
              {HOSPITAL_FIELDS.map((f) => (
                <FilaGlosario key={f.key} f={f} valor={hospital[f.key]} />
              ))}
            </tbody>
          </table>

          <p className="help" style={{ marginTop: 14 }}>
            Una duración y una velocidad son la misma cosa vista al revés: donde el glosario dice{' '}
            <Tex>{'1/\\nu_h'}</Tex> «5 días de incubación», las ecuaciones usan <Tex>{'\\nu_h'}</Tex>,
            que vale <Tex>{'1/5'}</Tex> por día.
          </p>

          <div style={{ marginTop: 18 }}>
            <button className="btn btn-primary" onClick={() => goTo('escenario')}>Ir a Escenario</button>
          </div>
        </div>
      </div>

      <div className="prose" style={{ marginTop: 22 }}>
        <p className="help">
          Referencia del modelo: Bhuju, G., Phaijoo, G. R. &amp; Gurung, D. B. (2020).
          <i> Fuzzy Approach Analyzing SEIR-SEI Dengue Dynamics.</i> BioMed Research International.
          Adaptado al caso de la emergencia por dengue en Honduras (2024), Distrito Central.
        </p>
      </div>
    </div>
  )
}
