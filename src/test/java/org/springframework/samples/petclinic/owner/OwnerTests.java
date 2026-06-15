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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Owner} aggregate, focused on pet membership behavior.
 */
class OwnerTests {

	@Test
	void shouldRemovePetFromOwner() {
		// Arrange: an owner with two pets
		Owner owner = new Owner();
		Pet rex = new Pet();
		rex.setName("Rex");
		Pet max = new Pet();
		max.setName("Max");
		owner.addPet(rex);
		owner.addPet(max);
		assertThat(owner.getPets()).containsExactlyInAnyOrder(rex, max);

		// Act: remove one pet
		owner.removePet(rex);

		// Assert: only the removed pet is gone; the other remains
		assertThat(owner.getPets()).containsExactly(max);
		assertThat(owner.getPets()).doesNotContain(rex);
	}

}
