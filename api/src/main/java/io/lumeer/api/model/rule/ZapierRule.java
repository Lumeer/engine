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
package io.lumeer.api.model.rule;

import io.lumeer.api.model.Rule;

public class ZapierRule {
   public static final String HOOK_URL = "hookUrl";
   public static final String SUBSCRIBE_ID = "id";

   private final Rule rule;

   public ZapierRule(final Rule rule) {
      this.rule = rule;

      if (rule.getType() != Rule.RuleType.ZAPIER) {
         throw new IllegalArgumentException("Cannot create Zapier Rule from a rule of type " + rule.getType());
      }
   }

   public Rule getRule() {
      return rule;
   }

   public String getHookUrl() {
      return rule.getConfiguration().getString(HOOK_URL);
   }

   public void setHookUrl(final String hookUrl) {
      rule.getConfiguration().put(HOOK_URL, hookUrl);
   }

   public String getSubscribeId() {
      return rule.getConfiguration().getString(SUBSCRIBE_ID);
   }

   public void setSubscribeId(final String subscribeId) {
      rule.getConfiguration().put(SUBSCRIBE_ID, subscribeId);
   }

}
