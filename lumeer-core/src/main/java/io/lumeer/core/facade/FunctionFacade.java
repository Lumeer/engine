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
package io.lumeer.core.facade;

import io.lumeer.api.model.function.Function;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.storage.api.dao.FunctionDao;

import java.util.Collections;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class FunctionFacade extends AbstractFacade {

   @Inject
   private FunctionDao functionDao;

   public void onCreateFunction(Function function) {
      List<FunctionRow> functionRows = parseXml(function.getXml());

      functionDao.createRows(functionRows);
      computeFunctionValues(functionRows);
   }

   private List<FunctionRow> parseXml(String xml) {
      // TODO parse
      return Collections.emptyList();
   }

   private void computeFunctionValues(List<FunctionRow> rows){
      // TODO use rows to create computing chain
   }

   public void onUpdateFunction(String collectionId, String attributeId, Function function) {
      functionDao.deleteByCollection(collectionId, attributeId);
      onCreateFunction(function);
   }

   public void onDeleteFunction(String collectionId, String attributeId) {
      functionDao.deleteByCollection(collectionId, attributeId);
   }

   public void onDocumentValueChanged(String collectionId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentCollection(collectionId, attributeId);

      computeFunctionValues(functionRows);
   }

   public void onLinkValueChanged(String linkTypeId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkTypeId, attributeId);

      computeFunctionValues(functionRows);
   }

   public void onDeleteColection(String collectionId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyCollection(collectionId, null);

      deleteByRows(functionRows);
   }

   public void onDeleteLinkType(String linkTypeId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkTypeId, null);

      deleteByRows(functionRows);
   }

   public void onDeleteCollectionAttribute(String collectionId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyCollection(collectionId, attributeId);

      deleteByRows(functionRows);
   }

   public void onDeleteLinkAttribute(String linkTypeId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkTypeId, attributeId);

      deleteByRows(functionRows);
   }

   private void deleteByRows(List<FunctionRow> functionRows) {
      String[] collectionIdsToDelete = functionRows.stream().map(FunctionRow::getCollectionId).toArray(String[]::new);
      functionDao.deleteByCollections(collectionIdsToDelete);
   }

}
