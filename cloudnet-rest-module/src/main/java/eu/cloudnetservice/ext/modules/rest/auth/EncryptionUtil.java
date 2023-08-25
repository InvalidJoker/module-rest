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

package eu.cloudnetservice.ext.modules.rest.auth;

import eu.cloudnetservice.common.tuple.Tuple2;
import eu.cloudnetservice.common.util.StringUtil;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import lombok.NonNull;

final class EncryptionUtil {

  private static final int KEY_LENGTH = 512;
  private static final int ITERATION_COUNT = 1_000_000;

  public static @NonNull Tuple2<String, String> encrypt(@NonNull String password) {
    var salt = StringUtil.generateRandomString(16);
    var keySpec = new PBEKeySpec(
      password.toCharArray(),
      salt.getBytes(StandardCharsets.UTF_8),
      ITERATION_COUNT,
      KEY_LENGTH);

    try {
      var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      var hash = factory.generateSecret(keySpec).getEncoded();

      return new Tuple2<>(salt, new String(hash, StandardCharsets.UTF_8));
    } catch (InvalidKeySpecException e) {
      throw new IllegalStateException("Unable to generate secret from key spec");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to retrieve PBKDF2WithHmacSHA256 key factory");
    }
  }
}
