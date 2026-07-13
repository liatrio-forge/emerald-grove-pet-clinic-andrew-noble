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

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.stereotype.Component;

/**
 * Detects scheduling conflicts for new appointments. Every appointment occupies a fixed
 * {@link Visit#APPOINTMENT_DURATION} slot; two appointments on the same date conflict
 * when their half-open intervals {@code [start, start + duration)} intersect, expressed
 * as {@code startA < endB && startB < endA}. Back-to-back appointments (one ending
 * exactly as the next begins) therefore do <em>not</em> conflict.
 * <p>
 * The pure {@link #overlaps(LocalTime, LocalTime)} rule is the critical business logic
 * and is unit-tested to full branch coverage; the repository-backed checks apply it to
 * the appointments already booked for a given vet or pet on a given date.
 */
@Component
public class AppointmentConflictDetector {

	private final VisitRepository visits;

	public AppointmentConflictDetector(VisitRepository visits) {
		this.visits = visits;
	}

	/**
	 * Whether two fixed-duration appointments starting at the given times overlap.
	 * @param startA the start time of the first appointment
	 * @param startB the start time of the second appointment
	 * @return {@code true} if their {@code [start, end)} intervals intersect
	 */
	public boolean overlaps(LocalTime startA, LocalTime startB) {
		LocalTime endA = startA.plus(Visit.APPOINTMENT_DURATION);
		LocalTime endB = startB.plus(Visit.APPOINTMENT_DURATION);
		return startA.isBefore(endB) && startB.isBefore(endA);
	}

	/**
	 * Lock the resources whose schedules are about to be checked and updated.
	 * @param vet the veterinarian being booked, may be {@code null}
	 * @param petId the pet being booked
	 */
	public void lockAppointmentResources(Vet vet, int petId) {
		if (vet != null && vet.getId() != null) {
			this.visits.lockVetById(vet.getId());
		}
		this.visits.lockPetById(petId);
	}

	/**
	 * Whether booking the given vet at the given date/time would overlap an existing
	 * appointment for that same vet. No conflict is reported when no vet is assigned
	 * (legacy null-vet rows never produce a vet conflict).
	 * @param vet the veterinarian being booked, may be {@code null}
	 * @param date the appointment date
	 * @param startTime the proposed start time
	 * @return {@code true} if the vet is already booked in an overlapping slot
	 */
	public boolean hasVetConflict(Vet vet, LocalDate date, LocalTime startTime) {
		if (vet == null || vet.getId() == null || date == null || startTime == null) {
			return false;
		}
		for (Visit existing : this.visits.findByVetAndDate(vet.getId(), date)) {
			if (existing.getStartTime() != null && overlaps(startTime, existing.getStartTime())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether booking the given pet at the given date/time would overlap an existing
	 * appointment for that same pet.
	 * @param petId the pet being booked
	 * @param date the appointment date
	 * @param startTime the proposed start time
	 * @return {@code true} if the pet is already booked in an overlapping slot
	 */
	public boolean hasPetConflict(int petId, LocalDate date, LocalTime startTime) {
		if (date == null || startTime == null) {
			return false;
		}
		for (Visit existing : this.visits.findByPetAndDate(petId, date)) {
			if (existing.getStartTime() != null && overlaps(startTime, existing.getStartTime())) {
				return true;
			}
		}
		return false;
	}

}
