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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global exception handler that renders user-friendly error pages instead of exposing raw
 * exception views or stack traces.
 *
 * @author Andrew Noble
 */
@ControllerAdvice
class GlobalExceptionHandler {

	/**
	 * Renders a friendly 404 page (the shared {@code error} view) when a requested owner
	 * or pet cannot be found. No exception details are exposed to the user.
	 * @return a {@link ModelAndView} for the error view with a 404 status marker
	 */
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ModelAndView handleNotFound() {
		ModelAndView mav = new ModelAndView("error");
		mav.addObject("status", HttpStatus.NOT_FOUND.value());
		return mav;
	}

}
