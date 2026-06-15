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

/**
 * Read-only view model representing a single upcoming visit, flattening the {@link Owner}
 * &rarr; {@link Pet} &rarr; {@link Visit} relationship into the fields the Upcoming
 * Visits page needs to display. Populated directly from a JPQL constructor expression in
 * {@link VisitRepository#findUpcomingVisits}.
 */
public class UpcomingVisit {

	private final Integer ownerId;

	private final String ownerName;

	private final String petName;

	private final LocalDate date;

	private final String description;

	/**
	 * Create an upcoming-visit view model. The owner's first and last name are supplied
	 * separately (matching the JPQL projection) and combined into a single display name.
	 * @param ownerId the owning owner's id, used for linking to the owner detail page
	 * @param ownerFirstName the owner's first name
	 * @param ownerLastName the owner's last name
	 * @param petName the visiting pet's name
	 * @param date the visit date
	 * @param description the visit description
	 */
	public UpcomingVisit(Integer ownerId, String ownerFirstName, String ownerLastName, String petName, LocalDate date,
			String description) {
		this.ownerId = ownerId;
		this.ownerName = ownerFirstName + " " + ownerLastName;
		this.petName = petName;
		this.date = date;
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

	public LocalDate getDate() {
		return this.date;
	}

	public String getDescription() {
		return this.description;
	}

}
