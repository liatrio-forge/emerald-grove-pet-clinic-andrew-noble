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

/**
 * Read-only view model representing a single appointment on the clinic-wide day schedule,
 * flattening the {@link Owner} &rarr; {@link Pet} &rarr; {@link Visit} (and its
 * {@code Vet}) relationship into the fields the schedule page needs. Populated directly
 * from a JPQL constructor expression in
 * {@link VisitRepository#findScheduledAppointmentsByDate}.
 * <p>
 * Legacy appointments have no assigned vet; for those the vet name is an empty string.
 */
public class ScheduledAppointment {

	private final Integer ownerId;

	private final String ownerName;

	private final String petName;

	private final String vetName;

	private final LocalDate date;

	private final LocalTime startTime;

	private final String description;

	/**
	 * Create a scheduled-appointment view model. Owner and vet names are supplied as
	 * separate first/last parts (matching the JPQL projection) and combined for display.
	 * @param ownerId the owning owner's id, used for linking to the owner detail page
	 * @param ownerFirstName the owner's first name
	 * @param ownerLastName the owner's last name
	 * @param petName the pet's name
	 * @param vetFirstName the assigned vet's first name, may be {@code null} (legacy
	 * rows)
	 * @param vetLastName the assigned vet's last name, may be {@code null} (legacy rows)
	 * @param date the appointment date
	 * @param startTime the appointment start time
	 * @param description the appointment description
	 */
	public ScheduledAppointment(Integer ownerId, String ownerFirstName, String ownerLastName, String petName,
			String vetFirstName, String vetLastName, LocalDate date, LocalTime startTime, String description) {
		this.ownerId = ownerId;
		this.ownerName = ownerFirstName + " " + ownerLastName;
		this.petName = petName;
		this.vetName = ((vetFirstName == null ? "" : vetFirstName) + " " + (vetLastName == null ? "" : vetLastName))
			.trim();
		this.date = date;
		this.startTime = startTime;
		this.description = description;
	}

	public Integer getOwnerId() {
		return this.ownerId;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getPetName() {
		return this.petName;
	}

	public String getVetName() {
		return this.vetName;
	}

	public LocalDate getDate() {
		return this.date;
	}

	public LocalTime getStartTime() {
		return this.startTime;
	}

	public String getDescription() {
		return this.description;
	}

}
