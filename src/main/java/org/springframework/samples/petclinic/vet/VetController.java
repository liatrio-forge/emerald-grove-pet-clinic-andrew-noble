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
package org.springframework.samples.petclinic.vet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private static final int PAGE_SIZE = 5;

	/** Filter value (and selected-state marker) meaning "show every vet". */
	static final String ALL = "all";

	/** Filter value (and selected-state marker) meaning "vets with no specialty". */
	static final String NONE = "none";

	private final VetRepository vetRepository;

	public VetController(VetRepository vetRepository) {
		this.vetRepository = vetRepository;
	}

	@GetMapping("/vets.html")
	public String showVetList(@RequestParam(defaultValue = "1") int page,
			@RequestParam(name = "specialty", required = false) String specialty, Model model) {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for Object-Xml mapping
		int currentPage = Math.max(page, 1);
		List<Vet> allVets = new ArrayList<>(this.vetRepository.findAll());

		List<String> availableSpecialties = distinctSpecialtyNames(allVets);
		String selectedSpecialty = normalizeSelection(specialty, availableSpecialties);
		List<Vet> filteredVets = filterVets(allVets, specialty, availableSpecialties);

		Page<Vet> paginated = paginate(filteredVets, currentPage);
		Vets vets = new Vets();
		vets.getVetList().addAll(paginated.toList());
		model.addAttribute("specialties", availableSpecialties);
		model.addAttribute("selectedSpecialty", selectedSpecialty);
		return addPaginationModel(currentPage, paginated, model);
	}

	private String addPaginationModel(int page, Page<Vet> paginated, Model model) {
		List<Vet> listVets = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listVets", listVets);
		return "vets/vetList";
	}

	/**
	 * Distinct specialty names across all vets, sorted alphabetically. These drive the
	 * options offered by the specialty filter dropdown.
	 */
	private List<String> distinctSpecialtyNames(List<Vet> vets) {
		return vets.stream()
			.flatMap(vet -> vet.getSpecialties().stream())
			.map(Specialty::getName)
			.distinct()
			.sorted()
			.toList();
	}

	/**
	 * Resolve which dropdown option should appear selected. Empty/blank, "all", and any
	 * unrecognized value fall back to {@link #ALL}; "none" and known specialty names are
	 * preserved (normalized to the canonical specialty name).
	 */
	private String normalizeSelection(String specialty, List<String> availableSpecialties) {
		String value = specialty == null ? "" : specialty.trim();
		if (value.isEmpty() || value.equalsIgnoreCase(ALL)) {
			return ALL;
		}
		if (value.equalsIgnoreCase(NONE)) {
			return NONE;
		}
		return matchSpecialty(value, availableSpecialties).orElse(ALL);
	}

	/**
	 * Apply the specialty filter to the vet list. A known specialty name keeps only vets
	 * with that specialty; "none" keeps only vets with no specialties; empty/"all" keeps
	 * every vet; an unrecognized value yields an empty list (the dropdown falls back to
	 * "All" for display, but a bad/shared URL must not silently show all vets).
	 */
	private List<Vet> filterVets(List<Vet> vets, String specialty, List<String> availableSpecialties) {
		String value = specialty == null ? "" : specialty.trim();
		if (value.isEmpty() || value.equalsIgnoreCase(ALL)) {
			return vets;
		}
		if (value.equalsIgnoreCase(NONE)) {
			return vets.stream().filter(vet -> vet.getNrOfSpecialties() == 0).toList();
		}
		Optional<String> matched = matchSpecialty(value, availableSpecialties);
		if (matched.isEmpty()) {
			return Collections.emptyList();
		}
		String name = matched.get();
		return vets.stream()
			.filter(vet -> vet.getSpecialties().stream().anyMatch(s -> s.getName().equals(name)))
			.toList();
	}

	private Optional<String> matchSpecialty(String value, List<String> availableSpecialties) {
		return availableSpecialties.stream().filter(name -> name.equalsIgnoreCase(value)).findFirst();
	}

	/**
	 * Paginate an in-memory, already-filtered list so the existing pagination model
	 * (totalPages/totalItems) reflects the filtered result set.
	 */
	private Page<Vet> paginate(List<Vet> vets, int page) {
		Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
		int start = (int) pageable.getOffset();
		if (start >= vets.size()) {
			return new PageImpl<>(Collections.emptyList(), pageable, vets.size());
		}
		int end = Math.min(start + PAGE_SIZE, vets.size());
		return new PageImpl<>(vets.subList(start, end), pageable, vets.size());
	}

	@GetMapping({ "/vets" })
	public @ResponseBody Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vetRepository.findAll());
		return vets;
	}

}
