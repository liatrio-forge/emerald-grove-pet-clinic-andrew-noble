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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link UpcomingVisitsController}.
 */
@WebMvcTest(UpcomingVisitsController.class)
class UpcomingVisitsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitRepository visits;

	private static UpcomingVisit sampleVisit() {
		return new UpcomingVisit(6, "Jean", "Coleman", "Samantha", LocalDate.now(), "rabies shot");
	}

	@BeforeEach
	void setup() {
		given(this.visits.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class)))
			.willReturn(List.of(sampleVisit()));
	}

	@Test
	void shouldReturnUpcomingVisitsView() throws Exception {
		this.mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("upcomingVisits"))
			.andExpect(model().attribute("days", 7));
	}

	@Test
	void shouldDefaultToSevenDayWindowWhenDaysAbsent() throws Exception {
		// Capture the date range around the request so a midnight rollover between the
		// controller's LocalDate.now() and the assertion cannot make this test flaky.
		LocalDate before = LocalDate.now();
		this.mockMvc.perform(get("/visits/upcoming")).andExpect(status().isOk());
		LocalDate after = LocalDate.now();

		ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
		verify(this.visits).findUpcomingVisits(startCaptor.capture(), endCaptor.capture());
		assertThat(ChronoUnit.DAYS.between(startCaptor.getValue(), endCaptor.getValue())).isEqualTo(7);
		assertThat(startCaptor.getValue()).isBetween(before, after);
	}

	@Test
	void shouldUseProvidedDaysWindow() throws Exception {
		this.mockMvc.perform(get("/visits/upcoming").param("days", "30"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("days", 30));

		ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
		verify(this.visits).findUpcomingVisits(startCaptor.capture(), endCaptor.capture());
		assertThat(ChronoUnit.DAYS.between(startCaptor.getValue(), endCaptor.getValue())).isEqualTo(30);
	}

	@Test
	void shouldFallBackToDefaultWhenDaysIsNonNumeric() throws Exception {
		this.mockMvc.perform(get("/visits/upcoming").param("days", "abc"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("days", 7));
	}

	@Test
	void shouldFallBackToDefaultWhenDaysIsNotPositive() throws Exception {
		this.mockMvc.perform(get("/visits/upcoming").param("days", "0"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("days", 7));

		this.mockMvc.perform(get("/visits/upcoming").param("days", "-5"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("days", 7));
	}

	@Test
	void shouldRenderSuccessfullyWhenNoUpcomingVisits() throws Exception {
		given(this.visits.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class))).willReturn(List.of());

		this.mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attribute("upcomingVisits", List.of()));
	}

}
