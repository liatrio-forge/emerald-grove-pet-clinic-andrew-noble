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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.model.BaseEntity;
import org.springframework.samples.petclinic.vet.Vet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

/**
 * Simple JavaBean domain object representing a visit.
 *
 * @author Ken Krebs
 * @author Dave Syer
 */
@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {

	/**
	 * Fixed duration of every appointment. Centralized here so a future spec can make it
	 * configurable; used by conflict detection to compute each appointment's end time.
	 */
	public static final Duration APPOINTMENT_DURATION = Duration.ofMinutes(30);

	@Column(name = "visit_date")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@FutureOrPresent(message = "{visit.date.future}")
	private LocalDate date;

	@Column(name = "start_time")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime startTime;

	@ManyToOne
	@JoinColumn(name = "vet_id")
	private Vet vet;

	@NotBlank
	private String description;

	/**
	 * Creates a new instance of Visit for the current date. The start time and vet are
	 * intentionally left {@code null} so that new bookings are required to supply them
	 * (see the validation on the booking form), while legacy rows remain valid.
	 */
	public Visit() {
		this.date = LocalDate.now();
	}

	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getStartTime() {
		return this.startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * The effective end time of this appointment, derived from its start time and the
	 * fixed {@link #APPOINTMENT_DURATION}.
	 * @return the end time, or {@code null} if this visit has no start time
	 */
	public LocalTime getEndTime() {
		return (this.startTime != null) ? this.startTime.plus(APPOINTMENT_DURATION) : null;
	}

	public Vet getVet() {
		return this.vet;
	}

	public void setVet(Vet vet) {
		this.vet = vet;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
