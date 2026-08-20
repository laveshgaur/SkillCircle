package com.skillcircle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full application context load test.
 * Requires PostgreSQL and Redis to be running.
 * Only runs when INTEGRATION_TESTS=true environment variable is set.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class SkillCircleApplicationTests {

	@Test
	void contextLoads() {
	}

}
