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

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(value = VisitController.class,
		includeFilters = @ComponentScan.Filter(value = VetFormatter.class, type = FilterType.ASSIGNABLE_TYPE))
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final int TEST_VET_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private AppointmentBookingService appointmentBookingService;

	@BeforeEach
	void init() {
		Owner owner = new Owner();
		Pet pet = new Pet();
		owner.addPet(pet);
		pet.setId(TEST_PET_ID);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));

		Vet vet = new Vet();
		vet.setId(TEST_VET_ID);
		vet.setFirstName("James");
		vet.setLastName("Carter");
		given(this.vets.findAll()).willReturn(List.of(vet));
		given(this.appointmentBookingService.bookNewVisit(any(), eq(TEST_PET_ID), any())).willReturn(true);
	}

	@Test
	void testInitNewVisitForm() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("vets"))
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void testProcessNewVisitFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("startTime", "10:00")
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", "Visit Description"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessNewVisitFormHasErrors() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).param("name",
					"George"))
			.andExpect(model().attributeHasErrors("visit"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void testProcessNewVisitFormVetRequired() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("startTime", "10:00")
				.param("description", "Annual checkup"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"))
			.andExpect(model().attributeHasFieldErrors("visit", "vet"));
	}

	@Test
	void testProcessNewVisitFormStartTimeRequired() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", "Annual checkup"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"))
			.andExpect(model().attributeHasFieldErrors("visit", "startTime"));
	}

	@Test
	void testProcessNewVisitFormVetConflictRejected() throws Exception {
		given(this.appointmentBookingService.bookNewVisit(any(), eq(TEST_PET_ID), any())).willReturn(false);

		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("startTime", "09:00")
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", "Overlaps the vet"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"))
			.andExpect(model().attributeHasFieldErrors("visit", "startTime"));

		verify(this.owners, never()).save(any());
	}

	@Test
	void testProcessNewVisitFormPetConflictRejected() throws Exception {
		given(this.appointmentBookingService.bookNewVisit(any(), eq(TEST_PET_ID), any())).willReturn(false);

		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("startTime", "09:00")
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", "Overlaps the pet"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"))
			.andExpect(model().attributeHasFieldErrors("visit", "startTime"));

		verify(this.owners, never()).save(any());
	}

	@Test
	void testProcessNewVisitFormBackToBackAllowed() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
				.param("startTime", "09:30")
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", "Back to back"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessNewVisitFormPastDateRejected() throws Exception {
		LocalDate pastLocalDate = LocalDate.now().minusDays(1);
		String pastDate = pastLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		String description = "Annual checkup";

		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
				.param("date", pastDate)
				.param("startTime", "10:00")
				.param("vet", String.valueOf(TEST_VET_ID))
				.param("description", description))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"))
			.andExpect(model().attributeHasFieldErrors("visit", "date"))
			// the re-rendered form preserves the values the user entered
			.andExpect(model().attribute("visit", hasProperty("description", is(description))))
			.andExpect(model().attribute("visit", hasProperty("date", is(pastLocalDate))));
	}

	@Test
	void testInitNewVisitFormOwnerNotFoundReturns404() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", 999999, TEST_PET_ID))
			.andExpect(status().isNotFound())
			.andExpect(view().name("error"));
	}

	@Test
	void testInitNewVisitFormPetNotFoundReturns404() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, 999999))
			.andExpect(status().isNotFound())
			.andExpect(view().name("error"));
	}

}
