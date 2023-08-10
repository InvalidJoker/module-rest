package eu.cloudnetservice.ext.rest.http.response;

import lombok.NonNull;

/**
 * A wrapper object to construct a response from an object.
 *
 * @param <T> the type of the response body.
 * @since 4.0
 */
@FunctionalInterface
public interface IntoResponse<T> {

  /**
   * Constructs the inner response data for this object.
   *
   * @return the response data for the object that implements this interface.
   */
  @NonNull Response<T> into();
}
