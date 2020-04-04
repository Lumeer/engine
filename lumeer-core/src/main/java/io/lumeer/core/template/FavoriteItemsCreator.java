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

import io.lumeer.core.facade.CollectionFacade;
import io.lumeer.core.facade.ViewFacade;

import org.json.simple.JSONArray;

public class FavoriteItemsCreator extends WithIdCreator {

   final private CollectionFacade collectionFacade;
   final private ViewFacade viewFacade;

   private FavoriteItemsCreator(final TemplateParser templateParser, final CollectionFacade collectionFacade, final ViewFacade viewFacade) {
      super(templateParser);
      this.collectionFacade = collectionFacade;
      this.viewFacade = viewFacade;
   }

   public static void createFavoriteItems(final TemplateParser templateParser, final CollectionFacade collectionFacade, final ViewFacade viewFacade) {
      final FavoriteItemsCreator creator = new FavoriteItemsCreator(templateParser, collectionFacade, viewFacade);
      creator.createFavoriteItems();
   }

   private void createFavoriteItems() {
      final JSONArray favoriteCollections = (JSONArray) templateParser.template.get("favoriteCollections");
      final JSONArray favoriteViews = (JSONArray) templateParser.template.get("favoriteViews");

      if (favoriteCollections != null) {
         favoriteCollections.forEach(o -> {
            var id = templateParser.getDict().getCollectionId(o.toString());
            if (id != null) {
               collectionFacade.addFavoriteCollection(id);
            }
         });
      }

      if (favoriteViews != null) {
         favoriteViews.forEach(o -> {
            var id = templateParser.getDict().getViewId(o.toString());
            if (id != null) {
               viewFacade.addFavoriteView(id);
            }
         });
      }
   }

}
