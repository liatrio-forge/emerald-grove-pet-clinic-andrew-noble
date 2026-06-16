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

import java.nio.charset.StandardCharsets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Exports owner search results as a CSV file. Reuses the same optional search criteria as
 * the HTML owners search ({@code lastName}, {@code city}, {@code telephone}, combined
 * with AND) and returns every matching owner (unpaged) as a downloadable {@code text/csv}
 * response. The CSV is RFC 4180 compliant: a header row, CRLF line terminators, and
 * fields containing a comma, double quote, or line break are quoted with embedded quotes
 * doubled. The response is encoded as UTF-8 and advertises {@code charset=UTF-8} on the
 * content type so non-ASCII owner names and addresses are not corrupted.
 *
 * @author Andrew Noble
 */
@Controller
class OwnerCsvExportController {

	private static final String HEADER_ROW = "firstName,lastName,address,city,telephone";

	private static final String CRLF = "\r\n";

	private final OwnerRepository owners;

	OwnerCsvExportController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping(path = "/owners.csv", produces = "text/csv")
	ResponseEntity<String> exportOwnersCsv(@RequestParam(defaultValue = "") String lastName,
			@RequestParam(defaultValue = "") String city, @RequestParam(defaultValue = "") String telephone) {
		Page<Owner> matches = this.owners.findByOptionalCriteria(lastName, city, telephone, Pageable.unpaged());

		StringBuilder csv = new StringBuilder(HEADER_ROW).append(CRLF);
		for (Owner owner : matches) {
			csv.append(escape(owner.getFirstName()))
				.append(',')
				.append(escape(owner.getLastName()))
				.append(',')
				.append(escape(owner.getAddress()))
				.append(',')
				.append(escape(owner.getCity()))
				.append(',')
				.append(escape(owner.getTelephone()))
				.append(CRLF);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"owners.csv\"");
		return new ResponseEntity<>(csv.toString(), headers, HttpStatus.OK);
	}

	/**
	 * Escape a single CSV field per RFC 4180: if it contains a comma, double quote, CR,
	 * or LF, wrap it in double quotes and double any embedded double quotes.
	 * @param value the raw field value (may be {@code null})
	 * @return the escaped field, ready to be written between commas
	 */
	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n");
		if (!mustQuote) {
			return value;
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}

}
