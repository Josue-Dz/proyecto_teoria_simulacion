package unah.hn.repository;

import unah.hn.model.ScenarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScenarioRepository extends JpaRepository<ScenarioEntity, UUID> {
}
