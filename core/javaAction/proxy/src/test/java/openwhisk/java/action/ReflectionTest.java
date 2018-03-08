package openwhisk.java.action;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;

public class ReflectionTest {

  @Test
  public void testInvokeFunction() throws Exception {
    String fnClass = "openwhisk.java.action.UpperCase";
    com.google.gson.JsonObject request = new com.google.gson.JsonObject();
    request.addProperty("text", "Hello World!");

    Class clazz = ClassUtils.getClass(fnClass);

    com.google.gson.JsonObject out = (com.google.gson.JsonObject)
            MethodUtils.invokeMethod(clazz.newInstance(), "apply",
                    new Object[]{request},
                    new Class[]{com.google.gson.JsonObject.class});


    System.out.println(out);


  }
}
