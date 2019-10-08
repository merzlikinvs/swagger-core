package io.swagger.v3.springmvc.integration.api;

import io.swagger.v3.oas.integration.api.OpenApiContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public interface WebOpenApiContext extends OpenApiContext {

    ServletContext getServletContext();

    ServletConfig getServletConfig();

}
