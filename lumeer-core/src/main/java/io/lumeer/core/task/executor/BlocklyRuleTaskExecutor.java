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
import io.lumeer.core.task.RuleTask;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

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
         final Set<String> documentIds = new HashSet<>();
         ruleTask.getDaoContextSnapshot().getLinkInstanceDao()
                 .searchLinkInstances(query)
                 .forEach(linkInstance -> {
                    final List<String> documents = linkInstance.getDocumentIds();
                    if (documents.size() >= 2) {
                       if (documents.get(0).equals(d.document.getId())) {
                          documentIds.add(documents.get(1));
                       } else {
                          documentIds.add(documents.get(0));
                       }
                    }
                 });

         // load document data
         final Map<String, DataDocument> data = new HashMap<>();
         ruleTask.getDaoContextSnapshot().getDataDao()
                 .getData(ruleTask.getCollection().getId(), documentIds)
                 .forEach(dd -> data.put(dd.getId(), dd));

         // load document meta data and match them with user data
         return ruleTask.getDaoContextSnapshot().getDocumentDao()
                        .getDocumentsByIds(documentIds.toArray(new String[documentIds.size()]))
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
         changes.forEach(change -> {
            ruleTask.getDaoContextSnapshot().getDataDao()
                    .patchData(change.document.getCollectionId(), change.document.getId(),
                          new DataDocument(change.attrId, change.value));
            // TODO: send push notification
            // TODO: use other types than String for Value
         });
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
      } catch (Exception e) {
         log.log(Level.WARNING, "Unable to execute Blockly Rule on document change: ", e);
         writeTaskError(e);
      }

      if (!rule.isDryRun()) {
         lumeerBridge.commitChanges();
      } else {
         try {
            writeDryRunResults(lumeerBridge.getChanges());
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private void writeTaskError(final Exception e) {
      final Collection originalCollection = ruleTask.getCollection().copy();

      try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
         e.printStackTrace(pw);

         rule.setError(sw.toString());
         rule.setErrorDate(System.currentTimeMillis());
         ruleTask.getCollection().getRules().put(ruleName, rule.getRule());
         ruleTask.getDaoContextSnapshot().getCollectionDao().updateCollection(ruleTask.getCollection().getId(), ruleTask.getCollection(), originalCollection);

         // TODO: send push notification
      } catch (IOException ioe) {
         // we tried, cannot do more
      }
   }

   private void writeDryRunResults(final String results) {
      final Collection originalCollection = ruleTask.getCollection().copy();

      rule.setDryRunResult(results);
      ruleTask.getCollection().getRules().put(ruleName, rule.getRule());
      ruleTask.getDaoContextSnapshot().getCollectionDao().updateCollection(ruleTask.getCollection().getId(), ruleTask.getCollection(), originalCollection);

      // TODO: send push notification
   }
}
