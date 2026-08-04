package unah.hn.service;

import unah.hn.dto.HospitalParameters;
import unah.hn.dto.Intervention;
import unah.hn.dto.ModelParameters;
import unah.hn.dto.ScenarioDto;
import unah.hn.dto.SimulationRequest;
import unah.hn.model.HospitalParamsEmbeddable;
import unah.hn.model.InterventionEntity;
import unah.hn.model.ModelParamsEmbeddable;
import unah.hn.model.ScenarioEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScenarioMapper {

    public ScenarioEntity toEntity(String name, String description, SimulationRequest req) {
        ScenarioEntity e = new ScenarioEntity();
        e.setName(name);
        e.setDescription(description);
        e.setHorizonDays(req.horizonDays());
        e.setSeed(req.seed());
        e.setStochastic(req.stochastic());

        ModelParameters m = req.model();
        ModelParamsEmbeddable me = new ModelParamsEmbeddable();
        me.setPopulationHuman(m.populationHuman());
        me.setVectorRatio(m.vectorRatio());
        me.setBitingRate(m.bitingRate());
        me.setBetaHuman(m.betaHuman());
        me.setBetaVector(m.betaVector());
        me.setIncubationHumanDays(m.incubationHumanDays());
        me.setIncubationVectorDays(m.incubationVectorDays());
        me.setInfectiousHumanDays(m.infectiousHumanDays());
        me.setLifeExpectancyDaysHuman(m.lifeExpectancyDaysHuman());
        me.setMosquitoLifespanDays(m.mosquitoLifespanDays());
        me.setInitialInfectedHumans(m.initialInfectedHumans());
        me.setInitialImmuneFraction(m.initialImmuneFraction());
        e.setModel(me);

        HospitalParameters h = req.hospital();
        HospitalParamsEmbeddable he = new HospitalParamsEmbeddable();
        he.setInitialBeds(h.initialBeds());
        he.setHospitalizedFraction(h.hospitalizedFraction());
        he.setAdmissionDelayDays(h.admissionDelayDays());
        he.setAvgStayDays(h.avgStayDays());
        he.setDeteriorationThresholdDays(h.deteriorationThresholdDays());
        he.setGraveMortalityPerDay(h.graveMortalityPerDay());
        he.setMaxWaitDays(h.maxWaitDays());
        he.setSevereFraction(h.severeFraction());
        e.setHospital(he);

        if (req.interventions() != null) {
            for (Intervention iv : req.interventions()) {
                InterventionEntity ie = new InterventionEntity();
                ie.setType(iv.type());
                ie.setStartDay(iv.startDay());
                ie.setMagnitude(iv.magnitude());
                e.addIntervention(ie);
            }
        }
        return e;
    }

    public SimulationRequest toRequest(ScenarioEntity e) {
        ModelParamsEmbeddable me = e.getModel();
        ModelParameters model = new ModelParameters(
                me.getPopulationHuman(), me.getVectorRatio(), me.getBitingRate(),
                me.getBetaHuman(), me.getBetaVector(), me.getIncubationHumanDays(),
                me.getIncubationVectorDays(), me.getInfectiousHumanDays(),
                me.getLifeExpectancyDaysHuman(), me.getMosquitoLifespanDays(),
                me.getInitialInfectedHumans(), me.getInitialImmuneFraction());

        HospitalParamsEmbeddable he = e.getHospital();
        HospitalParameters hospital = new HospitalParameters(
                he.getInitialBeds(), he.getHospitalizedFraction(), he.getAdmissionDelayDays(),
                he.getAvgStayDays(), he.getDeteriorationThresholdDays(),
                he.getGraveMortalityPerDay(), he.getMaxWaitDays(), he.getSevereFraction());

        List<Intervention> interventions = new ArrayList<>();
        for (InterventionEntity ie : e.getInterventions()) {
            interventions.add(new Intervention(ie.getType(), ie.getStartDay(), ie.getMagnitude()));
        }

        return new SimulationRequest(model, hospital, interventions,
                e.getHorizonDays(), e.getSeed(), e.getStochastic());
    }

    public ScenarioDto toDto(ScenarioEntity e) {
        return new ScenarioDto(
                e.getId() != null ? e.getId().toString() : null,
                e.getName(),
                e.getDescription(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                toRequest(e)
        );
    }
}
