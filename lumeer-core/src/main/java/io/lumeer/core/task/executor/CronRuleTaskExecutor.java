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

import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.executor.bridge.DocumentBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CronRuleTaskExecutor extends BlocklyRuleTaskExecutor {

   public CronRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      super(ruleName, ruleTask);
   }

   @Override
   protected Map<String, Object> getBindings() {
      final Map<String, Object> bindings = new HashMap<>();

      bindings.put("records", ruleTask.getDocuments().stream().map(DocumentBridge::new).collect(Collectors.toList()));

      return bindings;
   }
}
