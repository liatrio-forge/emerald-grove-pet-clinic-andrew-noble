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

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure overlap rule in {@link AppointmentConflictDetector}.
 * Appointments are a fixed 30 minutes; the half-open rule means back-to-back slots do not
 * conflict.
 */
class AppointmentConflictDetectorTests {

	private final AppointmentConflictDetector detector = new AppointmentConflictDetector(null);

	@Test
	void identicalStartTimesConflict() {
		assertThat(this.detector.overlaps(LocalTime.of(9, 0), LocalTime.of(9, 0))).isTrue();
	}

	@Test
	void partialOverlapConflicts() {
		// 09:00-09:30 vs 09:15-09:45
		assertThat(this.detector.overlaps(LocalTime.of(9, 0), LocalTime.of(9, 15))).isTrue();
	}

	@Test
	void partialOverlapConflictsRegardlessOfOrder() {
		// 09:15-09:45 vs 09:00-09:30 (arguments swapped)
		assertThat(this.detector.overlaps(LocalTime.of(9, 15), LocalTime.of(9, 0))).isTrue();
	}

	@Test
	void backToBackAppointmentsDoNotConflict() {
		// 09:00-09:30 immediately followed by 09:30-10:00
		assertThat(this.detector.overlaps(LocalTime.of(9, 0), LocalTime.of(9, 30))).isFalse();
	}

	@Test
	void backToBackAppointmentsDoNotConflictReversed() {
		// 09:30-10:00 vs 09:00-09:30 (arguments swapped) exercises the first && branch
		assertThat(this.detector.overlaps(LocalTime.of(9, 30), LocalTime.of(9, 0))).isFalse();
	}

	@Test
	void fullySeparateAppointmentsDoNotConflict() {
		assertThat(this.detector.overlaps(LocalTime.of(9, 0), LocalTime.of(11, 0))).isFalse();
	}

}
