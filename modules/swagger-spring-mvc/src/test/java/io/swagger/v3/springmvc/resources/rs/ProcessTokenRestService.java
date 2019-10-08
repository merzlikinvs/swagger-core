package io.swagger.v3.springmvc.resources.rs;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("token")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProcessTokenRestService extends AbstractEntityRestService<ProcessTokenDTO> {


}
