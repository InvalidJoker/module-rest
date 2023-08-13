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

dependencies {
  compileOnly("eu.cloudnetservice.cloudnet:driver:4.0.0-RC9")

  implementation("com.google.guava:guava:32.1.2-jre")
  implementation("io.netty:netty5-codec-http:5.0.0.Alpha5")
  implementation("io.netty.contrib:netty-codec-haproxy:5.0.0.Alpha2")
}
