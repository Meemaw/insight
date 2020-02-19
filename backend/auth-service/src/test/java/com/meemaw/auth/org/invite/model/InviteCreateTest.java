package com.meemaw.auth.org.invite.model;

import static com.meemaw.test.matchers.SameJSON.sameJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meemaw.auth.org.invite.model.dto.InviteCreateDTO;
import com.meemaw.auth.user.model.UserRole;
import com.meemaw.test.rest.mappers.JacksonMapper;
import org.junit.jupiter.api.Test;

public class InviteCreateTest {

  @Test
  public void dtoJacksonSerialization() throws JsonProcessingException {
    InviteCreateDTO teamInviteCreate = new InviteCreateDTO("test@gmail.com",
        UserRole.STANDARD);

    String payload = JacksonMapper.get().writeValueAsString(teamInviteCreate);
    assertThat(payload, sameJson(
        "{\"email\":\"test@gmail.com\",\"role\":\"STANDARD\"}"));

    InviteCreateDTO deserialized = JacksonMapper.get()
        .readValue(payload, InviteCreateDTO.class);
    assertEquals(teamInviteCreate, deserialized);
    assertEquals(payload, JacksonMapper.get().writeValueAsString(deserialized));
  }

}