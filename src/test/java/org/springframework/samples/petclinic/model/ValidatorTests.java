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

package org.springframework.samples.petclinic.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * @author Michael Isvy Simple test to make sure that Bean Validation is working (useful
 * when upgrading to a new version of Hibernate Validator/ Bean Validation)
 */
class ValidatorTests {

	private Validator createValidator() {
		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.afterPropertiesSet();
		return localValidatorFactoryBean;
	}

	/**
	 * Validator backed by the application message bundle so that custom message keys
	 * (e.g. {visit.date.future}) resolve exactly as they do at runtime.
	 */
	private Validator createValidatorWithMessages() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages/messages");
		messageSource.setDefaultEncoding("UTF-8");

		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.setValidationMessageSource(messageSource);
		localValidatorFactoryBean.afterPropertiesSet();
		return localValidatorFactoryBean;
	}

	@Test
	void shouldNotValidateWhenFirstNameEmpty() {

		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Person person = new Person();
		person.setFirstName("");
		person.setLastName("smith");

		Validator validator = createValidator();
		Set<ConstraintViolation<Person>> constraintViolations = validator.validate(person);

		assertThat(constraintViolations).hasSize(1);
		ConstraintViolation<Person> violation = constraintViolations.iterator().next();
		assertThat(violation.getPropertyPath()).hasToString("firstName");
		assertThat(violation.getMessage()).isEqualTo("must not be blank");
	}

	@Test
	void shouldNotValidateWhenVisitDateInPast() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Visit visit = new Visit();
		visit.setDescription("Checkup");
		visit.setDate(LocalDate.now().minusDays(1));

		Validator validator = createValidatorWithMessages();
		Set<ConstraintViolation<Visit>> constraintViolations = validator.validate(visit);

		assertThat(constraintViolations).hasSize(1);
		ConstraintViolation<Visit> violation = constraintViolations.iterator().next();
		assertThat(violation.getPropertyPath()).hasToString("date");
		assertThat(violation.getMessage()).isEqualTo("must be today or a future date");
	}

	@Test
	void shouldValidateWhenVisitDateIsToday() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Visit visit = new Visit();
		visit.setDescription("Checkup");
		visit.setDate(LocalDate.now());

		Validator validator = createValidator();
		Set<ConstraintViolation<Visit>> constraintViolations = validator.validate(visit);

		assertThat(constraintViolations).isEmpty();
	}

	@Test
	void shouldValidateWhenVisitDateInFuture() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Visit visit = new Visit();
		visit.setDescription("Checkup");
		visit.setDate(LocalDate.now().plusDays(1));

		Validator validator = createValidator();
		Set<ConstraintViolation<Visit>> constraintViolations = validator.validate(visit);

		assertThat(constraintViolations).isEmpty();
	}

}
