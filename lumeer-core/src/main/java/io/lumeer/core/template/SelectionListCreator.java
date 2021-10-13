package io.lumeer.core.template;/*
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

import io.lumeer.api.model.Project;
import io.lumeer.api.model.SelectOption;
import io.lumeer.api.model.SelectionList;
import io.lumeer.core.facade.SelectionListFacade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelectionListCreator extends WithIdCreator {

   private final SelectionListFacade selectionListFacade;
   private final Project project;

   private SelectionListCreator(final TemplateParser templateParser, final Project project, final SelectionListFacade selectionListFacade) {
      super(templateParser);
      this.project = project;
      this.selectionListFacade = selectionListFacade;
   }

   public static void createLists(final TemplateParser templateParser, final Project project, final SelectionListFacade selectionListFacade) {
      final SelectionListCreator creator = new SelectionListCreator(templateParser, project, selectionListFacade);
      creator.createLists();
   }

   private void createLists() {
      final JSONArray selectionLists = (JSONArray) templateParser.template.get("selectionLists");

      List<SelectionList> currentLists = selectionListFacade.getLists(project.getId());

      if (selectionLists != null) {
         selectionLists.forEach(object -> {
            var json = (JSONObject) object;
            SelectionList selectionList = getSelectionList(json, project.getId());
            String selectionListId = selectionList.getId();

            Optional<SelectionList> foundSelectionList = currentLists.stream().filter(s -> s.getName().equals(selectionList.getName())).findFirst();
            if (foundSelectionList.isEmpty()) {
               var createdList = selectionListFacade.createList(selectionList);

               templateParser.getDict().addSelectionList(selectionListId, createdList);
            }
         });
      }
   }

   private SelectionList getSelectionList(final JSONObject o, final String projectId) {
      final SelectionList selectionList = new SelectionList(
            (String) o.get(SelectionList.ID),
            (String) o.get(SelectionList.NAME),
            (String) o.get(SelectionList.DESCRIPTION),
            null,
            projectId,
            (Boolean) o.get(SelectionList.DISPLAY_VALUES),
            List.of()
      );

      final JSONArray optionsArray = (JSONArray) o.get(SelectionList.OPTIONS);
      if (optionsArray != null) {
         List<SelectOption> options = new ArrayList<>();

         optionsArray.forEach(optionObject -> {
            var option = (JSONObject) optionObject;
            options.add(new SelectOption(option.get(SelectOption.VALUE), option.get(SelectOption.DISPLAY_VALUE), (String) option.get(SelectOption.BACKGROUND)));
         });

         selectionList.setOptions(options);
      }

      return selectionList;
   }
}
