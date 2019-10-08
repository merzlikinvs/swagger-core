package io.swagger.v3.springmvc.petstore.parameter;

import io.swagger.v3.springmvc.resources.model.User;
import io.swagger.v3.oas.annotations.Operation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Class with a single not annotated parameter.
 */
public class SingleNotAnnotatedParameter {
    @GET
    @Path("/singlenoannotatedparameter")
    @Operation(operationId = "create User")
    public User findUser(final String id) {
        return new User();
    }
}
