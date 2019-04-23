/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.core.template;

import io.lumeer.api.model.LinkInstance;
import io.lumeer.core.facade.LinkInstanceFacade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class LinkInstanceCreator extends WithIdCreator {

   private final LinkInstanceFacade linkInstanceFacade;

   private LinkInstanceCreator(final TemplateParser templateParser, final LinkInstanceFacade linkInstanceFacade) {
      super(templateParser);
      this.linkInstanceFacade = linkInstanceFacade;
   }

   public static void createLinkInstances(final TemplateParser templateParser, final LinkInstanceFacade linkInstanceFacade) {
      final LinkInstanceCreator creator = new LinkInstanceCreator(templateParser, linkInstanceFacade);
      creator.createLinkInstances();
   }

   private void createLinkInstances() {
      JSONArray a = (JSONArray) templateParser.getTemplate().get("linkInstances");
      a.forEach(link -> {
         var linkObj = (JSONObject) link;
         var linkTemplateId = TemplateParserUtils.getId(linkObj);
         var linkTypeTemplateId = (String) linkObj.get("linkTypeId");
         var linkInstance = new LinkInstance((String) templateParser.getDict().getLinkTypeId(linkTypeTemplateId), getDocumentIds(linkObj));
         linkInstance = linkInstanceFacade.createLinkInstance(linkInstance);
         templateParser.getDict().addLinkInstance(linkTemplateId, linkInstance);

         getLinkData(linkInstance, linkTemplateId, linkTypeTemplateId);
         linkInstance = linkInstanceFacade.updateLinkInstanceData(linkInstance.getId(), linkInstance.getData());
      });
   }

   @SuppressWarnings("unchecked")
   private List<String> getDocumentIds(JSONObject o) {
      var ids = new ArrayList<String>();
      ((JSONArray) o.get("documentIds")).forEach(id -> ids.add(templateParser.getDict().getDocumentId((String) id)));

      return ids;
   }

   @SuppressWarnings("unchecked")
   private LinkInstance getLinkData(final LinkInstance linkInstance, final String linkTemplateId, final String linkTypeTemplateId) {
      final Optional<JSONObject> data = ((JSONArray) ((JSONObject) templateParser.getTemplate().get("linkData")).get(linkTypeTemplateId)).stream().filter(d -> linkTemplateId.equals(TemplateParserUtils.getId((JSONObject) d))).findFirst();

      if (data.isPresent() && data.get().size() > 1) {
         linkInstance.setData(translateDataDocument(templateParser.getDict().getLinkType(linkTypeTemplateId), data.get()));
      }

      return linkInstance;
   }
}
