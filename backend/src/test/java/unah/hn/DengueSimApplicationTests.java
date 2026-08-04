package unah.hn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Verifica que el contexto completo levanta (controladores, servicios, JPA). */
@SpringBootTest
@ActiveProfiles("test")
class DengueSimApplicationTests {

	@Test
	void contextLoads() {
	}

}
