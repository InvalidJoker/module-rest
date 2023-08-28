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

package eu.cloudnetservice.ext.modules.rest.v2;

import eu.cloudnetservice.ext.rest.api.HttpResponseCode;
import eu.cloudnetservice.ext.rest.api.annotation.RequestHandler;
import eu.cloudnetservice.ext.rest.api.annotation.RequestPath;
import eu.cloudnetservice.ext.rest.api.problem.ProblemDetail;
import eu.cloudnetservice.ext.rest.api.response.IntoResponse;
import eu.cloudnetservice.ext.rest.api.response.type.InputStreamResponse;
import eu.cloudnetservice.ext.rest.api.response.type.PlainTextResponse;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import lombok.NonNull;

//TODO: this might be totally yek
@Singleton
public final class V2HttpHandlerDocumentation {

  @RequestHandler(path = "/api/v2/documentation")
  public @NonNull IntoResponse<?> handleDocumentationRequest() {
    return PlainTextResponse.builder()
      .responseCode(HttpResponseCode.MOVED_PERMANENTLY)
      .location(URI.create("/api/v2/documentation/index.html"));
  }

  @RequestHandler(path = "/api/v2/documentation/*")
  public @NonNull IntoResponse<?> handleDocumentationFileRequest(@NonNull @RequestPath String path) throws IOException {
    var filePath = path.replaceFirst("/api/v2/", "");
    if (filePath.contains("..")) {
      return ProblemDetail.builder()
        .type("file-browsing-is-forbidden")
        .title("File Browsing Is Forbidden")
        .status(HttpResponseCode.BAD_REQUEST)
        .detail("File browsing is not allowed.");
    }

    // get the resource and the content type of it
    var resource = V2HttpHandlerDocumentation.class.getClassLoader().getResource(filePath);
    if (resource == null) {
      return ProblemDetail.builder()
        .type("documentation-not-found")
        .title("Documentation Not Found")
        .status(HttpResponseCode.NOT_FOUND)
        .detail("The requested documentation file was not found.");
    }

    return InputStreamResponse.builder().body(resource.openStream());
  }

}
