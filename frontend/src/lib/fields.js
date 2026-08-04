
export const MODEL_FIELDS = [
  { key: 'populationHuman', label: 'Población humana', sym: 'N_h', tex: 'N_h', unit: 'pers.', min: 10000, max: 3000000, step: 1,
    help: 'Total de habitantes de la zona simulada.' },
  { key: 'vectorRatio', label: 'Mosquitos por humano', sym: 'm', tex: 'm', unit: '×', min: 0.5, max: 4, step: 0.1,
    help: 'Tamaño de la población de mosquitos: N_v = m · N_h.' },
  { key: 'bitingRate', label: 'Tasa de picadura', sym: 'b', tex: 'b', unit: '1/día', min: 0.1, max: 1.5, step: 0.05,
    help: 'Picaduras por mosquito por día.' },
  { key: 'betaHuman', label: 'Transmisión vector→humano', sym: 'β_h', tex: '\\beta_h', unit: '', min: 0.05, max: 1, step: 0.05,
    help: 'Probabilidad de contagio de una picadura infectada a un humano.' },
  { key: 'betaVector', label: 'Transmisión humano→vector', sym: 'β_v', tex: '\\beta_v', unit: '', min: 0.05, max: 1, step: 0.05,
    help: 'Probabilidad de que un mosquito se infecte al picar a un humano infeccioso.' },
  { key: 'incubationHumanDays', label: 'Incubación humana', sym: '1/ν_h', tex: '1/\\nu_h', unit: 'días', min: 2, max: 12, step: 0.5,
    help: 'Días desde el contagio hasta volverse infeccioso (intrínseca).' },
  { key: 'incubationVectorDays', label: 'Incubación en mosquito', sym: '1/ν_v', tex: '1/\\nu_v', unit: 'días', min: 4, max: 20, step: 0.5,
    help: 'Periodo de incubación extrínseca (EIP) en el mosquito.' },
  { key: 'infectiousHumanDays', label: 'Periodo infeccioso', sym: '1/γ_h', tex: '1/\\gamma_h', unit: 'días', min: 2, max: 12, step: 0.5,
    help: 'Días que una persona permanece infecciosa.' },
  { key: 'lifeExpectancyDaysHuman', label: 'Esperanza de vida', sym: '1/μ_h', tex: '1/\\mu_h', unit: 'días', min: 7300, max: 36500, step: 365,
    help: 'Controla la renovación demográfica (natalidad = mortalidad).' },
  { key: 'mosquitoLifespanDays', label: 'Vida del mosquito', sym: '1/μ_v', tex: '1/\\mu_v', unit: 'días', min: 5, max: 40, step: 1,
    help: 'Vida media del mosquito adulto. Los modelos publicados usan 20-40 días; en campo la supervivencia es menor.' },
  { key: 'initialInfectedHumans', label: 'Infectados iniciales', sym: 'I_h(0)', tex: 'I_h(0)', unit: 'pers.', min: 1, max: 500, step: 1,
    help: 'Semilla del brote: casos infecciosos en el día 0.' },
  { key: 'initialImmuneFraction', label: 'Inmunidad previa', sym: 'σ', tex: '\\sigma', unit: '', min: 0, max: 0.95, step: 0.05,
    help: 'Fracción de la población ya inmune al serotipo que circula. En zona endémica no todos son susceptibles; súbela y el brote encuentra menos gente a quien contagiar.' },
]

export const HOSPITAL_FIELDS = [
  { key: 'initialBeds', label: 'Camas iniciales', sym: 'c₀', tex: 'c_0', unit: 'camas', min: 10, max: 2000, step: 10,
    help: 'Servidores del sistema de colas: camas disponibles al inicio.' },
  { key: 'hospitalizedFraction', label: 'Fracción hospitalizada', sym: 'p_h', tex: 'p_h', unit: '', min: 0.0005, max: 0.05, step: 0.0005,
    help: 'Proporción de TODAS las infecciones que requiere cama, no solo de los casos con síntomas. La OMS implica ~0,005.' },
  { key: 'admissionDelayDays', label: 'Retardo de ingreso', sym: 'τ', tex: '\\tau', unit: 'días', min: 0, max: 10, step: 1,
    help: 'Días entre el inicio de síntomas y la búsqueda de cama.' },
  { key: 'avgStayDays', label: 'Estancia media', sym: 'LOS', tex: '\\mathrm{LOS}', unit: 'días', min: 1, max: 15, step: 0.5,
    help: 'Duración media de la hospitalización (1/μ_s).' },
  { key: 'deteriorationThresholdDays', label: 'Umbral de deterioro', sym: 'θ', tex: '\\theta', unit: 'días', min: 0.5, max: 7, step: 0.5,
    help: 'Espera tras la cual un paciente vulnerable se agrava.' },
  { key: 'severeFraction', label: 'Fracción vulnerable', sym: 'f_v', tex: 'f_v', unit: '', min: 0, max: 0.6, step: 0.01,
    help: 'Proporción de hospitalizados con riesgo real de muerte. Sube con población más joven o más segundas infecciones (serotipo/ADE).' },
  { key: 'graveMortalityPerDay', label: 'Mortalidad de grave', sym: 'δ', tex: '\\delta', unit: '1/día', min: 0, max: 0.1, step: 0.001,
    help: 'Riesgo diario de muerte de un paciente vulnerable que sigue esperando cama. Con 0,013 un grave que espera 12 días acumula ~15 % de letalidad.' },
  { key: 'maxWaitDays', label: 'Espera máxima', sym: 'W_max', tex: 'W_{max}', unit: 'días', min: 1, max: 30, step: 1,
    help: 'Espera tras la cual el paciente abandona la cola sin haber recibido cama.' },
]

export const INTERVENTION_TYPES = [
  { value: 'FUMIGATION', label: 'Fumigación', tag: '#2e9e6b',
    magLabel: 'Fracción eliminada', magUnit: '0–1', min: 0, max: 1, step: 0.05, def: 0.6, entero: false },
  { value: 'EXTRA_BEDS', label: 'Camas extra', tag: '#0e7c86',
    magLabel: 'Camas añadidas', magUnit: 'camas', min: 0, max: 2000, step: 10, def: 100, entero: true },
  { value: 'TRANSMISSION_REDUCTION', label: 'Reducción de transmisión', tag: '#d99400',
    magLabel: 'Reducción', magUnit: '0–1', min: 0, max: 1, step: 0.05, def: 0.3, entero: false },
]

export const interventionMeta = (type) =>
  INTERVENTION_TYPES.find((t) => t.value === type) || INTERVENTION_TYPES[0]
