/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.servlet.filter;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

/**
 * {@link Filter} that intercepts error dispatches to ensure authorized access to the
 * error page.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public class ErrorPageSecurityFilter implements Filter {

	private static final WebInvocationPrivilegeEvaluator ALWAYS = new AlwaysAllowWebInvocationPrivilegeEvaluator();

	private final ApplicationContext context;

	private volatile WebInvocationPrivilegeEvaluator privilegeEvaluator;

	public ErrorPageSecurityFilter(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
	}

	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (DispatcherType.ERROR.equals(request.getDispatcherType()) && !isAllowed(request)) {
			sendError(request, response);
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean isAllowed(HttpServletRequest request) {
		String uri = request.getRequestURI();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return getPrivilegeEvaluator().isAllowed(uri, authentication);
	}

	private WebInvocationPrivilegeEvaluator getPrivilegeEvaluator() {
		WebInvocationPrivilegeEvaluator privilegeEvaluator = this.privilegeEvaluator;
		if (privilegeEvaluator == null) {
			privilegeEvaluator = getPrivilegeEvaluatorBean();
			this.privilegeEvaluator = privilegeEvaluator;
		}
		return privilegeEvaluator;
	}

	private WebInvocationPrivilegeEvaluator getPrivilegeEvaluatorBean() {
		try {
			return this.context.getBean(WebInvocationPrivilegeEvaluator.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return ALWAYS;
		}
	}

	private void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Integer errorCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		response.sendError((errorCode != null) ? errorCode : 401);
	}

	/**
	 * {@link WebInvocationPrivilegeEvaluator} that always allows access.
	 */
	private static class AlwaysAllowWebInvocationPrivilegeEvaluator implements WebInvocationPrivilegeEvaluator {

		@Override
		public boolean isAllowed(String uri, Authentication authentication) {
			return true;
		}

		@Override
		public boolean isAllowed(String contextPath, String uri, String method, Authentication authentication) {
			return true;
		}

	}

}
