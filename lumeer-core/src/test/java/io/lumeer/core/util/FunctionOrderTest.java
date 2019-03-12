package io.lumeer.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.function.FunctionParameter;
import io.lumeer.api.model.function.FunctionResourceType;

import org.junit.Test;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class FunctionOrderTest {

   final static FunctionParameter fpA = fp("A"), fpB = fp("B"), fpC = fp("C"),
         fpD = fp("D"), fpE = fp("E"), fpF = fp("F");

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
}