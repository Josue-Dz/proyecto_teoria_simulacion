export function computeR0(m) {
  if (!m) return NaN
  const nh = m.populationHuman
  const nv = m.vectorRatio * nh
  const b = m.bitingRate
  const bh = m.betaHuman
  const bv = m.betaVector
  const nuH = 1 / m.incubationHumanDays
  const nuV = 1 / m.incubationVectorDays
  const gammaH = 1 / m.infectiousHumanDays
  const muH = 1 / m.lifeExpectancyDaysHuman
  const muV = 1 / m.mosquitoLifespanDays

  const numerator = b * b * bh * bv * nuH * nuV * nv
  const denominator = (nuH + muH) * (gammaH + muH) * (nuV + muV) * muV * nh
  if (denominator <= 0) return NaN
  return Math.sqrt(numerator / denominator)
}

export function computeRe0(m) {
  if (!m) return NaN
  const sigma = clamp01(m.initialImmuneFraction ?? 0)
  return computeR0(m) * Math.sqrt(1 - sigma)
}

export function herdImmunityThreshold(m) {
  const r0 = computeR0(m)
  if (!Number.isFinite(r0) || r0 <= 1) return 0
  return 1 - 1 / (r0 * r0)
}

function clamp01(v) {
  return Math.max(0, Math.min(1, v))
}
