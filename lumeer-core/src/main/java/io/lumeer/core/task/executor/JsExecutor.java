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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JsExecutor {

   private static Logger log = Logger.getLogger(JsExecutor.class.getName());

   private LumeerBridge lumeerBridge;

   public static class LumeerBridge {

      private ContextualTask ruleTask;
      private Collection collection;
      private Set<DocumentChange> changes = new HashSet<>();

      private LumeerBridge(final ContextualTask task, final Collection collection) {
         this.ruleTask = task;
         this.collection = collection;
      }

      public void setDocumentAttribute(DocumentBridge d, String attrId, Value value) {
         changes.add(new DocumentChange(d.document, attrId, convertValue(value)));
      }

      Object convertValue(final Value value) {
         if (value.isNumber()) {
            return value.fitsInLong() ? value.asLong() : value.asDouble();
         } else if (value.isBoolean()) {
            return value.asBoolean();
         } else if (value.isNull()) {
            return null;
         } else if (value.hasArrayElements()) {
            final List list = new ArrayList();
            for (long i = 0; i < value.getArraySize(); i = i + 1) {
               list.add(convertValue(value.getArrayElement(i)));
            }
            return list;
         } else {
            return value.asString();
         }
      }

      public List<DocumentBridge> getLinkedDocuments(DocumentBridge d, String linkTypeId) {
         final LinkType linkType = ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId);
         final String otherCollectionId = linkType.getCollectionIds().get(0).equals(collection.getId()) ?
               linkType.getCollectionIds().get(1) : linkType.getCollectionIds().get(0);

         final SearchQuery query = SearchQuery
               .createBuilder()
               .stems(Arrays.asList(
                     SearchQueryStem
                           .createBuilder("")
                           .linkTypeIds(Arrays.asList(linkTypeId))
                           .documentIds(Set.of(d.document.getId()))
                           .build()))
               .build();

         // load linked document ids
         final List<LinkInstance> links = ruleTask.getDaoContextSnapshot().getLinkInstanceDao()
                                                  .searchLinkInstances(query);

         if (links.size() > 0) {
            final Set<String> documentIds = links.stream()
                                                 .map(linkInstance -> linkInstance.getDocumentIds())
                                                 .flatMap(java.util.Collection::stream)
                                                 .collect(Collectors.toSet());
            documentIds.remove(d.document.getId());

            // load document data
            final Map<String, DataDocument> data = new HashMap<>();
            ruleTask.getDaoContextSnapshot().getDataDao()
                    .getData(otherCollectionId, documentIds)
                    .forEach(dd -> data.put(dd.getId(), dd));

            // load document meta data and match them with user data
            return ruleTask.getDaoContextSnapshot().getDocumentDao()
                           .getDocumentsByIds(documentIds.toArray(new String[0]))
                           .stream().map(document -> {
                     document.setData(data.get(document.getId()));
                     return new DocumentBridge(document);
                  }).collect(Collectors.toList());
         } else {
            return Collections.emptyList();
         }
      }

      public Value getDocumentAttribute(DocumentBridge d, String attrId) {
         return Value.asValue(d.document.getData().get(attrId));
      }

      public List<Value> getDocumentAttribute(List<DocumentBridge> docs, String attrId) {
         final List<Value> result = new ArrayList<>();
         docs.forEach(doc -> result.add(Value.asValue(doc.document.getData().get(attrId))));

         return result;
      }

      void commitChanges() {
         final Map<String, List<String>> updatedDocuments = new HashMap<>(); // Collection -> [Document]

         changes.forEach(change -> {
            ruleTask.getDaoContextSnapshot().getDataDao()
                    .patchData(change.document.getCollectionId(), change.document.getId(),
                          new DataDocument(change.attrId, change.value));
            updatedDocuments.computeIfAbsent(change.document.getCollectionId(), key -> new ArrayList<String>())
                            .add(change.document.getId());
         });

         // send push notification
         if (ruleTask.getPusherClient() != null) {
            updatedDocuments.keySet().forEach(collectionId -> {
               ruleTask.sendPushNotifications(collectionId, updatedDocuments.get(collectionId));
            });
         }
      }

      String getChanges() {
         final Map<String, Collection> collections = new HashMap<>();
         final StringBuilder sb = new StringBuilder("");

         changes.forEach(change -> {
            final Collection collection = collections.computeIfAbsent(change.document.getCollectionId(), id -> ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));

            sb.append(collection.getName() + "(" + last4(change.document.getId()) + "): ");
            sb.append(collection.getAttributes().stream().filter(a -> a.getId().equals(change.attrId)).map(a -> a.getName()).findFirst().orElse(""));
            sb.append(" = ");
            sb.append(change.value);
            sb.append("\n");
         });

         return sb.toString();
      }

      private String last4(final String str) {
         if (str.length() <= 4) {
            return str;
         }
         return str.substring(str.length() - 4);
      }
   }

   public static class DocumentChange {
      private final Document document;
      private final String attrId;
      private final Object value;

      public DocumentChange(final Document document, final String attrId, final Object value) {
         this.document = document;
         this.attrId = attrId;
         this.value = value;
      }

      public Document getDocument() {
         return document;
      }

      public String getAttrId() {
         return attrId;
      }

      public Object getValue() {
         return value;
      }

      @Override
      public String toString() {
         return "DocumentChange{" +
               "document=" + document.getId() +
               ", attrId='" + attrId + '\'' +
               ", value=" + value +
               '}';
      }
   }

   public static class DocumentBridge {
      private Document document;

      DocumentBridge(final Document document) {
         this.document = document;
      }

      @Override
      public String toString() {
         return "DocumentBridge{" +
               "document=" + document +
               '}';
      }
   }

   public void execute(final Map<String, Object> bindings, final ContextualTask task, final Collection collection, final String js) {
      lumeerBridge = new LumeerBridge(task, collection);

      Context context = Context.newBuilder("js").engine(Engine.newBuilder().option("js.experimental-array-prototype", "true").build()).build();
      context.initialize("js");
      context.getPolyglotBindings().putMember("lumeer", lumeerBridge);

      bindings.forEach((k, v) -> context.getBindings("js").putMember(k, v));

      Timer timer = new Timer(true);
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
            context.close(true);
         }
      }, 3000);

      context.eval("js", js);
   }

   public void commitChanges() {
      lumeerBridge.commitChanges();
   }

   public String getChanges() {
      return lumeerBridge.getChanges();
   }

}
