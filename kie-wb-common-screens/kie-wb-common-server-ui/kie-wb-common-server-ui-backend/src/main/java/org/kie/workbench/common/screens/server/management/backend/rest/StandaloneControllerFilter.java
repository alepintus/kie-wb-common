/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.server.management.backend.rest;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneControllerFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneControllerFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.debug("Standalone controller in use, returning 404/NotFound for request url: {}",
                     requestContext.getUriInfo().getRequestUri().getPath());
        requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build());
    }
}