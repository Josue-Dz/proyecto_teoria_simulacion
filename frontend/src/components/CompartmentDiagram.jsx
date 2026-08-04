import { useChartColors } from '../state/useTheme.js'

const BOX_W = 108
const BOX_H = 46
const COLUMNAS = [0, 176, 352, 528] 

const FILA_HUMANA = 20
const FILA_VECTOR = 170
const ROTULO_HUMANA = 12
const ROTULO_VECTOR = 160
const BANDA_TEXTO = 96             

const MONO = 'JetBrains Mono, monospace'
const SANS = 'Inter, system-ui, sans-serif'

const centro = (i) => COLUMNAS[i] + BOX_W / 2

const HUMANOS = [
  { col: 0, sym: 'Sₕ', nombre: 'Susceptibles', tono: 'sh' },
  { col: 1, sym: 'Eₕ', nombre: 'Expuestos', tono: 'eh' },
  { col: 2, sym: 'Iₕ', nombre: 'Infecciosos', tono: 'ih' },
  { col: 3, sym: 'Rₕ', nombre: 'Recuperados', tono: 'rh' },
]


const VECTORES = [
  { col: 0, sym: 'Iᵥ', nombre: 'Infecciosos', tono: 'ih' },
  { col: 1, sym: 'Eᵥ', nombre: 'Expuestos', tono: 'eh' },
  { col: 2, sym: 'Sᵥ', nombre: 'Susceptibles', tono: 'sh' },
]

function Caja({ col, y, sym, nombre, color, tinta }) {
  const x = COLUMNAS[col]
  return (
    <g>
      <rect x={x} y={y} width={BOX_W} height={BOX_H} rx="8"
        fill={color} fillOpacity="0.14" stroke={color} strokeWidth="1.5" />
      <text x={x + BOX_W / 2} y={y + 20} textAnchor="middle"
        fontFamily={MONO} fontSize="15" fontWeight="600" fill={color}>{sym}</text>
      <text x={x + BOX_W / 2} y={y + 36} textAnchor="middle"
        fontFamily={SANS} fontSize="10.5" fill={tinta}>{nombre}</text>
    </g>
  )
}

function Flujo({ colDesde, colHasta, y, etiqueta, gris, tinta }) {
  const haciaLaDerecha = colHasta > colDesde
  const x1 = haciaLaDerecha ? COLUMNAS[colDesde] + BOX_W : COLUMNAS[colDesde]
  const x2 = haciaLaDerecha ? COLUMNAS[colHasta] : COLUMNAS[colHasta] + BOX_W
  return (
    <g>
      <line x1={x1} y1={y} x2={x2} y2={y} stroke={gris} strokeWidth="1.5" markerEnd="url(#punta)" />
      <text x={(x1 + x2) / 2} y={y - 9} textAnchor="middle"
        fontFamily={MONO} fontSize="11" fill={tinta}>{etiqueta}</text>
    </g>
  )
}

function Picadura({ col, haciaAbajo, lineas, rojo }) {
  const x = centro(col)
  const y1 = haciaAbajo ? FILA_HUMANA + BOX_H : FILA_VECTOR
  const y2 = haciaAbajo ? FILA_VECTOR : FILA_HUMANA + BOX_H
  return (
    <g>
      <line x1={x} y1={y1} x2={x} y2={y2}
        stroke={rojo} strokeWidth="1.5" strokeDasharray="5 4" markerEnd="url(#punta-roja)" />
      {lineas.map((linea, i) => (
        <text key={linea} x={x + 12} y={BANDA_TEXTO + i * 15}
          fontFamily={SANS} fontSize="11" fill={rojo}>{linea}</text>
      ))}
    </g>
  )
}

function Rotulo({ y, children, gris }) {
  return (
    <text x="0" y={y} fontFamily={MONO} fontSize="10.5" letterSpacing="1.2" fill={gris}>
      {children}
    </text>
  )
}

export default function CompartmentDiagram() {
  const C = useChartColors()
  const gris = C.ink3
  const tinta = C.ink2
  const centroHumana = FILA_HUMANA + BOX_H / 2
  const centroVector = FILA_VECTOR + BOX_H / 2

  return (
    <svg viewBox="0 0 640 224" className="diagrama" role="img"
      aria-labelledby="diag-titulo diag-desc">
      <title id="diag-titulo">Diagrama de compartimentos del modelo SEIR-SEI</title>
      <desc id="diag-desc">
        Los humanos pasan de susceptibles a expuestos, luego a infecciosos y finalmente a
        recuperados. Los mosquitos pasan de susceptibles a expuestos y a infecciosos, sin
        recuperarse nunca. Un mosquito infeccioso contagia a un humano susceptible al picarlo,
        y un mosquito susceptible se contagia al picar a un humano infeccioso: esas dos
        picaduras cierran el ciclo de transmisión.
      </desc>

      <defs>
        <marker id="punta" viewBox="0 0 10 10" refX="9" refY="5"
          markerWidth="6" markerHeight="6" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill={gris} />
        </marker>
        <marker id="punta-roja" viewBox="0 0 10 10" refX="9" refY="5"
          markerWidth="6" markerHeight="6" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" fill={C.red} />
        </marker>
      </defs>

      <Rotulo y={ROTULO_HUMANA} gris={gris}>PERSONAS · S → E → I → R</Rotulo>
      {HUMANOS.map((c) => <Caja key={c.sym} y={FILA_HUMANA} {...c} color={C[c.tono]} tinta={tinta} />)}
      <Flujo colDesde={0} colHasta={1} y={centroHumana} etiqueta="contagio" gris={gris} tinta={tinta} />
      <Flujo colDesde={1} colHasta={2} y={centroHumana} etiqueta="νₕ" gris={gris} tinta={tinta} />
      <Flujo colDesde={2} colHasta={3} y={centroHumana} etiqueta="γₕ" gris={gris} tinta={tinta} />

      <Rotulo y={ROTULO_VECTOR} gris={gris}>MOSQUITOS · S → E → I (sin R)</Rotulo>
      {VECTORES.map((c) => <Caja key={c.sym} y={FILA_VECTOR} {...c} color={C[c.tono]} tinta={tinta} />)}
      <Flujo colDesde={2} colHasta={1} y={centroVector} etiqueta="contagio" gris={gris} tinta={tinta} />
      <Flujo colDesde={1} colHasta={0} y={centroVector} etiqueta="νᵥ" gris={gris} tinta={tinta} />

      
      <Picadura col={2} haciaAbajo lineas={['pica a alguien', 'enfermo', 'b · βᵥ']} rojo={C.red} />

      
      <Picadura col={0} lineas={['pica a alguien', 'sano', 'b · βₕ']} rojo={C.red} />
    </svg>
  )
}
