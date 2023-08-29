/*
 * Copyright 2019-2023 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.ext.rest.api.auth;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The result of an authentication process returned by an authentication provider.
 *
 * @since 1.0
 */
@SuppressWarnings("ClassCanBeRecord") // NO - I don't want to expose the class constructor
public final class AuthenticationResult {

  private static final AuthenticationResult EMPTY = new AuthenticationResult(null, null);
  private static final AuthenticationResult USER_NOT_FOUND = new AuthenticationResult(State.USER_NOT_FOUND, null);
  private static final AuthenticationResult INVALID_CREDENTIALS = new AuthenticationResult(
    State.INVALID_CREDENTIALS,
    null);

  private final State state;
  private final RestUser user;

  /**
   * Constructs a new authentication result instance. This constructor should not be used, prefer the static methods
   * defined in this class instead.
   *
   * @param state the state. Only allowed to be null for the proceeding state.
   * @param user  the user that is associated with the result. Can be null if no user is required for the state.
   */
  private AuthenticationResult(@UnknownNullability State state, @UnknownNullability RestUser user) {
    this.state = state;
    this.user = user;
  }

  /**
   * Constructs a new authentication result that indicates a full successful authentication. The given required user is
   * the authenticated subject of the process.
   *
   * @param user the user that got authenticated successfully.
   * @return a new successful (OK) authentication result with the given user as the authenticated subject.
   * @throws NullPointerException if the given user is null.
   */
  @Contract("_ -> new")
  public static @NonNull AuthenticationResult ok(@NonNull RestUser user) {
    return new AuthenticationResult(State.OK, user);
  }

  /**
   * Returns a jvm-static authentication state that indicates that the current auth provider was unable to process the
   * login with the provided parameters and that the next authentication provider should be used to try to authenticate
   * the subject.
   *
   * @return a jvm-static result instance to indicate a handling delegation to the next auth provider.
   */
  public static @NonNull AuthenticationResult proceed() {
    return EMPTY;
  }

  /**
   * Returns a jvm-static authentication state that indicates that the current provider was able to decode the current
   * authentication information but the user that was supplied is not registered (anymore).
   *
   * @return a jvm-static result instance to indicate that the requested user was not found.
   */
  public static @NonNull AuthenticationResult userNotFound() {
    return USER_NOT_FOUND;
  }

  /**
   * Returns a jvm-static authentication state that indicates that the current provider would be able to handle the
   * authentication but the provided information did not pass the constraint validation. This can also be used if for
   * example the provided authentication token is no longer valid.
   *
   * @return a jvm-static result instance to indicate that the given credentials were invalid in any form.
   */
  public static @NonNull AuthenticationResult invalidCredentials() {
    return INVALID_CREDENTIALS;
  }

  /**
   * Gets if this result does not contain any information. This is used to indicate that the next authentication handler
   * should be used to process the request.
   *
   * @return true if this provider does not contain any information, false otherwise.
   */
  public boolean empty() {
    return this == EMPTY;
  }

  /**
   * Get if this result represents a successful authentication. If this method returns true, then retrieving the user
   * from this result will not throw an exception.
   *
   * @return true if this result represents a successful authentication, false otherwise.
   */
  public boolean ok() {
    return this.state == State.OK;
  }

  /**
   * Gets the authentication state of this result. This method cannot be called on an empty result.
   *
   * @return the authentication state that is represented by this result.
   * @throws IllegalStateException if this result is empty and therefore does not contain a state.
   * @see #empty()
   */
  public @NonNull State state() {
    Preconditions.checkState(this != EMPTY, "State is only present for non-empty results.");
    return this.state;
  }

  /**
   * Gets the authenticated subject of this result. This method cannot be called on any non-successful result.
   *
   * @return the authenticated subject that is represented by this result.
   * @throws IllegalStateException if this result does not represent a successful authentication.
   * @see #ok()
   */
  public @NonNull RestUser user() {
    Preconditions.checkState(this.state == State.OK, "User is only present on OK state.");
    return this.user;
  }

  /**
   * Represents the possible states of an authentication result.
   *
   * @since 1.0
   */
  public enum State {

    /**
     * The result is representing a successful authentication.
     */
    OK,
    /**
     * The requested user was not found.
     */
    USER_NOT_FOUND,
    /**
     * The credentials that were provided to the auth provider where invalid and therefore the authentication process
     * couldn't be completed.
     */
    INVALID_CREDENTIALS
  }
}
