package io.lumeer.core.util;

import io.lumeer.api.model.SelectionList;

import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.List;

public class SelectionListUtils {

   private SelectionListUtils() {
   }

   public static List<SelectionList> appendPredefinedLists(List<SelectionList> lists) {
      var finalLists = new ArrayList<>(getPredefinedLists());
      finalLists.addAll(lists);
      return finalLists;
   }

   public static List<SelectionList> getPredefinedLists() {
      // TODO;
      return Lists.emptyList();
   }
}
