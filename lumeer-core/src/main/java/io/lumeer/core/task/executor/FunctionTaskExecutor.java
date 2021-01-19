/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.task.TaskExecutor;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FunctionTaskExecutor {

   private static Logger log = Logger.getLogger(FunctionTaskExecutor.class.getName());

   private final FunctionTask task;
   private final Document document;
   private final Collection collection;
   private final LinkType linkType;
   private final LinkInstance linkInstance;
   private ChangesTracker changesTracker;

   public FunctionTaskExecutor(final FunctionTask functionTask, final Collection collection, final Document document) {
      this.task = functionTask;
      this.document = document;
      this.collection = collection;
      this.linkInstance = null;
      this.linkType = null;
   }

   public FunctionTaskExecutor(final FunctionTask functionTask, final LinkType linkType, final LinkInstance linkInstance) {
      this.task = functionTask;
      this.document = null;
      this.collection = null;
      this.linkInstance = linkInstance;
      this.linkType = linkType;
   }

   public ChangesTracker execute(final TaskExecutor taskExecutor) {
      changesTracker = new ChangesTracker();
      final JsExecutor.DocumentBridge thisDocument = new JsExecutor.DocumentBridge(document);
      final JsExecutor.LinkBridge thisLink = new JsExecutor.LinkBridge(linkInstance);
      final Map<String, Object> bindings = linkInstance == null ? Map.of("thisRecord", thisDocument, "thisDocument", thisDocument) : Map.of("thisLink", thisLink);

      final JsExecutor jsExecutor = new JsExecutor();

      try {
         jsExecutor.execute(bindings, task, collection, task.getFunction().getJs());
         final ChangesTracker tracker = jsExecutor.commitChanges(taskExecutor);
         checkErrorErasure();

         return tracker;
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to execute function: ", e);
         writeTaskError(e, jsExecutor.getCause());
         jsExecutor.setErrorInAttribute(document, task.getAttribute().getId(), taskExecutor);
      }

      return null;
   }

   private void checkErrorErasure() {
      if (task.getFunction().getTimestamp() > 0 && System.currentTimeMillis() - task.getFunction().getTimestamp() > 3600_000) {
         task.getFunction().setErrorReport("");
         task.getFunction().setTimestamp(0L);

         if (collection != null) {
            task.getDaoContextSnapshot().getCollectionDao().updateCollection(collection.getId(), collection, null);
            // we won't send push notifications as this is not important, it gets updated eventually
         } else if (linkType != null) {
            task.getDaoContextSnapshot().getLinkTypeDao().updateLinkType(linkType.getId(), linkType, null);
         }
      }
   }

   private void writeTaskError(final Exception e, final Exception cause) {
      final StringBuilder sb = new StringBuilder();
      sb.append(getStackTrace(e, 2));

      if (cause != null) {
         sb.append("Caused by\n");
         sb.append(getStackTrace(cause, 10));
      }

      task.getFunction().setErrorReport(sb.toString());
      task.getFunction().setTimestamp(System.currentTimeMillis());

      if (collection != null) {
         task.getDaoContextSnapshot().getCollectionDao().updateCollection(collection.getId(), collection, null);
         changesTracker.updateCollectionsMap(Map.of(collection.getId(), collection));
         changesTracker.addCollections(Set.of(collection));
      } else if (linkType != null) {
         task.getDaoContextSnapshot().getLinkTypeDao().updateLinkType(linkType.getId(), linkType, null);
         changesTracker.updateLinkTypesMap(Map.of(linkType.getId(), linkType));
         changesTracker.addLinkTypes(Set.of(linkType));
      }
   }

   private String getStackTrace(final Exception e, int lines) {
      final StringBuilder sb = new StringBuilder();
      sb.append(e.toString());

      final StackTraceElement[] trace = e.getStackTrace();
      for (int i = 0; i < lines && i < trace.length; i++) {
         sb.append("\tat" + trace[i]);
      }

      return sb.toString();
   }
}
