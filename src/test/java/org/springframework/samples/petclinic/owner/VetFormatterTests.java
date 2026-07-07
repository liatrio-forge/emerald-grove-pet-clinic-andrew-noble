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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;

/**
 * Test class for {@link VetFormatter}
 */
@ExtendWith(MockitoExtension.class)
class VetFormatterTests {

	@Mock
	private VetRepository vets;

	private VetFormatter formatter;

	private Vet carter;

	@BeforeEach
	void setup() {
		this.formatter = new VetFormatter(this.vets);
		this.carter = new Vet();
		this.carter.setId(1);
		this.carter.setFirstName("James");
		this.carter.setLastName("Carter");
	}

	@Test
	void shouldPrintVetId() {
		assertThat(this.formatter.print(this.carter, Locale.ENGLISH)).isEqualTo("1");
	}

	@Test
	void shouldPrintEmptyStringForNullVet() {
		assertThat(this.formatter.print(null, Locale.ENGLISH)).isEmpty();
	}

	@Test
	void shouldParseKnownVetIdToMatchingVet() throws ParseException {
		given(this.vets.findAll()).willReturn(List.of(this.carter));

		Vet parsed = this.formatter.parse("1", Locale.ENGLISH);

		assertThat(parsed).isNotNull();
		assertThat(parsed.getId()).isEqualTo(1);
		assertThat(parsed.getLastName()).isEqualTo("Carter");
	}

	@Test
	void shouldParseEmptyStringToNull() throws ParseException {
		assertThat(this.formatter.parse("", Locale.ENGLISH)).isNull();
		assertThat(this.formatter.parse("   ", Locale.ENGLISH)).isNull();
	}

	@Test
	void shouldThrowWhenVetIdNotFound() {
		given(this.vets.findAll()).willReturn(List.of(this.carter));

		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> this.formatter.parse("999", Locale.ENGLISH));
	}

	@Test
	void shouldThrowWhenTextIsNotNumeric() {
		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> this.formatter.parse("abc", Locale.ENGLISH));
	}

}
