package unah.hn.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scenario")
public class ScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "horizon_days", nullable = false)
    private Integer horizonDays;

    private Long seed;

    @Column(nullable = false)
    private Boolean stochastic = Boolean.FALSE;

    @Embedded
    private ModelParamsEmbeddable model = new ModelParamsEmbeddable();

    @Embedded
    private HospitalParamsEmbeddable hospital = new HospitalParamsEmbeddable();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InterventionEntity> interventions = new ArrayList<>();

    public void addIntervention(InterventionEntity iv) {
        iv.setScenario(this);
        this.interventions.add(iv);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getHorizonDays() { return horizonDays; }
    public void setHorizonDays(Integer horizonDays) { this.horizonDays = horizonDays; }
    public Long getSeed() { return seed; }
    public void setSeed(Long seed) { this.seed = seed; }
    public Boolean getStochastic() { return stochastic; }
    public void setStochastic(Boolean stochastic) { this.stochastic = stochastic; }
    public ModelParamsEmbeddable getModel() { return model; }
    public void setModel(ModelParamsEmbeddable model) { this.model = model; }
    public HospitalParamsEmbeddable getHospital() { return hospital; }
    public void setHospital(HospitalParamsEmbeddable hospital) { this.hospital = hospital; }
    public List<InterventionEntity> getInterventions() { return interventions; }
    public void setInterventions(List<InterventionEntity> interventions) { this.interventions = interventions; }
}
