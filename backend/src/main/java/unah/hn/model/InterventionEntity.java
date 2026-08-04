package unah.hn.model;

import unah.hn.dto.InterventionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "intervention")
public class InterventionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ScenarioEntity scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterventionType type;

    @Column(name = "start_day", nullable = false)
    private Integer startDay;

    @Column(nullable = false)
    private Double magnitude;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ScenarioEntity getScenario() { return scenario; }
    public void setScenario(ScenarioEntity scenario) { this.scenario = scenario; }
    public InterventionType getType() { return type; }
    public void setType(InterventionType type) { this.type = type; }
    public Integer getStartDay() { return startDay; }
    public void setStartDay(Integer startDay) { this.startDay = startDay; }
    public Double getMagnitude() { return magnitude; }
    public void setMagnitude(Double magnitude) { this.magnitude = magnitude; }
}
