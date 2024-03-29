/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.security.authorization;

import java.util.ArrayList;
import java.util.List;

/**
 * A factory class to create an {@link AuthorizationManager} instances.
 *
 * @author Evgeniy Cheban
 * @author Josh Cummings
 * @since 5.8
 */
public final class AuthorizationManagers {

	/**
	 * Creates an {@link AuthorizationManager} that grants access if at least one
	 * {@link AuthorizationManager} granted or abstained, if <code>managers</code> are
	 * empty then denied decision is returned.
	 * @param <T> the type of object that is being authorized
	 * @param managers the {@link AuthorizationManager}s to use
	 * @return the {@link AuthorizationManager} to use
	 */
	@SafeVarargs
	public static <T> AuthorizationManager<T> anyOf(AuthorizationManager<T>... managers) {
		return anyOf(new AuthorizationDecision(false), managers);
	}

	/**
	 * Creates an {@link AuthorizationManager} that grants access if at least one
	 * {@link AuthorizationManager} granted, if <code>managers</code> are empty or
	 * abstained, a default {@link AuthorizationDecision} is returned.
	 * @param <T> the type of object that is being authorized
	 * @param allAbstainDefaultDecision the default decision if all
	 * {@link AuthorizationManager}s abstained
	 * @param managers the {@link AuthorizationManager}s to use
	 * @return the {@link AuthorizationManager} to use
	 * @since 6.3
	 */
	@SafeVarargs
	public static <T> AuthorizationManager<T> anyOf(AuthorizationDecision allAbstainDefaultDecision,
			AuthorizationManager<T>... managers) {
		return (authentication, object) -> {
			List<AuthorizationDecision> decisions = new ArrayList<>();
			for (AuthorizationManager<T> manager : managers) {
				AuthorizationDecision decision = manager.check(authentication, object);
				if (decision == null) {
					continue;
				}
				if (decision.isGranted()) {
					return decision;
				}
				decisions.add(decision);
			}
			if (decisions.isEmpty()) {
				return allAbstainDefaultDecision;
			}
			return new CompositeAuthorizationDecision(false, decisions);
		};
	}

	/**
	 * Creates an {@link AuthorizationManager} that grants access if all
	 * {@link AuthorizationManager}s granted or abstained, if <code>managers</code> are
	 * empty then granted decision is returned.
	 * @param <T> the type of object that is being authorized
	 * @param managers the {@link AuthorizationManager}s to use
	 * @return the {@link AuthorizationManager} to use
	 */
	@SafeVarargs
	public static <T> AuthorizationManager<T> allOf(AuthorizationManager<T>... managers) {
		return allOf(new AuthorizationDecision(true), managers);
	}

	/**
	 * Creates an {@link AuthorizationManager} that grants access if all
	 * {@link AuthorizationManager}s granted, if <code>managers</code> are empty or
	 * abstained, a default {@link AuthorizationDecision} is returned.
	 * @param <T> the type of object that is being authorized
	 * @param allAbstainDefaultDecision the default decision if all
	 * {@link AuthorizationManager}s abstained
	 * @param managers the {@link AuthorizationManager}s to use
	 * @return the {@link AuthorizationManager} to use
	 * @since 6.3
	 */
	@SafeVarargs
	public static <T> AuthorizationManager<T> allOf(AuthorizationDecision allAbstainDefaultDecision,
			AuthorizationManager<T>... managers) {
		return (authentication, object) -> {
			List<AuthorizationDecision> decisions = new ArrayList<>();
			for (AuthorizationManager<T> manager : managers) {
				AuthorizationDecision decision = manager.check(authentication, object);
				if (decision == null) {
					continue;
				}
				if (!decision.isGranted()) {
					return decision;
				}
				decisions.add(decision);
			}
			if (decisions.isEmpty()) {
				return allAbstainDefaultDecision;
			}
			return new CompositeAuthorizationDecision(true, decisions);
		};
	}

	/**
	 * Creates an {@link AuthorizationManager} that reverses whatever decision the given
	 * {@link AuthorizationManager} granted. If the given {@link AuthorizationManager}
	 * abstains, then the returned manager also abstains.
	 * @param <T> the type of object that is being authorized
	 * @param manager the {@link AuthorizationManager} to reverse
	 * @return the reversing {@link AuthorizationManager}
	 * @since 6.3
	 */
	public static <T> AuthorizationManager<T> not(AuthorizationManager<T> manager) {
		return (authentication, object) -> {
			AuthorizationDecision decision = manager.check(authentication, object);
			if (decision == null) {
				return null;
			}
			return new NotAuthorizationDecision(decision);
		};
	}

	private AuthorizationManagers() {
	}

	private static final class CompositeAuthorizationDecision extends AuthorizationDecision {

		private final List<AuthorizationDecision> decisions;

		private CompositeAuthorizationDecision(boolean granted, List<AuthorizationDecision> decisions) {
			super(granted);
			this.decisions = decisions;
		}

		@Override
		public String toString() {
			return "CompositeAuthorizationDecision [decisions=" + this.decisions + ']';
		}

	}

	private static final class NotAuthorizationDecision extends AuthorizationDecision {

		private final AuthorizationDecision decision;

		private NotAuthorizationDecision(AuthorizationDecision decision) {
			super(!decision.isGranted());
			this.decision = decision;
		}

		@Override
		public String toString() {
			return "NotAuthorizationDecision [decision=" + this.decision + ']';
		}

	}

}
