package io.lumeer.core.task.executor;/*
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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.rule.ZapierRule;
import io.lumeer.core.facade.ZapierFacade;
import io.lumeer.core.task.RuleTask;
import io.lumeer.engine.api.data.DataDocument;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class ZapierRuleTaskExecutor {

   private static Logger log = Logger.getLogger(ZapierRuleTaskExecutor.class.getName());

   private String ruleName;
   private ZapierRule rule;
   private RuleTask ruleTask;

   public ZapierRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      this.ruleName = ruleName;
      this.rule = new ZapierRule(ruleTask.getRule());
      this.ruleTask = ruleTask;
   }

   public void execute() {
      final Document document = ruleTask.getNewDocument();
      final Document oldDocument = ruleTask.getOldDocument();
      final Collection collection = ruleTask.getCollection();
      final Map<String, String> attributeNames = collection.getAttributes().stream().map(attribute -> Map.entry(attribute.getId(), attribute.getName())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      final DataDocument doc = new DataDocument(
            document.getData()
                    .entrySet()
                    .stream()
                    .map(entry -> Map.entry(attributeNames.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
      oldDocument.getData()
                 .entrySet()
                 .stream()
                 .forEach(entry -> {
                    final String key = attributeNames.getOrDefault(entry.getKey(), entry.getKey());
                    final Object o = doc.get(key);
                    if ((o == null && entry.getValue() != null) || (o != null && !o.equals(entry.getValue()))) {
                       doc.put(ZapierFacade.CHANGED_PREFIX + key, true);
                    } else {
                       doc.put(ZapierFacade.CHANGED_PREFIX + key, false);
                    }

                    doc.put(ZapierFacade.PREVIOUS_VALUE_PREFIX + key, entry.getValue());
                 });

      final Entity<DataDocument> entity = Entity.json(doc);

      final Client client = ClientBuilder.newClient();
      client.target(rule.getHookUrl()).request(MediaType.APPLICATION_JSON).buildPost(entity).invoke();
      client.close();
   }
}