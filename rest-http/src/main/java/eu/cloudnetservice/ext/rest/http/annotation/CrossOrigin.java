package eu.cloudnetservice.ext.rest.http.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossOrigin {

  @Language("RegExp")
  String[] origins() default {};

  String[] allowedHeaders() default {};

  String[] exposedHeaders() default {};

  TriState allowCredentials() default TriState.UNDEFINED;

  TriState allowPrivateNetworks() default TriState.UNDEFINED;

  @Range(from = 1, to = 86400)
  long maxAge() default -1;

  enum TriState {
    TRUE,
    FALSE,
    UNDEFINED;

    public @Nullable Boolean toBoolean() {
      if (this == UNDEFINED) {
        return null;
      }

      return this == TRUE ? Boolean.TRUE : Boolean.FALSE;
    }
  }

}
