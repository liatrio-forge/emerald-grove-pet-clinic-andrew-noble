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

package org.springframework.samples.petclinic.system;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * View-layer tests for the header language selector rendered by the shared
 * {@code fragments/layout.html} template. Uses a full application context so the
 * Thymeleaf layout and i18n message bundles are resolved exactly as in production,
 * including the existing {@code LocaleChangeInterceptor}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class LanguageSelectorViewTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void homePageRendersLanguageSelectorWithThreeNativeLanguageOptions() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("data-testid=\"language-selector\"")))
			.andExpect(content().string(containsString("lang=en")))
			.andExpect(content().string(containsString("lang=es")))
			.andExpect(content().string(containsString("lang=de")))
			.andExpect(content().string(containsString("English")))
			.andExpect(content().string(containsString("Español")))
			.andExpect(content().string(containsString("Deutsch")));
	}

	@Test
	void selectingSpanishRendersCurrentPageInSpanishWithActiveIndicator() throws Exception {
		mockMvc.perform(get("/").param("lang", "es"))
			.andExpect(status().isOk())
			// Spanish nav label proves the current page is re-rendered in Spanish.
			.andExpect(content().string(containsString("Inicio")))
			// aria-current is only emitted on the active (matching) language option.
			.andExpect(content().string(containsString("aria-current=\"true\"")));
	}

}
