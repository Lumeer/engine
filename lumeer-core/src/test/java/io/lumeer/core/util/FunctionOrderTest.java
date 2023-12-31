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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.function.FunctionParameter;
import io.lumeer.api.model.function.FunctionResourceType;

import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FunctionOrderTest {

   final static FunctionParameter fpA = fp("A"), fpB = fp("B"), fpC = fp("C"),
         fpD = fp("D"), fpE = fp("E"), fpF = fp("F"), fpG = fp("G"), fpH = fp("H"),
         fpI = fp("I");

   private static FunctionParameter fp(String id) {
      return new FunctionParameter(FunctionResourceType.COLLECTION, "r1", id);
   }

   @Test
   public void orderFunctions1() {
      final Map<FunctionParameter, List<FunctionParameter>> input =
            Map.of(
                  fpA, List.of(fpB, fpC, fpD),
                  fpC, List.of(fpD, fpE, fpF),
                  fpD, List.of(fpB, fpE),
                  fpB, List.of(fpE, fpF)
            );
      Deque<FunctionParameter> result = FunctionOrder.orderFunctions(input);
      assertThat(result).containsSequence(fpB, fpD, fpC, fpA);
      assertThat(result).hasSize(input.size());
   }

   @Test
   public void orderFunctions2() {
      final Map<FunctionParameter, List<FunctionParameter>> input =
            Map.of(
                  fpC, List.of(fpA, fpB),
                  fpD, List.of(fpB, fpC),
                  fpE, List.of(fpA, fpB)
            );
      Deque<FunctionParameter> result = FunctionOrder.orderFunctions(input);
      assertThat(result).containsSubsequence(fpC, fpD);
      assertThat(result).hasSize(input.size());
   }

   @Test
   public void orderFunctions3() {
      final Map<FunctionParameter, List<FunctionParameter>> input =
            Map.of(
                  fpC, List.of(fpB),
                  fpD, List.of(fpC, fpB),
                  fpE, List.of(fpC, fpD)
            );
      Deque<FunctionParameter> result = FunctionOrder.orderFunctions(input);
      assertThat(result).containsSequence(fpC, fpD, fpE);
      assertThat(result).hasSize(input.size());
   }

   @Test
   public void orderFunctions4() {
      final Map<FunctionParameter, List<FunctionParameter>> input =
            new HashMap<>(Map.of(
                  fpC, List.of(fpB),
                  fpD, List.of(fpC, fpB),
                  fpE, List.of(fpC, fpD),
                  fpB, List.of(fpE, fpA)
            ));
      Deque<FunctionParameter> result = FunctionOrder.orderFunctions(input);

      List<FunctionParameter> twoResults = new LinkedList<>(result);
      twoResults.addAll(result);

      assertThat(twoResults).containsSequence(fpD, fpE, fpB, fpC);
      assertThat(result).hasSize(input.size());
   }

   @Test
   public void orderFunctions5() {
      final Map<FunctionParameter, List<FunctionParameter>> input =
            new HashMap<>(Map.of(
                  fpA, List.of(fpB, fpC),
                  fpD, List.of(fpG, fpH),
                  fpG, List.of(fpI),
                  fpB, List.of(fpE, fpF)
            ));
      Deque<FunctionParameter> result = FunctionOrder.orderFunctions(input);

      // B precedes A, G precedes D
      boolean wasG = false, wasB = false;
      Iterator<FunctionParameter> i = result.iterator();
      while (i.hasNext()) {
         FunctionParameter fp = i.next();

         if (fp.equals(fpG)) {
            wasG = true;
         } else if (fp.equals(fpB)) {
            wasB = true;
         } else if (fp.equals(fpA)) {
            assertThat(wasB).isTrue();
         } else if (fp.equals(fpD)) {
            assertThat(wasG).isTrue();
         }
      }

      // we had both values
      assertThat(wasB).isTrue();
      assertThat(wasG).isTrue();

      assertThat(result).hasSize(4);
   }
}
