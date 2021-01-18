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
package io.lumeer.api.model.constraint;

import java.util.HashMap;
import java.util.Map;

public abstract class ConstraintConfig {

   public Map<String, Object> parseConfig(Object config) {
      try {
         return (Map<String, Object>) config;
      } catch (ClassCastException exception) {
         return new HashMap<>();
      }
   }

   public Integer parseInt(Map<String, Object> config, String key) {
      if (config != null && config.containsKey(key)) {
         try {
            return Integer.valueOf(config.get(key).toString());
         } catch (NumberFormatException exception) {
            return null;
         }
      }
      return null;
   }
}
