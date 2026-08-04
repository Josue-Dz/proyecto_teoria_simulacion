package unah.hn.service;

import unah.hn.dto.ModelParameters;
import unah.hn.dto.SimulationKpis;
import unah.hn.engine.EpidemicOutput;
import unah.hn.engine.HospitalOutput;
import unah.hn.engine.SeirSeiEngine;
import org.springframework.stereotype.Component;

@Component
public class KpiCalculator {

    public SimulationKpis compute(EpidemicOutput epi, HospitalOutput hosp,
                                  ModelParameters model, int horizon) {

        double nH = model.populationHuman();

        Integer saturationDay = null;
        double peakPressure = 0.0;
        Integer peakDay = null;
        double maxDeficit = 0.0;

        for (int t = 1; t <= horizon; t++) {
            double cap = hosp.capacity()[t];
            double occ = hosp.occupied()[t];
            double q = hosp.queue()[t];

            if (saturationDay == null && q > 0.5) {
                saturationDay = t;
            }
            double pressure = cap > 0 ? (occ + q) / cap : 0.0;
            if (pressure > peakPressure) {
                peakPressure = pressure;
                peakDay = t;
            }
            if (q > maxDeficit) {
                maxDeficit = q;
            }
        }

        double deathsFromSaturation = Math.max(0.0, hosp.totalQueueDeaths());
        double patientsNeverGotBed = hosp.totalQueueDeaths()
                + hosp.totalLeftWithoutBed()
                + hosp.finalQueue();

        double shInitial = epi.states()[0][SeirSeiEngine.SH];
        double shFinal = epi.states()[horizon][SeirSeiEngine.SH];
        double attackRate = nH > 0 ? Math.max(0.0, (shInitial - shFinal) / nH) : 0.0;

        return new SimulationKpis(
                epi.r0(),
                saturationDay,
                saturationDay != null,
                peakPressure,
                peakDay,
                maxDeficit,
                patientsNeverGotBed,
                deathsFromSaturation,
                hosp.avgWaitDays(),
                attackRate,
                hosp.totalHospitalizations()
        );
    }
}
