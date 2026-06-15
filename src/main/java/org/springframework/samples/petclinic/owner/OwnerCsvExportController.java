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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Exports owner search results as a CSV file. Reuses the same optional search criteria as
 * the HTML owners search ({@code lastName}, {@code city}, {@code telephone}, combined
 * with AND) and returns every matching owner (unpaged) as a downloadable {@code text/csv}
 * response.
 *
 * @author Andrew Noble
 */
@Controller
class OwnerCsvExportController {

	private final OwnerRepository owners;

	OwnerCsvExportController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping(path = "/owners.csv", produces = "text/csv")
	@ResponseBody
	String exportOwnersCsv(@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "") String city, @RequestParam(defaultValue = "") String telephone) {
		Page<Owner> matches = this.owners.findByOptionalCriteria(lastName, city, telephone, Pageable.unpaged());

		StringBuilder csv = new StringBuilder("firstName,lastName,address,city,telephone");
		for (Owner owner : matches) {
			csv.append('\n')
				.append(owner.getFirstName())
				.append(',')
				.append(owner.getLastName())
				.append(',')
				.append(owner.getAddress())
				.append(',')
				.append(owner.getCity())
				.append(',')
				.append(owner.getTelephone());
		}
		return csv.toString();
	}

}
