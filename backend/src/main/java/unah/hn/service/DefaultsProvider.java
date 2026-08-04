package unah.hn.service;

import unah.hn.dto.HospitalParameters;
import unah.hn.dto.ModelParameters;
import unah.hn.dto.SimulationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;


@Component
public class DefaultsProvider {

    public ModelParameters defaultModel() {
        return new ModelParameters(
                1_342_329.0, // populationHuman
                1.5,         // vectorRatio
                0.65,        // bitingRate
                0.40,        // betaHuman
                0.40,        // betaVector
                5.0,         // incubationHumanDays
                10.0,        // incubationVectorDays
                6.0,         // infectiousHumanDays
                70.0 * 365,  // lifeExpectancyDaysHuman
                14.0,        // mosquitoLifespanDays
                10.0,        // initialInfectedHumans
                0.45         // initialImmuneFraction
        );
    }


    public HospitalParameters defaultHospital() {
        return new HospitalParameters(
                150.0, // initialBeds: 0,11 por 1.000 hab.
                0.005, // hospitalizedFraction, sobre TODAS las infecciones: 500.000/390M = 0,13 %
                       //   de graves [1][2], por 3,7 al sumar los ingresos por signos de alarma
                4.0,   // admissionDelayDays
                6.0,   // avgStayDays: extremo bajo del rango 5-13 días [4]
                2.0,   // deteriorationThresholdDays
                0.013, // graveMortalityPerDay: ~15 % de letalidad tras 12 días de espera
                14.0,  // maxWaitDays
                0.27   // severeFraction: 13,4 % graves sobre el 49,9 % que requiere cama
        );
    }

    public int defaultHorizon() {
        return 365;
    }

    /** Completa los campos nulos de la petición con los valores por defecto. */
    public SimulationRequest normalize(SimulationRequest req) {
        SimulationRequest r = req != null ? req : new SimulationRequest(null, null, null, null, null, null);
        return new SimulationRequest(
                mergeModel(r.model()),
                mergeHospital(r.hospital()),
                r.interventions() != null ? r.interventions() : new ArrayList<>(),
                r.horizonDays() != null ? r.horizonDays() : defaultHorizon(),
                r.seed(),
                r.stochastic() != null ? r.stochastic() : Boolean.FALSE
        );
    }

    private ModelParameters mergeModel(ModelParameters m) {
        ModelParameters d = defaultModel();
        if (m == null) return d;
        return new ModelParameters(
                or(m.populationHuman(), d.populationHuman()),
                or(m.vectorRatio(), d.vectorRatio()),
                or(m.bitingRate(), d.bitingRate()),
                or(m.betaHuman(), d.betaHuman()),
                or(m.betaVector(), d.betaVector()),
                or(m.incubationHumanDays(), d.incubationHumanDays()),
                or(m.incubationVectorDays(), d.incubationVectorDays()),
                or(m.infectiousHumanDays(), d.infectiousHumanDays()),
                or(m.lifeExpectancyDaysHuman(), d.lifeExpectancyDaysHuman()),
                or(m.mosquitoLifespanDays(), d.mosquitoLifespanDays()),
                or(m.initialInfectedHumans(), d.initialInfectedHumans()),
                or(m.initialImmuneFraction(), d.initialImmuneFraction())
        );
    }

    private HospitalParameters mergeHospital(HospitalParameters h) {
        HospitalParameters d = defaultHospital();
        if (h == null) return d;
        return new HospitalParameters(
                or(h.initialBeds(), d.initialBeds()),
                or(h.hospitalizedFraction(), d.hospitalizedFraction()),
                or(h.admissionDelayDays(), d.admissionDelayDays()),
                or(h.avgStayDays(), d.avgStayDays()),
                or(h.deteriorationThresholdDays(), d.deteriorationThresholdDays()),
                or(h.graveMortalityPerDay(), d.graveMortalityPerDay()),
                or(h.maxWaitDays(), d.maxWaitDays()),
                or(h.severeFraction(), d.severeFraction())
        );
    }

    private Double or(Double value, Double fallback) {
        return value != null ? value : fallback;
    }

    public SimulationRequest defaultRequest() {
        return new SimulationRequest(defaultModel(), defaultHospital(),
                new ArrayList<>(), defaultHorizon(), null, Boolean.FALSE);
    }
}
