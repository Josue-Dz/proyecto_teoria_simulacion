package unah.hn.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulation_run")
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scenario_id")
    private UUID scenarioId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private Long seed;

    @Column(nullable = false)
    private Boolean stochastic;

    private Double r0;

    @Column(name = "saturation_day")
    private Integer saturationDay;

    private Boolean saturated;

    @Column(name = "peak_pressure_ratio")
    private Double peakPressureRatio;

    @Column(name = "peak_pressure_day")
    private Integer peakPressureDay;

    @Column(name = "max_bed_deficit")
    private Double maxBedDeficit;

    @Column(name = "patients_never_got_bed")
    private Double patientsNeverGotBed;

    @Column(name = "deaths_from_saturation")
    private Double deathsFromSaturation;

    @Column(name = "avg_wait_days")
    private Double avgWaitDays;

    @Column(name = "final_attack_rate")
    private Double finalAttackRate;

    @Column(name = "total_hospitalizations")
    private Double totalHospitalizations;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getScenarioId() { return scenarioId; }
    public void setScenarioId(UUID scenarioId) { this.scenarioId = scenarioId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }
    public Boolean getStochastic() { return stochastic; }
    public void setStochastic(Boolean stochastic) { this.stochastic = stochastic; }
    public Double getR0() { return r0; }
    public void setR0(Double r0) { this.r0 = r0; }
    public Integer getSaturationDay() { return saturationDay; }
    public void setSaturationDay(Integer saturationDay) { this.saturationDay = saturationDay; }
    public Boolean getSaturated() { return saturated; }
    public void setSaturated(Boolean saturated) { this.saturated = saturated; }
    public Double getPeakPressureRatio() { return peakPressureRatio; }
    public void setPeakPressureRatio(Double peakPressureRatio) { this.peakPressureRatio = peakPressureRatio; }
    public Integer getPeakPressureDay() { return peakPressureDay; }
    public void setPeakPressureDay(Integer peakPressureDay) { this.peakPressureDay = peakPressureDay; }
    public Double getMaxBedDeficit() { return maxBedDeficit; }
    public void setMaxBedDeficit(Double maxBedDeficit) { this.maxBedDeficit = maxBedDeficit; }
    public Double getPatientsNeverGotBed() { return patientsNeverGotBed; }
    public void setPatientsNeverGotBed(Double patientsNeverGotBed) { this.patientsNeverGotBed = patientsNeverGotBed; }
    public Double getDeathsFromSaturation() { return deathsFromSaturation; }
    public void setDeathsFromSaturation(Double deathsFromSaturation) { this.deathsFromSaturation = deathsFromSaturation; }
    public Double getAvgWaitDays() { return avgWaitDays; }
    public void setAvgWaitDays(Double avgWaitDays) { this.avgWaitDays = avgWaitDays; }
    public Double getFinalAttackRate() { return finalAttackRate; }
    public void setFinalAttackRate(Double finalAttackRate) { this.finalAttackRate = finalAttackRate; }
    public Double getTotalHospitalizations() { return totalHospitalizations; }
    public void setTotalHospitalizations(Double totalHospitalizations) { this.totalHospitalizations = totalHospitalizations; }
}
