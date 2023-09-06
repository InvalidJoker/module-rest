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

package eu.cloudnetservice.ext.modules.rest.auth.provider;

import eu.cloudnetservice.ext.rest.api.auth.AuthProvider;
import eu.cloudnetservice.ext.rest.jwt.JwtAuthProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class CloudNetJwtAuthProvider extends JwtAuthProvider {

  private static final Path PUBLIC_KEY_PATH = Path.of("rest.key");
  private static final Path PRIVATE_KEY_PATH = Path.of("rest.pem");

  private static final KeyPair RSA_KEY_PAIR;
  private static final KeyFactory RSA_KEY_FACTORY;
  private static final KeyPairGenerator RSA_PAIR_GENERATOR;

  static {
    try {
      RSA_KEY_FACTORY = KeyFactory.getInstance("RSA");
      RSA_PAIR_GENERATOR = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      var publicKey = readPublicKey();
      var privateKey = readPrivateKey();

      // check if we have to generate a new pair
      if (publicKey == null && privateKey == null) {
        RSA_KEY_PAIR = RSA_PAIR_GENERATOR.generateKeyPair();

        // write the generated pair
        writeKey(PUBLIC_KEY_PATH, RSA_KEY_PAIR.getPublic());
        writeKey(PRIVATE_KEY_PATH, RSA_KEY_PAIR.getPrivate());
      } else if (publicKey == null || privateKey == null) {
        throw new RuntimeException("RSA key pair is not complete. Missing one component.");
      } else {
        // we can just use the read ones
        RSA_KEY_PAIR = new KeyPair(publicKey, privateKey);
      }

    } catch (IOException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  public CloudNetJwtAuthProvider() {
    super("CloudNet Rest",
      RSA_KEY_PAIR.getPrivate(),
      RSA_KEY_PAIR.getPublic(),
      Duration.ofHours(12),
      Duration.ofDays(3));
  }

  @Override
  public int priority() {
    return AuthProvider.DEFAULT_PRIORITY + 10;
  }

  private static void writeKey(@NonNull Path path, @NonNull Key key) throws IOException {
    Files.write(path, key.getEncoded(), StandardOpenOption.CREATE);
  }

  private static @Nullable PublicKey readPublicKey() throws IOException, InvalidKeySpecException {
    if (Files.notExists(PUBLIC_KEY_PATH)) {
      return null;
    }

    var bytes = Files.readAllBytes(PUBLIC_KEY_PATH);
    var spec = new X509EncodedKeySpec(bytes);
    return RSA_KEY_FACTORY.generatePublic(spec);
  }

  private static @Nullable PrivateKey readPrivateKey() throws IOException, InvalidKeySpecException {
    if (Files.notExists(PRIVATE_KEY_PATH)) {
      return null;
    }

    var bytes = Files.readAllBytes(PRIVATE_KEY_PATH);
    var spec = new PKCS8EncodedKeySpec(bytes);
    return RSA_KEY_FACTORY.generatePrivate(spec);
  }
}
