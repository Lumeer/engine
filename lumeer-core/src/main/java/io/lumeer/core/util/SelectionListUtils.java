package io.lumeer.core.util;

import io.lumeer.api.model.Language;
import io.lumeer.api.model.SelectOption;
import io.lumeer.api.model.SelectionList;

import java.util.List;
import java.util.stream.Collectors;

public class SelectionListUtils {

   private SelectionListUtils() {
   }

   public static List<SelectionList> getPredefinedLists(Language language, String organizationId, String projectId) {
      return List.of(
            new SelectionList(null, translateSelectionType(language, SelectionType.General), "", organizationId, projectId, false, translateOptionTypes(language, List.of(OptionType.Open, OptionType.InProgress, OptionType.Closed))),
            new SelectionList(null, translateSelectionType(language, SelectionType.Kanban), "", organizationId, projectId, false, translateOptionTypes(language, List.of(OptionType.Open, OptionType.InProgress, OptionType.Review, OptionType.Closed))),
            new SelectionList(null, translateSelectionType(language, SelectionType.Content), "", organizationId, projectId, false, translateOptionTypes(language, List.of(OptionType.Open, OptionType.Ready, OptionType.Writing, OptionType.Approved, OptionType.Rejected, OptionType.Publish, OptionType.Closed))),
            new SelectionList(null, translateSelectionType(language, SelectionType.Marketing), "", organizationId, projectId, false, translateOptionTypes(language, List.of(OptionType.Open, OptionType.Concept, OptionType.InProgress, OptionType.Running, OptionType.Review, OptionType.Closed))),
            new SelectionList(null, translateSelectionType(language, SelectionType.Scrum), "", organizationId, projectId, false, translateOptionTypes(language, List.of(OptionType.Open, OptionType.Pending, OptionType.InProgress, OptionType.Completed, OptionType.InReview, OptionType.Accepted, OptionType.Rejected, OptionType.Blocked, OptionType.Closed)))
      );
   }

   private static String translateSelectionType(Language language, SelectionType type) {
      switch (language) {
         case CS:
            return translateSelectionTypeCs(type);
         default:
            return translateSelectionTypeEn(type);
      }
   }

   private static String translateSelectionTypeEn(SelectionType type) {
      switch (type) {
         case General:
            return "General";
         case Kanban:
            return "Kanban";
         case Content:
            return "Content";
         case Marketing:
            return "Marketing";
         case Scrum:
            return "Scrum";
         default:
            return type.toString();
      }
   }

   private static String translateSelectionTypeCs(SelectionType type) {
      switch (type) {
         case General:
            return "Základní";
         case Kanban:
            return "Nástenka";
         case Content:
            return "Obsah";
         case Marketing:
            return "Marketing";
         case Scrum:
            return "Scrum";
         default:
            return type.toString();
      }
   }

   private static List<SelectOption> translateOptionTypes(Language language, List<OptionType> types) {
      return types.stream()
                  .map(type -> new SelectOption(translateOptionType(language, type), null, optionTypeBackground(type)))
                  .collect(Collectors.toList());
   }

   private static String optionTypeBackground(OptionType type) {
      switch (type) {
         case Open:
            return "#616161";
         case InProgress:
         case Running:
         case Writing:
            return "#fff176";
         case Closed:
         case Completed:
            return "#9575cd";
         case Review:
         case Ready:
         case InReview:
            return "#ff8a65";
         case Approved:
         case Accepted:
            return "#81c784";
         case Rejected:
         case Blocked:
            return "#e57373";
         case Publish:
         case Concept:
            return "#7986cb";
         case Pending:
            return "#aed581";
         default:
            return type.toString();
      }
   }

   private static String translateOptionType(Language language, OptionType type) {
      switch (language) {
         case CS:
            return translateOptionTypeCs(type);
         default:
            return translateOptionTypeEn(type);
      }
   }

   private static String translateOptionTypeCs(OptionType type) {
      switch (type) {
         case Open:
            return "Otevřený";
         case InProgress:
            return "Probíhá";
         case Closed:
            return "Zavřeno";
         case Review:
            return "Posouzení";
         case Ready:
            return "Připraveno";
         case Writing:
            return "Psaní";
         case Approved:
            return "Schváleno";
         case Rejected:
            return "Zamítnuto";
         case Publish:
            return "Publikovat";
         case Concept:
            return "Koncept";
         case Running:
            return "Běží";
         case Completed:
            return "Dokončeno";
         case Pending:
            return "Čekající";
         case InReview:
            return "Kontrola";
         case Accepted:
            return "Přijato";
         case Blocked:
            return "Blokováno";
         default:
            return type.toString();
      }
   }

   private static String translateOptionTypeEn(OptionType type) {
      switch (type) {
         case Open:
            return "Open";
         case InProgress:
            return "In Progress";
         case Closed:
            return "Closed";
         case Review:
            return "Review";
         case Ready:
            return "Ready";
         case Writing:
            return "Writing";
         case Approved:
            return "Approved";
         case Rejected:
            return "Rejected";
         case Publish:
            return "Publish";
         case Concept:
            return "Concept";
         case Running:
            return "Running";
         case Completed:
            return "Completed";
         case Pending:
            return "Pending";
         case InReview:
            return "In Review";
         case Accepted:
            return "Accepted";
         case Blocked:
            return "Blocked";
         default:
            return type.toString();
      }
   }

   private enum SelectionType {
      General,
      Kanban,
      Content,
      Marketing,
      Scrum
   }

   private enum OptionType {
      Open,
      InProgress,
      Closed,
      Review,
      Ready,
      Writing,
      Approved,
      Rejected,
      Publish,
      Concept,
      Running,
      Pending,
      Completed,
      InReview,
      Accepted,
      Blocked
   }

}
