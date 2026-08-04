import { useSimulation } from '../state/useSimulation.js'
import { buildExportCsvUrl } from "../api/services/simulationService";
import { TimeSeries } from '../components/Charts.jsx'
import { useChartColors } from '../state/useTheme.js'
import { Telemetry } from '../components/Telemetry.jsx'
import { Empty, CardHead } from '../components/Common.jsx'
import { int, dec1, dec2, pct, ratioX } from '../lib/format.js'

export default function SimulacionView({ goTo }) {
  const { result, day, current } = useSimulation()
  const C = useChartColors()

  if (!result) {
    return (
      <Empty
        title="Aún no hay una corrida"
        action={<button className="btn btn-primary" onClick={() => goTo('escenario')}>Ir a Escenario</button>}
      >
        Configura el escenario y ejecuta la simulación para ver la curva epidémica y el momento
        en que el sistema hospitalario se satura.
      </Empty>
    )
  }

  const k = result.kpis
  const data = result.series
  const satDay = k.saturationDay

  const primary = [
    { label: 'R₀', value: dec2(k.r0) },
    { label: 'Día de saturación', value: k.saturated ? int(k.saturationDay) : 'nunca', tone: k.saturated ? 'crit' : '' },
    { label: 'Presión pico', value: ratioX(k.peakPressureRatio), tone: k.peakPressureRatio > 1 ? 'crit' : '', unit: `día ${k.peakPressureDay ?? '—'}` },
    { label: 'Muertes por saturación', value: int(k.deathsFromSaturation), tone: k.deathsFromSaturation > 0 ? 'crit' : '' },
  ]
  const secondary = [
    { label: 'Tasa de ataque final', value: pct(k.finalAttackRate) },
    { label: 'Hospitalizaciones', value: int(k.totalHospitalizations) },
    { label: 'Espera media por cama', value: `${dec1(k.avgWaitDays)}`, unit: 'días' },
    { label: 'Déficit máximo de camas', value: int(k.maxBedDeficit), tone: k.maxBedDeficit > 0 ? 'warn' : '' },
  ]

  return (
    <div>
      <div className="view-head">
        <span className="eyebrow">02 · Resultados</span>
        <h1>Simulación</h1>
        <p>El punto donde la demanda de camas cruza la capacidad es el momento de saturación: ahí el sistema deja de dar abasto.</p>
      </div>

      <div className="row gap-12" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
        <a className="btn btn-ghost btn-sm" href={buildExportCsvUrl(result.id)} download>
          ↓ Exportar CSV
        </a>
        <span className="help">
          Descarga la serie diaria completa ({data.length} filas × {Object.keys(data[0]).length - 1} variables)
          para analizarla en Excel, R o Python.
        </span>
      </div>

      <Telemetry items={primary} />
      <div style={{ height: 12 }} />
      <Telemetry items={secondary} />

      
      <div className="card" style={{ marginTop: 20 }}>
        <CardHead
          title="Ocupación de camas frente a capacidad"
          sub={satDay != null ? `saturación en el día ${satDay}` : 'el sistema no se satura'}
          right={<span className={`sat-flag ${k.saturated ? 'crit' : 'ok'}`}>
            <span className={`dot ${k.saturated ? 'crit' : 'ok'}`} />
            {k.saturated ? 'Saturado' : 'Estable'}
          </span>}
        />
        <div className="card-pad">
          <TimeSeries
            data={data}
            height={250}
            yLabel="camas"
            series={[
              { key: 'bedsOccupied', name: 'Camas ocupadas', color: C.teal, area: true },
              { key: 'capacity', name: 'Capacidad', color: C.red, dashed: true },
            ]}
            refs={satDay != null ? [{ x: satDay, color: C.red, label: `día ${satDay}` }] : []}
            marker={day}
          />
        </div>
      </div>

      
      <div className="card" style={{ marginTop: 14 }}>
        <CardHead
          title="Pacientes sin cama"
          sub="los que esperan a que se libere una, el mismo eje de días que la gráfica de arriba"
        />
        <div className="card-pad">
          <TimeSeries
            data={data}
            height={190}
            yLabel="pacientes"
            series={[{ key: 'queueLength', name: 'En cola', color: C.amber, area: true }]}
            refs={satDay != null ? [{ x: satDay, color: C.red, label: `día ${satDay}` }] : []}
            marker={day}
          />
        </div>
      </div>

      
      <div className="grid" style={{ gridTemplateColumns: '1.6fr 1fr', marginTop: 18 }}>
        <div className="card">
          <CardHead title="Curva epidémica" sub="incidencia e infecciosos activos" />
          <div className="card-pad">
            <TimeSeries
              data={data}
              height={280}
              yLabel="personas"
              series={[
                
                
                { key: 'newCases', name: 'Nuevos casos/día', color: C.incidence, area: true },
                { key: 'ih', name: 'Infecciosos activos (I_h)', color: C.red },
                { key: 'eh', name: 'Expuestos (E_h)', color: C.amber },
              ]}
              marker={day}
            />
          </div>
        </div>

        <div className="card">
          <CardHead title={`Estado en el día ${day}`} />
          <div className="card-pad">
            {current && (
              <table className="data">
                <tbody>
                  <tr><td>Infecciosos activos</td><td className="num">{int(current.ih)}</td></tr>
                  <tr><td>Nuevos casos ese día</td><td className="num">{int(current.newCases)}</td></tr>
                  <tr><td>Camas ocupadas</td><td className="num">{int(current.bedsOccupied)} / {int(current.capacity)}</td></tr>
                  <tr><td>En cola</td><td className="num">{int(current.queueLength)}</td></tr>
                  <tr><td>Ocupación</td><td className="num">{pct(current.occupancyRatio)}</td></tr>
                  <tr><td>Presión (ocupadas+cola)/cap.</td><td className="num">{ratioX(current.pressureRatio)}</td></tr>
                  <tr><td>R efectivo R_e(t)</td><td className="num">{dec2(current.rEffective)}</td></tr>
                  <tr><td>Muertes acumuladas</td><td className="num">{int(current.cumulativeDeaths)}</td></tr>
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
