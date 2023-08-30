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

plugins {
  id("eu.cloudnetservice.juppiter") version "0.2.0"
}

dependencies {
  api(projects.webApi)
  implementation(projects.webImplNetty)
  implementation(projects.webCodecGson)
  compileOnlyApi("eu.cloudnetservice.cloudnet:node:4.0.0-RC9")
}

moduleJson {
  main = "eu.cloudnetservice.ext.rest.CloudNetRestModule"
  name = "CloudNet-Rest2"
  version = "1.0"
  author = "CloudNetService"
}
