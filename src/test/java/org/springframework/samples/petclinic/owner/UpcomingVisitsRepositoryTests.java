/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Integration tests for the upcoming-visits query on {@link VisitRepository}.
 * <p>
 * Uses the seeded H2 sample data, whose visits are dated in January 2013:
 * <ul>
 * <li>Samantha (Jean Coleman) — 2013-01-01 "rabies shot", 2013-01-04 "spayed"</li>
 * <li>Max (Jean Coleman) — 2013-01-02 "rabies shot", 2013-01-03 "neutered"</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UpcomingVisitsRepositoryTests {

	@Autowired
	private VisitRepository visits;

	@Test
	void shouldReturnVisitsWithinInclusiveWindowOrderedByDate() {
		List<UpcomingVisit> result = this.visits.findUpcomingVisits(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 2));

		assertThat(result).hasSize(2);
		assertThat(result).extracting(UpcomingVisit::getDate)
			.containsExactly(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 2));

		UpcomingVisit first = result.get(0);
		assertThat(first.getOwnerName()).isEqualTo("Jean Coleman");
		assertThat(first.getPetName()).isEqualTo("Samantha");
		assertThat(first.getDescription()).isEqualTo("rabies shot");
	}

	@Test
	void shouldIncludeVisitsOnTheWindowBoundaries() {
		List<UpcomingVisit> result = this.visits.findUpcomingVisits(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 4));

		assertThat(result).extracting(UpcomingVisit::getDate)
			.containsExactly(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 1, 2), LocalDate.of(2013, 1, 3),
					LocalDate.of(2013, 1, 4));
	}

	@Test
	void shouldExcludeVisitsOutsideTheWindow() {
		List<UpcomingVisit> result = this.visits.findUpcomingVisits(LocalDate.of(2013, 1, 2), LocalDate.of(2013, 1, 3));

		assertThat(result).extracting(UpcomingVisit::getDate)
			.containsExactly(LocalDate.of(2013, 1, 2), LocalDate.of(2013, 1, 3));
		assertThat(result).extracting(UpcomingVisit::getDescription).containsExactly("rabies shot", "neutered");
	}

	@Test
	void shouldReturnEmptyListWhenNoVisitsInWindow() {
		List<UpcomingVisit> result = this.visits.findUpcomingVisits(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2));

		assertThat(result).isEmpty();
	}

}
