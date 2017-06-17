package io.lumeer.utils;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RunWith(Parameterized.class)
public class RestImportTest {

   private HttpServer server;
   private Thread serverThread;

   private String location;

   @Parameterized.Parameters(name = "{index}: testRestImport({0})")
   public static Collection<Object[]> parameters() {
      return Arrays.asList(new Object[][] { { "json" }, { "xml" }, { "plain" }, { "array-json" }, { "array-xml" }, { "array-array" } });
   }

   public RestImportTest(final String location) {
      this.location = location;
   }

   @Test
   public void testRestImport() throws Exception {
      final List<RequestDto> messages = new ArrayList<>();

      startServer(messages);

      RestImport.main("-u", "http://127.0.0.1:8091/", "-H", "testHeader=itsValue", getPath(location));

      messages.forEach(System.out::println);

      stopServer();
   }

   private String getPath(final String pathPart) throws URISyntaxException {
      return new File(RestImportTest.class.getResource("/" + pathPart).toURI()).getAbsolutePath();
   }

   private void startServer(final List<RequestDto> messages) {
      final Vertx vertx = Vertx.vertx();
      final HttpServer server = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      final HttpClient client = vertx.createHttpClient();
      router.route("/*").handler(BodyHandler.create());
      router.route("/*").handler((context) -> {
         messages.add(new RequestDto(context.request().method().toString(),context.getBodyAsString(), context.request().headers()));
         context.response().setStatusCode(200).end();
      });

      this.server = server;

      this.serverThread = new Thread(() -> server.requestHandler(router::accept).listen(8091));
      this.serverThread.start();
   }

   private void stopServer() throws InterruptedException {
      server.close();
      serverThread.join(60 * 1000L);

      Assert.assertFalse(serverThread.isAlive());
   }
}