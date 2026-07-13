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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentBookingService {

	private final OwnerRepository owners;

	private final AppointmentConflictDetector conflictDetector;

	public AppointmentBookingService(OwnerRepository owners, AppointmentConflictDetector conflictDetector) {
		this.owners = owners;
		this.conflictDetector = conflictDetector;
	}

	@Transactional
	public boolean bookNewVisit(Owner owner, int petId, Visit visit) {
		this.conflictDetector.lockAppointmentResources(visit.getVet(), petId);
		boolean conflict = this.conflictDetector.hasVetConflict(visit.getVet(), visit.getDate(), visit.getStartTime())
				|| this.conflictDetector.hasPetConflict(petId, visit.getDate(), visit.getStartTime());
		if (conflict) {
			return false;
		}

		Owner bookingOwner = this.owners.findById(owner.getId()).orElse(owner);
		bookingOwner.addVisit(petId, visit);
		this.owners.save(bookingOwner);
		return true;
	}

}
