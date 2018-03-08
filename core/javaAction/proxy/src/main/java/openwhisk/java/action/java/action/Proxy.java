package openwhisk.java.action.java.action;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

public class Proxy extends AbstractVerticle {

  static final String[] OW_ENV_KEYS = {"api_key", "namespace", "action_name", "activation_id", "deadline"};

  private URLClassLoader urlClassLoader;
  private String mainClass;

  @Override
  public void start() throws InterruptedException {
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().failureHandler(ErrorHandler.create());

    router.route("/init").handler(this::initHandler);
    router.route("/run").handler(this::runHandler);

    httpServer
            .requestHandler(router::accept)
            .listen(8080);
  }


  private void initHandler(RoutingContext rc) {

    if (urlClassLoader != null || mainClass != null) {
      rc.response().setStatusCode(500).end("Cannot initialize the action more than once.");
      return;
    }

    JsonObject request = rc.getBodyAsJson();

    JsonObject value = request.getJsonObject("value");
    this.mainClass = value.getString("main");
    byte[] jarBinary = value.getBinary("code");

    JarUtil.writeCodeOnFileSystem(vertx, jarBinary, ar -> {
      if (ar.failed()) {
        rc.fail(ar.cause());
      } else {
        if (!ar.result().toFile().isFile()) {
          //TODO do we need to log ?
          rc.response().setStatusCode(500).end("Error invoking function");
        }
        try {
          urlClassLoader = new URLClassLoader(new URL[]{
                  ar.result().toUri().toURL()});
          rc.response().setStatusCode(200).end("OK");
        } catch (MalformedURLException e) {
          e.printStackTrace();
          rc.response().setStatusCode(500).end("An error has occurred (see logs for details): " + e);
        }

      }
    });
  }

  private void runHandler(RoutingContext rc) {
    JsonObject request = rc.getBodyAsJson();
    JsonObject value = request.getJsonObject("value");

    //Do we need to hold this as this will not be called concurrently
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    SecurityManager sm = System.getSecurityManager();

    final HashMap<String, String> env = new HashMap<>();
    for (String envKey : OW_ENV_KEYS) {
      String val = value.getString(envKey);
      env.put(String.format("__OW_%s", envKey.toUpperCase()), val);
    }

    Thread.currentThread().setContextClassLoader(urlClassLoader);
    System.setSecurityManager(new WhiskSecurityManager());

    try {
      Class clazz = urlClassLoader.loadClass(mainClass);
      //TODO - this is right now modelled to be as its now with JSONObject
      //TODO need to work to make it accept any type via Function parameters
      com.google.gson.JsonObject response = new com.google.gson.JsonObject();
      com.google.gson.JsonObject out = (com.google.gson.JsonObject)
              MethodUtils.invokeMethod(clazz.newInstance(), "apply",
                      new Object[]{request},
                      new Class[]{com.google.gson.JsonObject.class});

      if (out != null) {
        response.addProperty("OK", true);
      } else {
        response.addProperty("OK", false);
      }

      response.add("body", out);

      rc.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json")
              .end(response.toString());
    } catch (ClassNotFoundException e) {
      printAndRespond(rc, e);
    } catch (IllegalAccessException e) {
      printAndRespond(rc, e);
    } catch (NoSuchMethodException e) {
      printAndRespond(rc, e);
    } catch (InstantiationException e) {
      printAndRespond(rc, e);
    } catch (InvocationTargetException e) {
      printAndRespond(rc, e);
    }

  }

  private void printAndRespond(RoutingContext rc, Exception e) {
    e.printStackTrace();
    rc.response().setStatusCode(500).end("An error has occurred (see logs for details): " + e);
  }

  public static void main(String[] args) throws InterruptedException {
    new Proxy().start();
  }

}
