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
package io.lumeer.core.util;

import io.lumeer.api.model.function.FunctionParameter;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FunctionOrder {

   public static <T extends FunctionParameter> Deque<T> orderFunctions(final Map<T, List<T>> dependencies) {
      final Deque<T> result = new LinkedList<>();

      dependencies.keySet().forEach(param -> {
         final Set<T> visited = new HashSet<>(Set.of(param)); //new HashSet<>();
         findParent(result, param, visited, dependencies);
      });

      dependencies.keySet().forEach(param -> {
         if (!result.contains(param)) {
            result.add(param);
         }
      });

      result.removeIf(param -> !dependencies.containsKey(param));

      return result;
   }

   private static <T extends FunctionParameter> void findParent(final Deque<T> result, final T root, final Set<T> visited, final Map<T, List<T>> dependencies) {
      List<T> depNodes = dependencies.get(root);

      if (depNodes != null) {
         depNodes.forEach(param -> {
            if (!visited.contains(param) && !result.contains(param) && !param.equals(root)) {
               visited.add(param);
               if (dependencies.containsKey(param)) {
                  findParent(result, param, visited, dependencies);
               } else {
                  result.add(param);
               }
            }
         });
      }
      if (!result.contains(root)) {
         result.add(root);
      }
   }

}
