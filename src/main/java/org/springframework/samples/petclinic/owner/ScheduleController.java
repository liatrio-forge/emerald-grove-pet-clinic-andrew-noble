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
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only controller serving the clinic-wide day schedule, which lists every
 * appointment on a single date (defaulting to today) with prev/next-day navigation.
 */
@Controller
class ScheduleController {

	private final VisitRepository visits;

	ScheduleController(VisitRepository visits) {
		this.visits = visits;
	}

	/**
	 * Render the schedule for the requested day.
	 * @param date optional {@code yyyy-MM-dd} date; missing, blank, or unparseable values
	 * fall back to today so a bad parameter never produces an error response
	 * @param model the view model
	 * @return the day schedule view name
	 */
	@GetMapping("/schedule")
	public String showSchedule(@RequestParam(name = "date", required = false) String date, Model model) {
		LocalDate day = resolveDate(date);
		List<ScheduledAppointment> appointments = this.visits.findScheduledAppointmentsByDate(day);

		model.addAttribute("appointments", appointments);
		model.addAttribute("day", day);
		model.addAttribute("prevDay", day.minusDays(1));
		model.addAttribute("nextDay", day.plusDays(1));
		return "schedule/daySchedule";
	}

	/**
	 * Coerce the raw {@code date} request parameter into a valid date, falling back to
	 * today for missing, blank, or unparseable input.
	 * @param date the raw request parameter value, possibly {@code null}
	 * @return the resolved date
	 */
	private LocalDate resolveDate(String date) {
		if (date == null || date.isBlank()) {
			return LocalDate.now();
		}
		try {
			return LocalDate.parse(date.trim());
		}
		catch (DateTimeParseException ex) {
			return LocalDate.now();
		}
	}

}
