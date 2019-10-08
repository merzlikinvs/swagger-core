package io.swagger.v3.springmvc.resources;

import javax.ws.rs.Path;

@Path("/v1")
public class ClassPathParentResource {
    @Path("parent")
    public ClassPathSubResource getSubResource() {
        return new ClassPathSubResource();
    }
}