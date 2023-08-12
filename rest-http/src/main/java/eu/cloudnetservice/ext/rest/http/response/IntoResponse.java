package eu.cloudnetservice.ext.rest.http.response;

import lombok.NonNull;

/**
 * A wrapper object to construct a response from an object.
 *
 * @param <T> the type of the response body.
 * @since 4.0
 */
public interface IntoResponse<T> {

  /**
   * Constructs the inner response data for this object.
   *
   * @return the response data for the object that implements this interface.
   */
  default @NonNull Response<T> intoResponse() {
    return this.intoResponseBuilder().build();
  }

  /**
   * Constructs a response builder from this object.
   *
   * @return a builder for a response containing information from this object.
   */
  @NonNull Response.Builder<T, ?> intoResponseBuilder();
}
