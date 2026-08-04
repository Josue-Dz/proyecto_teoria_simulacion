package unah.hn.repository;

import unah.hn.dto.SimulationResult;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class SimulationStore {

    static final int CAPACITY = 50;

    private final Map<String, SimulationResult> data = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SimulationResult> eldest) {
                    return size() > CAPACITY;
                }
            });

    public void save(SimulationResult result) {
        data.put(result.id(), result);
    }

    public Optional<SimulationResult> find(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        data.clear();
    }
}
