package io.lumeer.core.util;

import io.lumeer.api.model.SelectOption;
import io.lumeer.api.model.SelectionList;

import java.util.ArrayList;
import java.util.Collections;
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
      /*
      Normal - Open, In Progress, Closed
      Kanban - Open, In Progress, Review, Closed
      Content - Open, Ready, Writing, Approval, Rejected, Publish, Closed
      Marketing - Open, Concept, In Progress, Running, Review, Closed
      Scrum - Open, Pending, In Progress, Completed, In Review, Accepted, Rejected, Blocked, Closed
      */
      // TODO;
      return Collections.emptyList();
   }
}
