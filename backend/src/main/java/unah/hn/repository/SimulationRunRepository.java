package unah.hn.repository;

import unah.hn.model.SimulationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, UUID> {

    List<SimulationRunEntity> findByScenarioIdOrderByCreatedAtDesc(UUID scenarioId);

    List<SimulationRunEntity> findAllByOrderByCreatedAtDesc();
}
