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

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository class for <code>Owner</code> domain objects. All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Wick Dynex
 */
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a Collection of matching {@link Owner}s (or an empty Collection if none
	 * found)
	 */
	Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);

	/**
	 * Retrieve {@link Owner}s matching all provided (non-blank) criteria, combined with
	 * AND: last name <i>starts-with</i>, city <i>starts-with</i>, and telephone
	 * <i>exact</i> match. A blank ({@code ""}) value for any parameter disables filtering
	 * on that field, so an all-blank call returns every owner.
	 * @param lastName last-name prefix to match, or {@code ""} to ignore
	 * @param city city prefix to match, or {@code ""} to ignore
	 * @param telephone exact telephone to match, or {@code ""} to ignore
	 * @param pageable paging/sorting information
	 * @return a {@link Page} of matching {@link Owner}s
	 */
	@Query("""
			SELECT o FROM Owner o
			WHERE (:lastName = '' OR o.lastName LIKE CONCAT(:lastName, '%'))
			AND (:city = '' OR o.city LIKE CONCAT(:city, '%'))
			AND (:telephone = '' OR o.telephone = :telephone)
			""")
	Page<Owner> findByOptionalCriteria(@Param("lastName") String lastName, @Param("city") String city,
			@Param("telephone") String telephone, Pageable pageable);

	/**
	 * Retrieve an {@link Owner} from the data store by id.
	 * <p>
	 * This method returns an {@link Optional} containing the {@link Owner} if found. If
	 * no {@link Owner} is found with the provided id, it will return an empty
	 * {@link Optional}.
	 * </p>
	 * @param id the id to search for
	 * @return an {@link Optional} containing the {@link Owner} if found, or an empty
	 * {@link Optional} if not found.
	 * @throws IllegalArgumentException if the id is null (assuming null is not a valid
	 * input for id)
	 */
	Optional<Owner> findById(Integer id);

}
