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

import java.text.ParseException;
import java.util.Locale;

import org.springframework.format.Formatter;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Component;

/**
 * Instructs Spring MVC on how to parse and print {@link Vet} elements when they are bound
 * through a form field (the veterinarian dropdown on the booking form). Mirrors
 * {@link PetTypeFormatter}, but binds on the vet's numeric id rather than its name.
 * <p>
 * An empty selection is treated as "no vet chosen" and parses to {@code null} so the
 * required-field validation on {@link Visit#getVet()} can produce a friendly error.
 */
@Component
public class VetFormatter implements Formatter<Vet> {

	private final VetRepository vets;

	public VetFormatter(VetRepository vets) {
		this.vets = vets;
	}

	@Override
	public String print(Vet vet, Locale locale) {
		return (vet == null || vet.getId() == null) ? "" : String.valueOf(vet.getId());
	}

	@Override
	public Vet parse(String text, Locale locale) throws ParseException {
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			int id = Integer.parseInt(text.trim());
			for (Vet vet : this.vets.findAll()) {
				if (vet.getId() != null && vet.getId() == id) {
					return vet;
				}
			}
		}
		catch (NumberFormatException ex) {
			throw new ParseException("Not a valid vet id: " + text, 0);
		}
		throw new ParseException("Vet not found: " + text, 0);
	}

}
