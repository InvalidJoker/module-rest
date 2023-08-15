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

package eu.cloudnetservice.ext.rest.http;

/**
 * The version of a http request, currently only http 1.X is supported by CloudNet.
 *
 * @since 4.0
 */
public enum HttpVersion {

  /**
   * Http version 1.0
   */
  HTTP_1_0,
  /**
   * Http version 1.1
   */
  HTTP_1_1
}