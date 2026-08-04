package unah.hn.service;

import unah.hn.dto.BandPoint;
import unah.hn.dto.CompareRequest;
import unah.hn.dto.CompareResult;
import unah.hn.dto.DailyState;
import unah.hn.dto.HospitalParameters;
import unah.hn.dto.Intervention;
import unah.hn.dto.ModelParameters;
import unah.hn.dto.SimulationKpis;
import unah.hn.dto.SimulationRequest;
import unah.hn.dto.SimulationResult;
import unah.hn.dto.UncertaintyRequest;
import unah.hn.dto.UncertaintyResult;
import unah.hn.engine.EpidemicOutput;
import unah.hn.engine.HospitalDesEngine;
import unah.hn.engine.HospitalEngine;
import unah.hn.engine.HospitalOutput;
import unah.hn.engine.Rng;
import unah.hn.engine.SeirSeiEngine;
import unah.hn.model.SimulationRunEntity;
import unah.hn.repository.SimulationRunRepository;
import unah.hn.repository.SimulationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SeirSeiEngine epidemicEngine;
    private final HospitalEngine hospitalEngine;         // determinista (línea base)
    private final HospitalDesEngine desEngine;           // estocástico (eventos discretos)
    private final KpiCalculator kpiCalculator;
    private final DefaultsProvider defaults;
    private final SimulationStore store;
    private final SimulationRunRepository runRepository;

    public SimulationService(SeirSeiEngine epidemicEngine, HospitalEngine hospitalEngine,
                             HospitalDesEngine desEngine, KpiCalculator kpiCalculator,
                             DefaultsProvider defaults, SimulationStore store,
                             SimulationRunRepository runRepository) {
        this.epidemicEngine = epidemicEngine;
        this.hospitalEngine = hospitalEngine;
        this.desEngine = desEngine;
        this.kpiCalculator = kpiCalculator;
        this.defaults = defaults;
        this.store = store;
        this.runRepository = runRepository;
    }

    public SimulationResult run(SimulationRequest request) {
        return execute(request, null);
    }

    /** Corrida de un escenario guardado: captura la corrida ligada a su id. */
    public SimulationResult runForScenario(SimulationRequest request, UUID scenarioId) {
        return execute(request, scenarioId);
    }

    private SimulationResult execute(SimulationRequest request, UUID scenarioId) {
        SimulationRequest req = defaults.normalize(request);
        validate(req);
        long seed = (req.seed() != null) ? req.seed() : ThreadLocalRandom.current().nextLong();
        SimulationResult result = simulate(req, seed);
        store.save(result);
        capture(req, result, seed, scenarioId);
        return result;
    }

    private SimulationResult simulate(SimulationRequest req, long seed) {
        int horizon = req.horizonDays();
        EpidemicOutput epi = epidemicEngine.integrate(
                req.model(), req.interventions(), horizon, 1.0);

        boolean stochastic = Boolean.TRUE.equals(req.stochastic());
        HospitalOutput hosp = stochastic
                ? desEngine.simulate(epi.dailyIncidence(), req.hospital(), req.interventions(), horizon, seed)
                : hospitalEngine.simulate(epi.dailyIncidence(), req.hospital(), req.interventions(), horizon);

        List<DailyState> series = buildSeries(epi, hosp, horizon);
        SimulationKpis kpis = kpiCalculator.compute(epi, hosp, req.model(), horizon);
        return new SimulationResult(UUID.randomUUID().toString(), series, kpis);
    }

    private List<DailyState> buildSeries(EpidemicOutput epi, HospitalOutput hosp, int horizon) {
        List<DailyState> series = new ArrayList<>(horizon + 1);
        for (int t = 0; t <= horizon; t++) {
            double[] s = epi.states()[t];
            double cap = hosp.capacity()[t];
            double occ = hosp.occupied()[t];
            double q = hosp.queue()[t];
            series.add(new DailyState(
                    t,
                    s[SeirSeiEngine.SH], s[SeirSeiEngine.EH], s[SeirSeiEngine.IH], s[SeirSeiEngine.RH],
                    s[SeirSeiEngine.SV], s[SeirSeiEngine.EV], s[SeirSeiEngine.IV],
                    epi.dailyIncidence()[t],
                    occ, q, cap,
                    hosp.admissions()[t], hosp.discharges()[t],
                    hosp.deaths()[t], hosp.cumulativeDeaths()[t],
                    hosp.leftWithoutBed()[t],
                    hosp.demand()[t],
                    cap > 0 ? occ / cap : 0.0,
                    cap > 0 ? (occ + q) / cap : 0.0,
                    epi.rEffective()[t],
                    hosp.offeredLoad()[t], hosp.rho()[t], hosp.probWait()[t], hosp.waitDaysQueue()[t]
            ));
        }
        return series;
    }

    public UncertaintyResult uncertainty(UncertaintyRequest request) {
        SimulationRequest req = defaults.normalize(request != null ? request.base() : null);
        validate(req);
        int runs = (request != null && request.runs() != null) ? request.runs() : 200;
        runs = Math.max(2, Math.min(runs, 1000));
        long baseSeed = (req.seed() != null) ? req.seed() : 42L;
        int horizon = req.horizonDays();
        double sigma = (request != null && request.transmissionVariability() != null)
                ? Math.max(0.0, Math.min(1.0, request.transmissionVariability()))
                : 0.10;

        EpidemicOutput epiComun = (sigma == 0.0)
                ? epidemicEngine.integrate(req.model(), req.interventions(), horizon, 1.0)
                : null;
        Rng ruido = new Rng(baseSeed);

        String[] vars = {"newCases", "bedsOccupied", "queueLength", "cumulativeDeaths"};
        Map<String, double[][]> samples = new LinkedHashMap<>();
        for (String v : vars) samples.put(v, new double[runs][horizon + 1]);

        List<SimulationKpis> perRunKpis = new ArrayList<>(runs);
        for (int r = 0; r < runs; r++) {
            EpidemicOutput epi = (epiComun != null) ? epiComun : epidemicEngine.integrate(
                    req.model(), req.interventions(), horizon, ruido.logNormalMultiplier(sigma));
            HospitalOutput hosp = desEngine.simulate(
                    epi.dailyIncidence(), req.hospital(), req.interventions(), horizon, baseSeed + r);
            for (int t = 0; t <= horizon; t++) {
                samples.get("newCases")[r][t] = epi.dailyIncidence()[t];
                samples.get("bedsOccupied")[r][t] = hosp.occupied()[t];
                samples.get("queueLength")[r][t] = hosp.queue()[t];
                samples.get("cumulativeDeaths")[r][t] = hosp.cumulativeDeaths()[t];
            }
            perRunKpis.add(kpiCalculator.compute(epi, hosp, req.model(), horizon));
        }

        Map<String, List<BandPoint>> bands = new LinkedHashMap<>();
        for (String v : vars) {
            bands.put(v, toBands(samples.get(v), horizon));
        }

        long saturadas = perRunKpis.stream().filter(SimulationKpis::saturated).count();
        double pSaturacion = (double) saturadas / runs;

        return new UncertaintyResult(UUID.randomUUID().toString(), runs, pSaturacion,
                bands, medianKpis(perRunKpis));
    }

    /**
     * Mediana de cada indicador sobre las N realizaciones
     */
    private SimulationKpis medianKpis(List<SimulationKpis> runs) {
        boolean saturated = runs.stream().filter(SimulationKpis::saturated).count() * 2 >= runs.size();
        // El día de saturación solo tiene sentido en las corridas que efectivamente saturaron.
        List<Double> dias = runs.stream()
                .map(SimulationKpis::saturationDay).filter(Objects::nonNull)
                .map(Double::valueOf).toList();

        return new SimulationKpis(
                medianOf(runs, SimulationKpis::r0),
                dias.isEmpty() ? null : (int) Math.round(median(dias)),
                saturated,
                medianOf(runs, SimulationKpis::peakPressureRatio),
                medianIntOf(runs, SimulationKpis::peakPressureDay),
                medianOf(runs, SimulationKpis::maxBedDeficit),
                medianOf(runs, SimulationKpis::patientsNeverGotBed),
                medianOf(runs, SimulationKpis::deathsFromSaturation),
                medianOf(runs, SimulationKpis::avgWaitDays),
                medianOf(runs, SimulationKpis::finalAttackRate),
                medianOf(runs, SimulationKpis::totalHospitalizations)
        );
    }

    private double medianOf(List<SimulationKpis> runs, ToDoubleFunction<SimulationKpis> field) {
        return median(runs.stream().map(k -> field.applyAsDouble(k)).toList());
    }

    private Integer medianIntOf(List<SimulationKpis> runs, Function<SimulationKpis, Integer> field) {
        List<Double> valores = runs.stream().map(field).filter(Objects::nonNull)
                .map(Double::valueOf).toList();
        return valores.isEmpty() ? null : (int) Math.round(median(valores));
    }

    private double median(List<Double> valores) {
        if (valores.isEmpty()) return 0.0;
        double[] ordenados = valores.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        return percentile(ordenados, 50);
    }

    public CompareResult compare(CompareRequest request) {
        List<CompareResult.NamedResult> out = new ArrayList<>();
        if (request != null && request.scenarios() != null) {
            for (CompareRequest.NamedScenario ns : request.scenarios()) {
                out.add(new CompareResult.NamedResult(ns.name(), run(ns.request())));
            }
        }
        return new CompareResult(out);
    }

    public SimulationResult find(String id) {
        return store.find(id).orElseThrow(() -> new NoSuchElementException(
                "La corrida " + id + " ya no está disponible: solo se conservan en memoria "
                        + "las más recientes. Vuelve a ejecutar la simulación."));
    }

    public String exportCsv(String id) {
        SimulationResult result = find(id);
        StringBuilder sb = new StringBuilder();
        sb.append("day,sh,eh,ih,rh,sv,ev,iv,newCases,bedsOccupied,queueLength,capacity,")
          .append("admissions,discharges,deaths,cumulativeDeaths,leftWithoutBed,bedDemand,")
          .append("occupancyRatio,pressureRatio,rEffective,offeredLoad,rho,probWait,waitDaysQueue\n");
        for (DailyState d : result.series()) {
            sb.append(d.day()).append(',')
              .append(d.sh()).append(',').append(d.eh()).append(',').append(d.ih()).append(',').append(d.rh()).append(',')
              .append(d.sv()).append(',').append(d.ev()).append(',').append(d.iv()).append(',')
              .append(d.newCases()).append(',')
              .append(d.bedsOccupied()).append(',').append(d.queueLength()).append(',').append(d.capacity()).append(',')
              .append(d.admissions()).append(',').append(d.discharges()).append(',')
              .append(d.deaths()).append(',').append(d.cumulativeDeaths()).append(',')
              .append(d.leftWithoutBed()).append(',')
              .append(d.bedDemand()).append(',').append(d.occupancyRatio()).append(',')
              .append(d.pressureRatio()).append(',').append(d.rEffective()).append(',')
              .append(d.offeredLoad()).append(',').append(d.rho()).append(',')
              .append(d.probWait()).append(',').append(d.waitDaysQueue()).append('\n');
        }
        return sb.toString();
    }


    /** Guarda la ficha estadística de la corrida */
    private void capture(SimulationRequest req, SimulationResult result, long seed, UUID scenarioId) {
        try {
            boolean stochastic = Boolean.TRUE.equals(req.stochastic());
            SimulationKpis k = result.kpis();
            SimulationRunEntity run = new SimulationRunEntity();
            run.setScenarioId(scenarioId);
            run.setSeed(stochastic ? seed : null);
            run.setStochastic(stochastic);
            run.setR0(k.r0());
            run.setSaturationDay(k.saturationDay());
            run.setSaturated(k.saturated());
            run.setPeakPressureRatio(k.peakPressureRatio());
            run.setPeakPressureDay(k.peakPressureDay());
            run.setMaxBedDeficit(k.maxBedDeficit());
            run.setPatientsNeverGotBed(k.patientsNeverGotBed());
            run.setDeathsFromSaturation(k.deathsFromSaturation());
            run.setAvgWaitDays(k.avgWaitDays());
            run.setFinalAttackRate(k.finalAttackRate());
            run.setTotalHospitalizations(k.totalHospitalizations());
            runRepository.save(run);
        } catch (Exception ex) {
            log.warn("No se pudo capturar la corrida en la base de datos: {}", ex.getMessage());
        }
    }

    private List<BandPoint> toBands(double[][] sample, int horizon) {
        List<BandPoint> band = new ArrayList<>(horizon + 1);
        int runs = sample.length;
        double[] col = new double[runs];
        for (int t = 0; t <= horizon; t++) {
            for (int r = 0; r < runs; r++) col[r] = sample[r][t];
            double[] sorted = col.clone();
            Arrays.sort(sorted);
            band.add(new BandPoint(t, percentile(sorted, 5), percentile(sorted, 50), percentile(sorted, 95)));
        }
        return band;
    }

    private double percentile(double[] sorted, double p) {
        if (sorted.length == 0) return 0;
        double rank = (p / 100.0) * (sorted.length - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        if (lo == hi) return sorted[lo];
        double frac = rank - lo;
        return sorted[lo] * (1 - frac) + sorted[hi] * frac;
    }

    private void validate(SimulationRequest req) {
        ModelParameters m = req.model();
        HospitalParameters h = req.hospital();

        if (m.populationHuman() <= 0)
            throw new IllegalArgumentException("La población humana debe ser mayor que 0.");
        if (m.initialInfectedHumans() < 0 || m.initialInfectedHumans() > m.populationHuman())
            throw new IllegalArgumentException(
                    "Los infectados iniciales deben estar entre 0 y la población total ("
                            + Math.round(m.populationHuman()) + ").");
        if (req.horizonDays() <= 0 || req.horizonDays() > 3650)
            throw new IllegalArgumentException("El horizonte debe estar entre 1 y 3650 días.");
        if (h.avgStayDays() <= 0)
            throw new IllegalArgumentException("La estancia media debe ser mayor que 0.");
        if (h.maxWaitDays() <= h.deteriorationThresholdDays())
            throw new IllegalArgumentException(
                    "La espera máxima (" + h.maxWaitDays() + " días) debe superar al umbral de "
                            + "deterioro (" + h.deteriorationThresholdDays() + " días): si no, "
                            + "ningún paciente llega a agravarse antes de salir de la cola.");

        // Una intervención posterior al horizonte no se llega a aplicar nunca. Sin esta
        // comprobación la corrida sale como si no existiera, sin que nada lo indique.
        for (Intervention iv : req.interventions()) {
            if (iv.startDay() != null && iv.startDay() > req.horizonDays())
                throw new IllegalArgumentException(
                        "La intervención del día " + iv.startDay() + " queda fuera del horizonte de "
                                + req.horizonDays() + " días, así que nunca se aplicaría.");
        }
    }
}
