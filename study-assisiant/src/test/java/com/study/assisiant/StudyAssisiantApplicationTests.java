package com.study.assisiant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:study_context;"
				+ "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=always",
		"jwt.secret=UnitTestSecretKeyForJwtSecurity2026!!",
		"app.upload-dir=${java.io.tmpdir}/study-assisiant-context-test"
})
class StudyAssisiantApplicationTests {

	@Test
	void contextLoads() {
	}

}
