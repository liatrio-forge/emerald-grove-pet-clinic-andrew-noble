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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.transaction.annotation.Transactional;

class AppointmentBookingServiceTests {

	private final OwnerRepository owners = org.mockito.Mockito.mock(OwnerRepository.class);

	private final AppointmentConflictDetector conflictDetector = org.mockito.Mockito
		.mock(AppointmentConflictDetector.class);

	private final AppointmentBookingService bookingService = new AppointmentBookingService(this.owners,
			this.conflictDetector);

	@Test
	void bookNewVisitIsTransactional() throws Exception {
		Method method = AppointmentBookingService.class.getMethod("bookNewVisit", Owner.class, int.class, Visit.class);

		assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
	}

	@Test
	void locksAppointmentResourcesBeforeCheckingConflictsAndSaving() {
		Owner owner = newOwnerWithPet();
		Visit visit = newVisit();
		given(this.owners.findById(owner.getId())).willReturn(Optional.of(owner));

		boolean booked = this.bookingService.bookNewVisit(owner, 1, visit);

		assertThat(booked).isTrue();
		InOrder inOrder = inOrder(this.conflictDetector, this.owners);
		inOrder.verify(this.conflictDetector).lockAppointmentResources(visit.getVet(), 1);
		inOrder.verify(this.conflictDetector).hasVetConflict(visit.getVet(), visit.getDate(), visit.getStartTime());
		inOrder.verify(this.conflictDetector).hasPetConflict(1, visit.getDate(), visit.getStartTime());
		inOrder.verify(this.owners).save(owner);
	}

	private Owner newOwnerWithPet() {
		Owner owner = new Owner();
		owner.setId(1);
		Pet pet = new Pet();
		owner.addPet(pet);
		pet.setId(1);
		return owner;
	}

	private Visit newVisit() {
		Vet vet = new Vet();
		vet.setId(1);
		Visit visit = new Visit();
		visit.setVet(vet);
		visit.setDate(LocalDate.now().plusDays(1));
		visit.setStartTime(LocalTime.of(9, 0));
		visit.setDescription("Annual checkup");
		return visit;
	}

}
