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
import io.lumeer.api.model.rule.BlocklyRule;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.task.RuleTask;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.marvec.pusher.data.Event;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class BlocklyRuleTaskExecutor {

   private static Logger log = Logger.getLogger(BlocklyRuleTaskExecutor.class.getName());

   private String ruleName;
   private BlocklyRule rule;
   private RuleTask ruleTask;

   public static class LumeerBridge {

      private RuleTask ruleTask;
      private Set<DocumentChange> changes = new HashSet<>();

      private LumeerBridge(final RuleTask ruleTask) {
         this.ruleTask = ruleTask;
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
         final Set<String> documentIds = ruleTask.getDaoContextSnapshot().getLinkInstanceDao()
                                                  .searchLinkInstances(query)
                                                  .stream()
                                                  .map(linkInstance -> linkInstance.getDocumentIds())
                                                  .flatMap(java.util.Collection::stream)
                                                  .collect(Collectors.toSet());
         documentIds.remove(d.document.getId());

         // load document data
         final Map<String, DataDocument> data = new HashMap<>();
         ruleTask.getDaoContextSnapshot().getDataDao()
                 .getData(ruleTask.getCollection().getId(), documentIds)
                 .forEach(dd -> data.put(dd.getId(), dd));

         // load document meta data and match them with user data
         return ruleTask.getDaoContextSnapshot().getDocumentDao()
                        .getDocumentsByIds(documentIds.toArray(new String[0]))
                        .stream().map(document -> {
                  document.setData(data.get(document.getId()));
                  return new DocumentBridge(document);
               }).collect(Collectors.toList());
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
               final Set<String> users = ruleTask.getDaoContextSnapshot().getCollectionReaders(collectionId);
               final List<Document> documents = ruleTask.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(updatedDocuments.get(collectionId).toArray(new String[0]));
               final List<Event> events = new ArrayList<>();

               users.stream()
                    .forEach(userId ->
                          documents.forEach(doc -> {
                                   final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(doc, ruleTask.getDaoContextSnapshot().getOrganizationId(), ruleTask.getDaoContextSnapshot().getProjectId());
                                   events.add(new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, doc.getClass().getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message));
                                }
                          ));

               ruleTask.getPusherClient().trigger(events);
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

      private DocumentBridge(final Document document) {
         this.document = document;
      }
   }

   public BlocklyRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      this.ruleName = ruleName;
      this.rule = new BlocklyRule(ruleTask.getRule());
      this.ruleTask = ruleTask;
   }

   public void execute() {
      final LumeerBridge lumeerBridge = new LumeerBridge(ruleTask);
      final DocumentBridge oldDocument = new DocumentBridge(ruleTask.getOldDocument());
      final DocumentBridge newDocument = new DocumentBridge(ruleTask.getNewDocument());

      Context context = Context.create("js");
      context.initialize("js");
      context.getPolyglotBindings().putMember("lumeer", lumeerBridge);
      context.getBindings("js").putMember("oldDocument", oldDocument);
      context.getBindings("js").putMember("newDocument", newDocument);

      Timer timer = new Timer(true);
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
            context.close(true);
         }
      }, 3000);

      try {
         context.eval("js", rule.getJs());

         if (!rule.isDryRun()) {
            lumeerBridge.commitChanges();
         } else {
            writeDryRunResults(lumeerBridge.getChanges());
         }
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to execute Blockly Rule on document change: ", e);
         writeTaskError(e);
      }
   }

   private void writeTaskError(final Exception e) {
      try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
         e.printStackTrace(pw);

         rule.setError(sw.toString());
         updateRule();

         // TODO: send push notification
      } catch (IOException ioe) {
         // we tried, cannot do more
      }
   }

   private void writeDryRunResults(final String results) {
      rule.setDryRunResult(results);
      updateRule();

      final Set<String> users = ruleTask.getDaoContextSnapshot().getCollectionManagers(ruleTask.getCollection().getId());
      final List<Event> events = new ArrayList<>();

      if (ruleTask.getPusherClient() != null) {
         users.stream()
              .forEach(userId -> {
                 final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(ruleTask.getCollection(), ruleTask.getDaoContextSnapshot().getOrganizationId(), ruleTask.getDaoContextSnapshot().getProjectId());
                 events.add(new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message));
              });

         ruleTask.getPusherClient().trigger(events);
      }
   }

   private void updateRule() {
      final Collection originalCollection = ruleTask.getCollection().copy();

      rule.setResultTimestamp(System.currentTimeMillis());
      ruleTask.getCollection().getRules().put(ruleName, rule.getRule());
      ruleTask.getDaoContextSnapshot().getCollectionDao().updateCollection(ruleTask.getCollection().getId(), ruleTask.getCollection(), originalCollection);
   }
}
