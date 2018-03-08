package openwhisk.java.action;

import com.google.gson.JsonObject;

import java.util.function.Function;

/**
 * Simple function to make it stay with {@link JsonObject} signature
 */
public class UpperCase implements Function<JsonObject, JsonObject> {
  @Override
  public JsonObject apply(JsonObject json) {
    JsonObject response = new JsonObject();
    response.addProperty("result", new Util().uppperAndReverse(json));
    return response;
  }

  private static class Util {
    public String uppperAndReverse(JsonObject json) {
      String s = json.getAsJsonPrimitive("text").getAsString();

      return new StringBuilder(s)
              .reverse()
              .toString()
              .toUpperCase();
    }
  }
}
