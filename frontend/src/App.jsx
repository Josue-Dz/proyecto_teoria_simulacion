import { lazy, Suspense, useState } from 'react'
import { useSimulation } from './state/useSimulation.js'
import { useTheme } from './state/useTheme.js'
import { dec2, int } from './lib/format.js'
import EscenarioView from './views/EscenarioView.jsx'
import SimulacionView from './views/SimulacionView.jsx'
import HospitalView from './views/HospitalView.jsx'
import IncertidumbreView from './views/IncertidumbreView.jsx'
import CompararView from './views/CompararView.jsx'
import BibliotecaView from './views/BibliotecaView.jsx'

const ModeloView = lazy(() => import('./views/ModeloView.jsx'))

const VIEWS = [
  { id: 'escenario', label: 'Escenario', Component: EscenarioView },
  { id: 'simulacion', label: 'Simulación', Component: SimulacionView },
  { id: 'hospital', label: 'Hospital', Component: HospitalView },
  { id: 'incertidumbre', label: 'Incertidumbre', Component: IncertidumbreView },
  { id: 'comparar', label: 'Comparar', Component: CompararView },
  { id: 'biblioteca', label: 'Biblioteca', Component: BibliotecaView },
  { id: 'modelo', label: 'Modelo', Component: ModeloView },
]

function BrandMark() {
  return (
    <svg className="brand-mark" viewBox="0 0 26 26" fill="none" aria-hidden="true">
      <line x1="2" y1="17" x2="24" y2="17" stroke="var(--ink-3)" strokeWidth="1.5" strokeDasharray="2 2" />
      <path d="M2 22 C 9 22, 10 5, 15 5 C 20 5, 20 22, 24 22" stroke="var(--teal)" strokeWidth="2" strokeLinecap="round" />
      <circle cx="15" cy="5" r="2.4" fill="var(--red)" />
    </svg>
  )
}

function ThemeToggle() {
  const { tema, alternar } = useTheme()
  const aOscuro = tema === 'light'
  return (
    <button
      className="theme-toggle" onClick={alternar}
      title={aOscuro ? 'Cambiar a modo oscuro' : 'Cambiar a modo claro'}
      aria-label={aOscuro ? 'Cambiar a modo oscuro' : 'Cambiar a modo claro'}
    >
      {aOscuro ? (
        <svg viewBox="0 0 20 20" aria-hidden="true">
          <path d="M16.5 12.3A7 7 0 0 1 7.7 3.5a7 7 0 1 0 8.8 8.8z" fill="currentColor" />
        </svg>
      ) : (
        <svg viewBox="0 0 20 20" aria-hidden="true">
          <circle cx="10" cy="10" r="3.8" fill="currentColor" />
          <g stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
            <path d="M10 1.6v2.1M10 16.3v2.1M1.6 10h2.1M16.3 10h2.1" />
            <path d="M4.1 4.1l1.5 1.5M14.4 14.4l1.5 1.5M15.9 4.1l-1.5 1.5M5.6 14.4l-1.5 1.5" />
          </g>
        </svg>
      )}
    </button>
  )
}

function StatusBar() {
  const { result, model } = useSimulation()
  const kpis = result?.kpis
  const saturated = kpis?.saturated

  return (
    <div className="statusbar">
      <div className="stat-chip">
        <span className="label">R₀</span>
        <span className="value">{kpis ? dec2(kpis.r0) : '—'}</span>
      </div>
      <div className="stat-chip">
        <span className="label">Población</span>
        <span className="value">{int(model.populationHuman)}</span>
      </div>
      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
        {!kpis ? (
          <span className="sat-flag ok">
            <span className="dot ok" /> Sin corrida
          </span>
        ) : saturated ? (
          <span className="sat-flag crit">
            <span className="dot crit" /> Saturación · día {kpis.saturationDay ?? '?'}
          </span>
        ) : (
          <span className="sat-flag ok">
            <span className="dot ok" /> El sistema aguanta
          </span>
        )}
        <ThemeToggle />
      </div>
    </div>
  )
}

function Scrubber() {
  const { result, day, setDay, playing, setPlaying } = useSimulation()
  if (!result) {
    return (
      <div className="scrubber-bar">
        <span className="scrub-empty">Ejecuta una simulación para recorrer la línea de tiempo día a día.</span>
      </div>
    )
  }
  const maxDay = result.series.length - 1
  const satDay = result.kpis?.saturationDay
  return (
    <div className="scrubber-bar">
      <button className="scrub-btn" onClick={() => setPlaying(!playing)} aria-label={playing ? 'Pausar' : 'Reproducir'}>
        {playing ? '❚❚' : '▶'}
      </button>
      <button className="scrub-btn" onClick={() => { setPlaying(false); setDay(0) }} aria-label="Reiniciar">⏮</button>
      <div className="scrub-track">
        <input
          type="range" min={0} max={maxDay} value={day}
          onChange={(e) => { setPlaying(false); setDay(Number(e.target.value)) }}
          aria-label="Día de la simulación"
        />
      </div>
      <div className="scrub-day">
        día <b>{day}</b> / {maxDay}
        {satDay != null && (
          <button
            className="btn btn-ghost btn-sm" style={{ marginLeft: 12 }}
            onClick={() => { setPlaying(false); setDay(satDay) }}
          >
            ir a saturación
          </button>
        )}
      </div>
    </div>
  )
}

export default function App() {
  const [active, setActive] = useState('escenario')
  const View = VIEWS.find((v) => v.id === active).Component

  return (
    <div className="shell">
      <div className="brand">
        <BrandMark />
        <span className="brand-name">Dengue<b>Sim</b></span>
      </div>

      <StatusBar />

      <nav className="nav" aria-label="Vistas">
        {VIEWS.map((v, i) => (
          <button
            key={v.id}
            className={`nav-item ${active === v.id ? 'active' : ''}`}
            onClick={() => setActive(v.id)}
          >
            <span className="nav-index">{String(i + 1).padStart(2, '0')}</span>
            {v.label}
          </button>
        ))}
        <div className="nav-foot">
          IS-910 Teoría de la Simulación · UNAH<br />
          Modelo SEIR-SEI + colas hospitalarias
        </div>
      </nav>

      <main className="main">
        <Suspense fallback={<div className="empty">Cargando…</div>}>
          <View goTo={setActive} />
        </Suspense>
      </main>

      <Scrubber />
    </div>
  )
}
