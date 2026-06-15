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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link OwnerCsvExportController}, the CSV export endpoint for owner search
 * results.
 *
 * @author Andrew Noble
 */
@WebMvcTest(OwnerCsvExportController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerCsvExportControllerTests {

	private static final String HEADER_ROW = "firstName,lastName,address,city,telephone";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	private Owner owner(String firstName, String lastName, String address, String city, String telephone) {
		Owner owner = new Owner();
		owner.setFirstName(firstName);
		owner.setLastName(lastName);
		owner.setAddress(address);
		owner.setCity(city);
		owner.setTelephone(telephone);
		return owner;
	}

	private Page<Owner> page(Owner... owners) {
		return new PageImpl<>(List.of(owners));
	}

	// --- Task 1.0: filtering, unpaged retrieval, blank/no-match paths ---

	@Test
	void exportReturnsOkAndCsvContentType() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")));

		this.mockMvc.perform(get("/owners.csv"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"));
	}

	@Test
	void exportRendersOneDataRowPerOwnerBeneathHeader() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"),
					owner("Betty", "Davis", "638 Cardinal Ave.", "Sun Prairie", "6085551749")));

		String body = this.mockMvc.perform(get("/owners.csv"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String[] lines = body.split("\\r\\n|\\n");
		assertThat(lines[0]).isEqualTo(HEADER_ROW);
		assertThat(lines).hasSize(3);
		assertThat(body).contains("Franklin").contains("Davis");
	}

	@Test
	void exportPassesSearchCriteriaAndUnpagedPageableToRepository() throws Exception {
		given(this.owners.findByOptionalCriteria(eq("Davis"), eq("Madison"), eq("6085551023"), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")));

		this.mockMvc.perform(
				get("/owners.csv").param("lastName", "Davis").param("city", "Madison").param("telephone", "6085551023"))
			.andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(this.owners).findByOptionalCriteria(eq("Davis"), eq("Madison"), eq("6085551023"),
				pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().isUnpaged()).isTrue();
	}

	@Test
	void exportWithoutParametersUsesBlankCriteriaAndReturnsAllOwners() throws Exception {
		given(this.owners.findByOptionalCriteria(eq(""), eq(""), eq(""), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"),
					owner("Betty", "Davis", "638 Cardinal Ave.", "Sun Prairie", "6085551749")));

		String body = this.mockMvc.perform(get("/owners.csv"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		verify(this.owners).findByOptionalCriteria(eq(""), eq(""), eq(""), any(Pageable.class));
		assertThat(body.split("\\r\\n|\\n")).hasSize(3);
	}

	@Test
	void exportWithNoMatchesReturnsHeaderRowOnly() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class))).willReturn(page());

		String body = this.mockMvc.perform(get("/owners.csv").param("lastName", "Nonexistent"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String[] lines = body.split("\\r\\n|\\n");
		assertThat(lines).containsExactly(HEADER_ROW);
	}

	// --- Task 2.0: content type, download headers, header row, escaping, CRLF ---

	@Test
	void exportSetsCsvContentTypeAndAttachmentDownloadHeader() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")));

		this.mockMvc.perform(get("/owners.csv"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(header().string("Content-Disposition", "attachment; filename=\"owners.csv\""));
	}

	@Test
	void exportFirstLineIsHeaderRowInColumnOrder() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")));

		String body = this.mockMvc.perform(get("/owners.csv")).andReturn().getResponse().getContentAsString();

		assertThat(body).startsWith(HEADER_ROW + "\r\n");
	}

	@Test
	void exportRendersFieldsInColumnOrderTerminatedByCrlf() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023")));

		String body = this.mockMvc.perform(get("/owners.csv")).andReturn().getResponse().getContentAsString();

		assertThat(body)
			.isEqualTo(HEADER_ROW + "\r\n" + "George,Franklin,110 W. Liberty St.,Madison,6085551023" + "\r\n");
	}

	@Test
	void exportEscapesFieldsContainingCommasQuotesAndNewlines() throws Exception {
		given(this.owners.findByOptionalCriteria(any(), any(), any(), any(Pageable.class)))
			.willReturn(page(owner("George", "Smith \"Jr\", III", "12 Main St.\nApt 4", "Madison", "6085551023")));

		String body = this.mockMvc.perform(get("/owners.csv")).andReturn().getResponse().getContentAsString();

		assertThat(body).contains("\"Smith \"\"Jr\"\", III\"");
		assertThat(body).contains("\"12 Main St.\nApt 4\"");
	}

}
