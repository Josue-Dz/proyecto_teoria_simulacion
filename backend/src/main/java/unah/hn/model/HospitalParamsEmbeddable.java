package unah.hn.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class HospitalParamsEmbeddable {

    private Double initialBeds;
    private Double hospitalizedFraction;
    private Double admissionDelayDays;
    private Double avgStayDays;
    private Double deteriorationThresholdDays;
    private Double graveMortalityPerDay;
    private Double maxWaitDays;
    private Double severeFraction;

    public Double getInitialBeds() { return initialBeds; }
    public void setInitialBeds(Double v) { this.initialBeds = v; }
    public Double getHospitalizedFraction() { return hospitalizedFraction; }
    public void setHospitalizedFraction(Double v) { this.hospitalizedFraction = v; }
    public Double getAdmissionDelayDays() { return admissionDelayDays; }
    public void setAdmissionDelayDays(Double v) { this.admissionDelayDays = v; }
    public Double getAvgStayDays() { return avgStayDays; }
    public void setAvgStayDays(Double v) { this.avgStayDays = v; }
    public Double getDeteriorationThresholdDays() { return deteriorationThresholdDays; }
    public void setDeteriorationThresholdDays(Double v) { this.deteriorationThresholdDays = v; }
    public Double getGraveMortalityPerDay() { return graveMortalityPerDay; }
    public void setGraveMortalityPerDay(Double v) { this.graveMortalityPerDay = v; }
    public Double getMaxWaitDays() { return maxWaitDays; }
    public void setMaxWaitDays(Double v) { this.maxWaitDays = v; }
    public Double getSevereFraction() { return severeFraction; }
    public void setSevereFraction(Double v) { this.severeFraction = v; }
}
