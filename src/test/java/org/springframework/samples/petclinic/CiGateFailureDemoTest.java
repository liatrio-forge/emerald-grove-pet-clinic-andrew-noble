package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEMPORARY test used only to demonstrate that the CI gate fails a PR when a test
 * fails. This file is created on a throwaway branch and is NOT merged. See Spec 13,
 * Task 1.5.
 */
class CiGateFailureDemoTest {

	@Test
	void deliberatelyFailingTestToProveCiGateBlocksMerge() {
		assertThat(1 + 1).isEqualTo(3);
	}

}
