package unah.hn.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class ModelParamsEmbeddable {

    private Double populationHuman;
    private Double vectorRatio;
    private Double bitingRate;
    private Double betaHuman;
    private Double betaVector;
    private Double incubationHumanDays;
    private Double incubationVectorDays;
    private Double infectiousHumanDays;
    private Double lifeExpectancyDaysHuman;
    private Double mosquitoLifespanDays;
    private Double initialInfectedHumans;
    private Double initialImmuneFraction;

    public Double getPopulationHuman() { return populationHuman; }
    public void setPopulationHuman(Double v) { this.populationHuman = v; }
    public Double getVectorRatio() { return vectorRatio; }
    public void setVectorRatio(Double v) { this.vectorRatio = v; }
    public Double getBitingRate() { return bitingRate; }
    public void setBitingRate(Double v) { this.bitingRate = v; }
    public Double getBetaHuman() { return betaHuman; }
    public void setBetaHuman(Double v) { this.betaHuman = v; }
    public Double getBetaVector() { return betaVector; }
    public void setBetaVector(Double v) { this.betaVector = v; }
    public Double getIncubationHumanDays() { return incubationHumanDays; }
    public void setIncubationHumanDays(Double v) { this.incubationHumanDays = v; }
    public Double getIncubationVectorDays() { return incubationVectorDays; }
    public void setIncubationVectorDays(Double v) { this.incubationVectorDays = v; }
    public Double getInfectiousHumanDays() { return infectiousHumanDays; }
    public void setInfectiousHumanDays(Double v) { this.infectiousHumanDays = v; }
    public Double getLifeExpectancyDaysHuman() { return lifeExpectancyDaysHuman; }
    public void setLifeExpectancyDaysHuman(Double v) { this.lifeExpectancyDaysHuman = v; }
    public Double getMosquitoLifespanDays() { return mosquitoLifespanDays; }
    public void setMosquitoLifespanDays(Double v) { this.mosquitoLifespanDays = v; }
    public Double getInitialInfectedHumans() { return initialInfectedHumans; }
    public void setInitialInfectedHumans(Double v) { this.initialInfectedHumans = v; }
    public Double getInitialImmuneFraction() { return initialImmuneFraction; }
    public void setInitialImmuneFraction(Double v) { this.initialImmuneFraction = v; }
}
