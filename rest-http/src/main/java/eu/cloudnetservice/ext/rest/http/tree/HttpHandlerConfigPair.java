package eu.cloudnetservice.ext.rest.http.tree;

import eu.cloudnetservice.ext.rest.http.HttpHandler;
import eu.cloudnetservice.ext.rest.http.config.HttpHandlerConfig;
import lombok.NonNull;

public record HttpHandlerConfigPair(@NonNull HttpHandler httpHandler, @NonNull HttpHandlerConfig config) {

}
