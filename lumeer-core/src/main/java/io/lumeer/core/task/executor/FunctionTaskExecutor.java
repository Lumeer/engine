/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.core.task.executor;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.core.task.FunctionTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionTaskExecutor {

   private static Logger log = Logger.getLogger(FunctionTaskExecutor.class.getName());

   private final FunctionTask task;
   private final Document document;
   private final Collection collection;

   public FunctionTaskExecutor(final FunctionTask functionTask, final Collection collection, final Document document) {
      this.task = functionTask;
      this.document = document;
      this.collection = collection;
   }

   public void execute() {
      final JsExecutor.DocumentBridge thisDocument = new JsExecutor.DocumentBridge(document);
      final Map<String, Object> bindings = Map.of("thisDocument", thisDocument);

      final JsExecutor jsExecutor = new JsExecutor();

      try {
         jsExecutor.execute(bindings, task, collection, task.getFunction().getJs());
         jsExecutor.commitChanges();
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to execute function: ", e);
         writeTaskError(e);
      }
   }

   private void writeTaskError(final Exception e) {
      try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
         e.printStackTrace(pw);

         task.getFunction().setErrorReport(sw.toString());
         task.getFunction().setTimestamp(System.currentTimeMillis());

         task.getDaoContextSnapshot().getCollectionDao().updateCollection(collection.getId(), collection, null);

         task.sendPushNotifications(collection);
      } catch (IOException ioe) {
         // we tried, cannot do more
      }
   }
}
