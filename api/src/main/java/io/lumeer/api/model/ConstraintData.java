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
package io.lumeer.api.model;

import java.util.List;
import java.util.Map;

public class ConstraintData {

   private final List<User> users;
   private final User currentUser;
   private final Map<String, String> durationUnitsMap;
   private final CurrencyData currencyData;

   public ConstraintData(final List<User> users, final User currentUser, final Map<String, String> durationUnitsMap, final CurrencyData currencyData) {
      this.users = users;
      this.currentUser = currentUser;
      this.durationUnitsMap = durationUnitsMap;
      this.currencyData = currencyData;
   }

   public List<User> getUsers() {
      return users;
   }

   public User getCurrentUser() {
      return currentUser;
   }

   public Map<String, String> getDurationUnitsMap() {
      return durationUnitsMap;
   }

   public CurrencyData getCurrencyData() {
      return currencyData;
   }
}
