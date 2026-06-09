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
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link VetController}
 */

@WebMvcTest(VetController.class)
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	private Vet vet(int id, String firstName, String lastName, String... specialtyNames) {
		Vet vet = new Vet();
		vet.setId(id);
		vet.setFirstName(firstName);
		vet.setLastName(lastName);
		int specialtyId = 1;
		for (String specialtyName : specialtyNames) {
			Specialty specialty = new Specialty();
			specialty.setId(specialtyId++);
			specialty.setName(specialtyName);
			vet.addSpecialty(specialty);
		}
		return vet;
	}

	/**
	 * Seed-like sample: James (no specialty), Helen (radiology), Linda (surgery,
	 * dentistry), Rafael (surgery), Sharon (no specialty).
	 */
	private List<Vet> sampleVets() {
		List<Vet> vetList = new ArrayList<>();
		vetList.add(vet(1, "James", "Carter"));
		vetList.add(vet(2, "Helen", "Leary", "radiology"));
		vetList.add(vet(3, "Linda", "Douglas", "surgery", "dentistry"));
		vetList.add(vet(4, "Rafael", "Ortega", "surgery"));
		vetList.add(vet(6, "Sharon", "Jenkins"));
		return vetList;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(sampleVets());
		given(this.vets.findAll(any(Pageable.class))).willReturn(new PageImpl<Vet>(sampleVets()));
	}

	@Test
	void testShowVetListHtml() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(view().name("vets/vetList"));

	}

	@Test
	void testShowResourcesVetList() throws Exception {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(vet(1, "James", "Carter")));
		ResultActions actions = mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		actions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.vetList[0].id").value(1));
	}

	@Test
	void testFilterByNamedSpecialtyShowsOnlyMatchingVets() throws Exception {
		mockMvc.perform(get("/vets.html?specialty=surgery"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", hasSize(2)))
			.andExpect(model().attribute("listVets",
					everyItem(hasProperty("specialties", hasItem(hasProperty("name", is("surgery")))))))
			.andExpect(model().attribute("selectedSpecialty", is("surgery")));
	}

	@Test
	void testFilterByNoneShowsOnlyVetsWithoutSpecialties() throws Exception {
		mockMvc.perform(get("/vets.html?specialty=none"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", hasSize(2)))
			.andExpect(model().attribute("listVets", everyItem(hasProperty("nrOfSpecialties", is(0)))))
			.andExpect(model().attribute("selectedSpecialty", is("none")));
	}

	@Test
	void testNoFilterShowsAllVets() throws Exception {
		mockMvc.perform(get("/vets.html"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", hasSize(5)))
			.andExpect(model().attribute("selectedSpecialty", is("all")));
	}

	@Test
	void testPageBelowOneIsTreatedAsFirstPage() throws Exception {
		mockMvc.perform(get("/vets.html?page=0"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("currentPage", is(1)))
			.andExpect(model().attribute("listVets", hasSize(5)));
	}

	@Test
	void testInvalidSpecialtyShowsEmptyListAndFallsBackToAll() throws Exception {
		mockMvc.perform(get("/vets.html?specialty=bogus"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listVets", empty()))
			.andExpect(model().attribute("selectedSpecialty", is("all")));
	}

	@Test
	void testModelExposesAvailableSpecialtyOptionsSortedAndDistinct() throws Exception {
		mockMvc.perform(get("/vets.html"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("specialties", contains("dentistry", "radiology", "surgery")));
	}

	@Test
	void testSelectedSpecialtyOptionIsMarkedSelectedInDropdown() throws Exception {
		mockMvc.perform(get("/vets.html?specialty=surgery"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("name=\"specialty\"")))
			.andExpect(content().string(containsString("<option value=\"surgery\" selected")));
	}

	@Test
	void testFilteredResultsPaginateAndPreserveFilterAcrossPages() throws Exception {
		// Eight vets all share the "surgery" specialty -> 2 pages at page size 5.
		List<Vet> manySurgeons = new ArrayList<>();
		for (int i = 1; i <= 8; i++) {
			manySurgeons.add(vet(i, "Vet" + i, "Surgeon", "surgery"));
		}
		given(this.vets.findAll()).willReturn(manySurgeons);

		mockMvc.perform(get("/vets.html?specialty=surgery&page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("totalItems", is(8L)))
			.andExpect(model().attribute("totalPages", is(2)))
			.andExpect(model().attribute("listVets", hasSize(5)));

		mockMvc.perform(get("/vets.html?specialty=surgery&page=2"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("currentPage", is(2)))
			.andExpect(model().attribute("selectedSpecialty", is("surgery")))
			.andExpect(model().attribute("listVets", hasSize(3)))
			.andExpect(model().attribute("listVets",
					everyItem(hasProperty("specialties", hasItem(hasProperty("name", is("surgery")))))))
			.andExpect(model().attribute("listVets", not(empty())));
	}

}
