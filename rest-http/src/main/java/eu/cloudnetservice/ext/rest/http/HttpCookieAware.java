package eu.cloudnetservice.ext.rest.http;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Some http message that is aware how to handle cookies (and therefore headers).
 *
 * @param <T> the type of the http message.
 * @since 4.0
 */
public interface HttpCookieAware<T> {

  /**
   * Gets a cookie by its name from the current context. Cookies are decoded and encoded in a relaxed (lax) format, this
   * means that duplicate cookies are allowed. If there are multiple cookies with the same name, the first one is
   * returned by this method. If no cookie with the given name is present, this method returns null.
   *
   * @param name the name of the cookie to get.
   * @return the first cookie with the given name.
   * @throws NullPointerException if the given name is null.
   */
  @Nullable HttpCookie cookie(@NonNull String name);

  /**
   * Get all cookies that are known to this context. Cookies are decoded and encoded in a relaxed (lax) format, this
   * means that duplicate cookies are allowed.
   *
   * @return all cookies.
   */
  @NonNull Collection<HttpCookie> cookies();

  /**
   * Get if a cookie with the given name is known to the current context. Cookies are decoded and encoded in a relaxed
   * (lax) format, this means that duplicate cookies are allowed. If there are multiple cookies with the same name set
   * in the request this method returns true anyway.
   *
   * @param name the name of the cookie to check for.
   * @return true if a cookie with the given name is present, false otherwise.
   * @throws NullPointerException if the given name is null.
   */
  boolean hasCookie(@NonNull String name);

  /**
   * Sets the given cookies in this context, removing all previously set cookies.
   *
   * @param cookies the new cookies of this context.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie collection is null.
   */
  @Contract("_ -> this")
  @NonNull T cookies(@NonNull Collection<HttpCookie> cookies);

  /**
   * Adds the given cookie to this context, removing the current one if there is already a cookie with the same name.
   *
   * @param httpCookie the cookie to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie is null.
   */
  @Contract("_ -> this")
  @NonNull T addCookie(@NonNull HttpCookie httpCookie);

  /**
   * Removes the given cookie from this context, if set.
   *
   * @param name the name of the cookie to remove.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given cookie is null.
   */
  @Contract("_ -> this")
  @NonNull T removeCookie(@NonNull String name);

  /**
   * Removes all cookies from this context.
   *
   * @return the same instance as used to call the method, for chaining.
   */
  @Contract(" -> this")
  @NonNull T clearCookies();
}
