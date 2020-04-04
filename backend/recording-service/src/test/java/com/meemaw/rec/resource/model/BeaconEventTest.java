package com.meemaw.rec.resource.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meemaw.rec.model.beacon.event.AbstractBeaconEvent;
import com.meemaw.rec.model.beacon.event.ClickBeaconEvent;
import com.meemaw.rec.model.beacon.event.MouseMoveBeaconEvent;
import com.meemaw.rec.model.beacon.event.NavigateBeaconEvent;
import com.meemaw.rec.model.beacon.event.PerformanceBeaconEvent;
import com.meemaw.rec.model.beacon.event.ResizeBeaconEvent;
import com.meemaw.rec.model.beacon.event.UnloadBeaconEvent;
import com.meemaw.test.rest.mappers.JacksonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BeaconEventTest {


  @Test
  public void unloadBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\"t\": 1234, \"e\": 1, \"a\": [\"http://localhost:8080\"]}";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(UnloadBeaconEvent.class, deserialized.getClass());

    UnloadBeaconEvent unloadBeaconEvent = (UnloadBeaconEvent) deserialized;
    assertEquals("http://localhost:8080", unloadBeaconEvent.getLocation());
  }

  @Test
  public void resizeBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\"t\": 1234, \"e\": 2, \"a\": [100, 200]}";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(ResizeBeaconEvent.class, deserialized.getClass());

    ResizeBeaconEvent unloadBeaconEvent = (ResizeBeaconEvent) deserialized;
    assertEquals(100, unloadBeaconEvent.getInnerWidth());
    assertEquals(200, unloadBeaconEvent.getInnerHeight());
  }

  @Test
  public void performanceBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\"t\": 1234, \"e\": 3, \"a\": [\"dom\", \"resource\", 0, 200]}";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(PerformanceBeaconEvent.class, deserialized.getClass());

    PerformanceBeaconEvent unloadBeaconEvent = (PerformanceBeaconEvent) deserialized;
    assertEquals("dom", unloadBeaconEvent.getName());
    assertEquals("resource", unloadBeaconEvent.getEntryType());
    assertEquals(0, unloadBeaconEvent.getStartTime());
    assertEquals(200, unloadBeaconEvent.getDuration());
  }

  @Test
  public void navigateBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\"t\": 1234, \"e\": 0, \"a\": [\"http://localhost:8080/test\", \"Test title\"]}";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(NavigateBeaconEvent.class, deserialized.getClass());

    NavigateBeaconEvent beaconEvent = (NavigateBeaconEvent) deserialized;
    assertEquals("http://localhost:8080/test", beaconEvent.getLocation());
    assertEquals("Test title", beaconEvent.getTitle());
  }

  @Test
  public void clickBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\n"
        + "    \"t\": 1306,\n"
        + "    \"e\": 4,\n"
        + "    \"a\": [1167, 732, \"<BUTTON\", \":data-baseweb\", \"button\", \":type\", \"submit\", \":class\", \"__debug-3 as at au av aw ax ay az b0 b1 b2 b3 b4 b5 b6 ak b7 b8 b9 ba bb bc bd be bf bg bh bi an ci ao c8 d8 d9 d7 da ek el em df en eo ep eq bw\"]\n"
        + "  }";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(ClickBeaconEvent.class, deserialized.getClass());

    ClickBeaconEvent beaconEvent = (ClickBeaconEvent) deserialized;
    assertEquals(1167, beaconEvent.getClientX());
    assertEquals(732, beaconEvent.getClientY());
    assertEquals("BUTTON", beaconEvent.getNode());
    assertEquals(List.of("<BUTTON", ":data-baseweb", "button", ":type", "submit", ":class",
        "__debug-3 as at au av aw ax ay az b0 b1 b2 b3 b4 b5 b6 ak b7 b8 b9 ba bb bc bd be bf bg bh bi an ci ao c8 d8 d9 d7 da ek el em df en eo ep eq bw"),
        beaconEvent.getNodeWithAttributes());
  }

  @Test
  public void mouseMoveBeaconEventDeserialization() throws JsonProcessingException {
    String payload = "{\n"
        + "    \"t\": 1306,\n"
        + "    \"e\": 5,\n"
        + "    \"a\": [1167, 732, \"<BUTTON\", \":data-baseweb\", \"button\", \":type\", \"submit\", \":class\", \"__debug-3 as at au av aw ax ay az b0 b1 b2 b3 b4 b5 b6 ak b7 b8 b9 ba bb bc bd be bf bg bh bi an ci ao c8 d8 d9 d7 da ek el em df en eo ep eq bw\"]\n"
        + "  }";
    AbstractBeaconEvent deserialized = JacksonMapper.get()
        .readValue(payload, AbstractBeaconEvent.class);

    assertEquals(MouseMoveBeaconEvent.class, deserialized.getClass());

    MouseMoveBeaconEvent beaconEvent = (MouseMoveBeaconEvent) deserialized;
    assertEquals(1167, beaconEvent.getClientX());
    assertEquals(732, beaconEvent.getClientY());
    assertEquals("BUTTON", beaconEvent.getNode());
    assertEquals(List.of("<BUTTON", ":data-baseweb", "button", ":type", "submit", ":class",
        "__debug-3 as at au av aw ax ay az b0 b1 b2 b3 b4 b5 b6 ak b7 b8 b9 ba bb bc bd be bf bg bh bi an ci ao c8 d8 d9 d7 da ek el em df en eo ep eq bw"),
        beaconEvent.getNodeWithAttributes());
  }
}
