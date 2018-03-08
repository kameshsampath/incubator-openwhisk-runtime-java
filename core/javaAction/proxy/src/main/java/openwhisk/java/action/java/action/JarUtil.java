package openwhisk.java.action.java.action;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class JarUtil {

    static void writeCodeOnFileSystem(Vertx vertx, byte[] binary, Handler<AsyncResult<Path>>
        completionHandler) {
        try {
            File file = File.createTempFile("useraction", ".jar");
            vertx.fileSystem().writeFile(file.getAbsolutePath(), Buffer.buffer(binary), res -> {
                if (res.failed()) {
                    completionHandler.handle(Future.failedFuture(res.cause()));
                } else {
                    completionHandler.handle(Future.succeededFuture(file.toPath()));
                }
            });
        } catch (IOException e) {
            completionHandler.handle(Future.failedFuture(e));
        }
    }
}
