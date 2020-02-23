package com.meemaw.auth.sso.resource.v1;

import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path(SsoGoogleResource.PATH)
public interface SsoGoogleResource {

  String PATH = SsoResource.PATH + "/google";

  @GET
  @Path("signin")
  Response signin(@NotBlank(message = "dest is required") @QueryParam("dest") String dest);

  @GET
  @Path("oauth2callback")
  CompletionStage<Response> oauth2callback(
      @NotBlank(message = "state is required") @QueryParam("state") String state,
      @NotBlank(message = "code is required") @QueryParam("code") String code);

}
