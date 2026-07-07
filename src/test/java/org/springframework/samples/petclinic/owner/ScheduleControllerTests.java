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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for {@link ScheduleController}
 */
@WebMvcTest(ScheduleController.class)
@DisabledInNativeImage
@DisabledInAotMode
class ScheduleControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitRepository visits;

	private ScheduledAppointment appointment(LocalTime startTime, String description) {
		return new ScheduledAppointment(1, "Jean", "Coleman", "Samantha", "James", "Carter", LocalDate.now(), startTime,
				description);
	}

	@Test
	void showScheduleDefaultsToToday() throws Exception {
		mockMvc.perform(get("/schedule"))
			.andExpect(status().isOk())
			.andExpect(view().name("schedule/daySchedule"))
			.andExpect(model().attribute("day", LocalDate.now()))
			.andExpect(model().attributeExists("appointments"));
	}

	@Test
	void showScheduleUsesRequestedDateAndPreservesOrder() throws Exception {
		LocalDate date = LocalDate.of(2020, 5, 1);
		List<ScheduledAppointment> ordered = List.of(appointment(LocalTime.of(9, 0), "first"),
				appointment(LocalTime.of(11, 0), "second"));
		given(this.visits.findScheduledAppointmentsByDate(eq(date))).willReturn(ordered);

		mockMvc.perform(get("/schedule").param("date", "2020-05-01"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("day", date))
			.andExpect(model().attribute("prevDay", LocalDate.of(2020, 4, 30)))
			.andExpect(model().attribute("nextDay", LocalDate.of(2020, 5, 2)))
			.andExpect(model().attribute("appointments", sameInstance(ordered)));
	}

	@Test
	void showScheduleFallsBackToTodayForInvalidDate() throws Exception {
		mockMvc.perform(get("/schedule").param("date", "not-a-date"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("day", LocalDate.now()));
	}

	@Test
	void showScheduleFallsBackToTodayForBlankDate() throws Exception {
		mockMvc.perform(get("/schedule").param("date", "   "))
			.andExpect(status().isOk())
			.andExpect(model().attribute("day", LocalDate.now()));
	}

	@Test
	void showScheduleRendersEmptyStateWhenNoAppointments() throws Exception {
		given(this.visits.findScheduledAppointmentsByDate(any())).willReturn(List.of());

		mockMvc.perform(get("/schedule"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("appointments", empty()));
	}

}
