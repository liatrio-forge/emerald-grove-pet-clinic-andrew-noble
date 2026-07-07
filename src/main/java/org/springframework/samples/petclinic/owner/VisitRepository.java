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
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repository for read-only queries over {@link Visit} data.
 * <p>
 * Because {@link Visit} has no association back to its {@link Pet} or {@link Owner}, the
 * upcoming-visits query is rooted at {@link Owner} and traverses the
 * {@code owner -> pets -> visits} associations, projecting each match into an
 * {@link UpcomingVisit} view model via a JPQL constructor expression.
 */
public interface VisitRepository extends Repository<Visit, Integer> {

	/**
	 * Retrieve all visits whose date falls within the given window (inclusive on both
	 * ends), ordered by date ascending and then by owner last name and pet name for
	 * stable output.
	 * @param start the first date of the window (inclusive)
	 * @param end the last date of the window (inclusive)
	 * @return a list of {@link UpcomingVisit} view models, empty if none match
	 */
	@Query("""
			SELECT new org.springframework.samples.petclinic.owner.UpcomingVisit(
				o.id, o.firstName, o.lastName, p.name, v.date, v.description)
			FROM Owner o JOIN o.pets p JOIN p.visits v
			WHERE v.date BETWEEN :start AND :end
			ORDER BY v.date, o.lastName, p.name
			""")
	List<UpcomingVisit> findUpcomingVisits(@Param("start") LocalDate start, @Param("end") LocalDate end);

	/**
	 * Find the appointments booked for a given veterinarian on a given date. Legacy
	 * visits with no assigned vet are excluded (their {@code vet} is null), so they never
	 * appear as a vet conflict.
	 * @param vetId the veterinarian's id
	 * @param date the date to search
	 * @return the matching appointments, empty if none
	 */
	@Query("""
			SELECT v FROM Owner o JOIN o.pets p JOIN p.visits v
			WHERE v.vet.id = :vetId AND v.date = :date
			""")
	List<Visit> findByVetAndDate(@Param("vetId") int vetId, @Param("date") LocalDate date);

	/**
	 * Find the appointments booked for a given pet on a given date.
	 * @param petId the pet's id
	 * @param date the date to search
	 * @return the matching appointments, empty if none
	 */
	@Query("""
			SELECT v FROM Owner o JOIN o.pets p JOIN p.visits v
			WHERE p.id = :petId AND v.date = :date
			""")
	List<Visit> findByPetAndDate(@Param("petId") int petId, @Param("date") LocalDate date);

}
