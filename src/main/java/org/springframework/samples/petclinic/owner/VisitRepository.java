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
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.vet.Vet;

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
	 * Lock a veterinarian row before checking and writing appointment conflicts, so
	 * concurrent bookings for the same vet serialize even when there are no existing
	 * visits to lock.
	 * @param vetId the veterinarian's id
	 * @return the locked veterinarian, empty if none exists
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT vet FROM Vet vet WHERE vet.id = :vetId")
	Optional<Vet> lockVetById(@Param("vetId") int vetId);

	/**
	 * Lock a pet row before checking and writing appointment conflicts, so concurrent
	 * bookings for the same pet serialize even when there are no existing visits to lock.
	 * @param petId the pet's id
	 * @return the locked pet, empty if none exists
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT p FROM Owner o JOIN o.pets p
			WHERE p.id = :petId
			""")
	Optional<Pet> lockPetById(@Param("petId") int petId);

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

	/**
	 * Retrieve every appointment on the given date across the whole clinic, projected
	 * into {@link ScheduledAppointment} view models and ordered by start time then vet
	 * last name so the day reads top-to-bottom chronologically. Uses a {@code LEFT JOIN}
	 * on the vet so legacy appointments with no assigned vet are still included.
	 * <p>
	 * Because the {@code LEFT JOIN} leaves {@code vet.lastName} null for legacy
	 * appointments with no assigned vet, pet name and owner last name are appended as
	 * further tiebreakers so the ordering stays deterministic regardless of how a given
	 * dialect sorts nulls or ties on the earlier keys.
	 * @param date the day to list
	 * @return the day's appointments, empty if none
	 */
	@Query("""
			SELECT new org.springframework.samples.petclinic.owner.ScheduledAppointment(
				o.id, o.firstName, o.lastName, p.name, vet.firstName, vet.lastName, v.date, v.startTime, v.description)
			FROM Owner o JOIN o.pets p JOIN p.visits v LEFT JOIN v.vet vet
			WHERE v.date = :date
			ORDER BY v.startTime, vet.lastName, p.name, o.lastName
			""")
	List<ScheduledAppointment> findScheduledAppointmentsByDate(@Param("date") LocalDate date);

}
