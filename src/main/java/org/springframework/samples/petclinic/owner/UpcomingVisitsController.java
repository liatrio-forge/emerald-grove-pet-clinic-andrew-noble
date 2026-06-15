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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only controller serving the Upcoming Visits page, which lists every visit
 * scheduled within the next N days (default {@value #DEFAULT_DAYS}).
 */
@Controller
class UpcomingVisitsController {

	/** Default and fallback size of the look-ahead window, in days. */
	static final int DEFAULT_DAYS = 7;

	private final VisitRepository visits;

	UpcomingVisitsController(VisitRepository visits) {
		this.visits = visits;
	}

	/**
	 * Render the upcoming visits within the requested window.
	 * @param days optional look-ahead size in days; missing, non-numeric, or non-positive
	 * values fall back to {@value #DEFAULT_DAYS}
	 * @param model the view model
	 * @return the upcoming visits view name
	 */
	@GetMapping("/visits/upcoming")
	public String showUpcomingVisits(@RequestParam(name = "days", required = false) String days, Model model) {
		int windowDays = resolveWindowDays(days);
		LocalDate start = LocalDate.now();
		LocalDate end = start.plusDays(windowDays);

		List<UpcomingVisit> upcomingVisits = this.visits.findUpcomingVisits(start, end);
		model.addAttribute("upcomingVisits", upcomingVisits);
		model.addAttribute("days", windowDays);
		return "visits/upcomingVisits";
	}

	/**
	 * Coerce the raw {@code days} request parameter into a valid positive window size,
	 * falling back to {@value #DEFAULT_DAYS} for missing, non-numeric, or non-positive
	 * input so a bad parameter never produces an error response.
	 * @param days the raw request parameter value, possibly {@code null}
	 * @return a positive number of days
	 */
	private int resolveWindowDays(String days) {
		if (days == null) {
			return DEFAULT_DAYS;
		}
		try {
			int parsed = Integer.parseInt(days.trim());
			return parsed < 1 ? DEFAULT_DAYS : parsed;
		}
		catch (NumberFormatException ex) {
			return DEFAULT_DAYS;
		}
	}

}
