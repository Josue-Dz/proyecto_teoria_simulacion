import {
  ResponsiveContainer, ComposedChart, AreaChart, Line, Area,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ReferenceLine,
} from 'recharts'
import { int } from '../lib/format.js'
import { useChartColors } from '../state/useTheme.js'

const ejeBase = { fontSize: 11, fontFamily: 'JetBrains Mono, monospace' }

function DengueTooltip({ active, payload, label }) {
  const C = useChartColors()
  if (!active || !payload || !payload.length) return null
  return (
    <div style={{
      background: C.surface, border: `1px solid ${C.border}`, borderRadius: 8,
      padding: '9px 12px', boxShadow: `0 4px 16px ${C.sombra}`, fontSize: 12,
    }}>
      <div style={{ fontFamily: 'JetBrains Mono, monospace', color: C.ink3, marginBottom: 5 }}>
        día {label}
      </div>
      {payload.map((p) => (
        <div key={p.dataKey} style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 2 }}>
          <span style={{ width: 9, height: 9, borderRadius: 2, background: p.color, display: 'inline-block' }} />
          <span style={{ color: C.ink2 }}>{p.name}</span>
          <span style={{ marginLeft: 'auto', fontFamily: 'JetBrains Mono, monospace', fontWeight: 500, color: C.ink }}>
            {int(p.value)}
          </span>
        </div>
      ))}
    </div>
  )
}


const miles = (v) => {
  if (!Number.isFinite(v)) return ''
  if (Math.abs(v) < 1e-9) return '0'
  return Math.abs(v) >= 1000 ? `${Math.round(v / 1000)}k` : `${Number(v.toFixed(2))}`
}


const DESDE_CERO = [0, techoRedondo]

function techoRedondo(max) {
  if (!(max > 0)) return 1
  const conAire = max * 1.05
  const paso = 10 ** Math.floor(Math.log10(conAire)) / 2
  return Math.ceil(conAire / paso) * paso
}


const MARGEN = { top: 24, right: 16, bottom: 8, left: 4 }


function conDatos(series, data) {
  if (!data || data.length === 0) return series
  return series.filter((s) =>
    data.some((fila) => {
      const v = fila[s.key]
      return v != null && Math.abs(v) > 1e-9
    }),
  )
}

export function TimeSeries({ data, series: seriesPedidas, refs = [], marker, height = 300, yLabel }) {
  const C = useChartColors()
  const axisStyle = { ...ejeBase, fill: C.ink3 }
  const series = conDatos(seriesPedidas, data)

  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={data} margin={MARGEN}>
        <CartesianGrid stroke={C.line} strokeDasharray="3 3" vertical={false} />
        
        <XAxis dataKey="day" tick={axisStyle} stroke={C.line} tickLine={false}
          label={{ value: 'día', position: 'insideBottomRight', offset: -2, style: axisStyle }} />

        <YAxis yAxisId="left" tick={axisStyle} stroke={C.line} tickLine={false} width={54}
          domain={DESDE_CERO} tickFormatter={miles}
          label={yLabel ? { value: yLabel, angle: -90, position: 'insideLeft', style: axisStyle } : undefined} />

        <Tooltip content={<DengueTooltip />} />
        <Legend wrapperStyle={{ fontSize: 12, paddingTop: 12 }} iconType="plainline" />
        {series.map((s) =>
          s.area ? (
            <Area key={s.key} yAxisId="left" type="monotone" dataKey={s.key} name={s.name}
              stroke={s.color} fill={s.color} fillOpacity={0.12} strokeWidth={2}
              dot={false} isAnimationActive={false} />
          ) : (
            <Line key={s.key} yAxisId="left" type="monotone" dataKey={s.key} name={s.name}
              stroke={s.color} strokeWidth={2} strokeDasharray={s.dashed ? '6 4' : undefined}
              dot={false} isAnimationActive={false} />
          ),
        )}
        {refs.map((r, i) => (
          <ReferenceLine key={`r${i}`} yAxisId="left" x={r.x} stroke={r.color} strokeWidth={1.5}
            label={{ value: r.label, position: 'top', fill: r.color, fontSize: 11, fontFamily: 'JetBrains Mono, monospace' }} />
        ))}
        {marker != null && (
          <ReferenceLine yAxisId="left" x={marker} stroke={C.ink} strokeWidth={1} strokeDasharray="2 2" opacity={0.5} />
        )}
      </ComposedChart>
    </ResponsiveContainer>
  )
}

function BandTooltip({ active, payload, label, color, name }) {
  const C = useChartColors()
  if (!active || !payload || !payload.length) return null
  const d = payload[0].payload
  const fila = (marca, texto, valor) => (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 3 }}>
      {marca}
      <span style={{ color: C.ink2 }}>{texto}</span>
      <span style={{ marginLeft: 'auto', fontFamily: 'JetBrains Mono, monospace', fontWeight: 500, color: C.ink }}>
        {valor}
      </span>
    </div>
  )
  return (
    <div style={{
      background: C.surface, border: `1px solid ${C.border}`, borderRadius: 8,
      padding: '9px 12px', boxShadow: `0 4px 16px ${C.sombra}`, fontSize: 12, minWidth: 190,
    }}>
      <div style={{ fontFamily: 'JetBrains Mono, monospace', color: C.ink3, marginBottom: 5 }}>día {label}</div>
      {fila(
        <span style={{ width: 10, height: 2.5, background: color, display: 'inline-block' }} />,
        name, int(d.mid),
      )}
      {fila(
        <span style={{ width: 10, height: 9, background: color, opacity: 0.28, borderRadius: 2, display: 'inline-block' }} />,
        'rango p05–p95', `${int(d.low)} – ${int(d.p95)}`,
      )}
    </div>
  )
}


export function BandChart({ points, color, name, height = 300, marker }) {
  const C = useChartColors()
  const axisStyle = { ...ejeBase, fill: C.ink3 }
  const trazo = color ?? C.teal
  const etiqueta = name || 'mediana p50'
  const data = points.map((p) => ({ day: p.day, low: p.p05, mid: p.p50, span: p.p95 - p.p05, p95: p.p95 }))
  return (
    <>
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data} margin={MARGEN}>
        <CartesianGrid stroke={C.line} strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="day" tick={axisStyle} stroke={C.line} tickLine={false}
          label={{ value: 'día', position: 'insideBottomRight', offset: -2, style: axisStyle }} />
        <YAxis tick={axisStyle} stroke={C.line} tickLine={false} width={54}
          domain={DESDE_CERO} tickFormatter={miles} />
        <Tooltip content={<BandTooltip color={trazo} name={etiqueta} />} />
        
        <Area type="monotone" dataKey="low" stackId="band" stroke="none" fill="none" isAnimationActive={false} />
        <Area type="monotone" dataKey="span" stackId="band" stroke="none" fill={trazo} fillOpacity={0.16} isAnimationActive={false} />
        <Area type="monotone" dataKey="mid" stroke={trazo} strokeWidth={2} fill="none" dot={false} isAnimationActive={false} />
        {marker != null && <ReferenceLine x={marker} stroke={C.ink} strokeWidth={1} strokeDasharray="2 2" opacity={0.5} />}
      </AreaChart>
    </ResponsiveContainer>
    <div className="row gap-16" style={{ justifyContent: 'center', fontSize: 12, color: C.ink2, paddingTop: 4 }}>
      <span className="row gap-8">
        <span style={{ width: 14, height: 2.5, background: trazo, display: 'inline-block' }} />
        {etiqueta}
      </span>
      <span className="row gap-8">
        <span style={{ width: 14, height: 10, background: trazo, opacity: 0.28, borderRadius: 2, display: 'inline-block' }} />
        rango p05–p95
      </span>
    </div>
    </>
  )
}
