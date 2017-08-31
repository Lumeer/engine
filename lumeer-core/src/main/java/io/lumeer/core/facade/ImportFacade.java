/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.core.facade;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import com.univocity.parsers.common.processor.RowListProcessor;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ImportFacade extends AbstractFacade {

   public static final String FORMAT_CSV = "csv";

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   public Collection importDocuments(final String format, final String name, final String data) {
      List<Document> documents;

      switch (format.toLowerCase()) {
         case FORMAT_CSV:
            documents = parseCSVFile(data);
            break;
         default:
            documents = Collections.emptyList();
      }

      Collection collection = createImportCollection(name);
      if (documents.isEmpty()) {
         return collection;
      }

      documents.forEach(doc -> addDocumentMetadata(collection, doc));

      List<Document> storedDocuments = documentDao.createDocuments(documents);

      List<DataDocument> dataDocuments = new LinkedList<>();
      for (int i = 0; i < documents.size(); i++) {
         DataDocument dataDocument = documents.get(i).getData();
         dataDocument.setId(storedDocuments.get(i).getId());
         dataDocuments.add(dataDocument);
      }
      dataDao.createData(collection.getId(), dataDocuments);

      return collection;
   }

   private void addDocumentMetadata(Collection collection, Document document) {
      document.setCollectionId(collection.getId());
      document.setCreatedBy(authenticatedUser.getCurrentUsername());
      document.setCreationDate(LocalDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
   }

   private Collection createImportCollection(String name) {
      checkProjectWriteRole();
      Set<String> codes = collectionDao.getAllCollectionCodes();

      String collectionName = name != null && !name.isEmpty() ? name : "Import";

      String code = generateCollectionCode(collectionName, codes);
      JsonPermissions collectionPermissions = new JsonPermissions();
      collectionPermissions.updateUserPermissions(new JsonPermission(authenticatedUser.getCurrentUsername(), Role.toStringRoles(Collection.ROLES)));
      JsonCollection collection = new JsonCollection(code, collectionName, null, null, collectionPermissions);

      Collection storedCollection = collectionDao.createCollection(collection);
      dataDao.createDataRepository(storedCollection.getId());

      return storedCollection;
   }

   private String generateCollectionCode(String collectionName, Set<String> existingCodes) {
      return CodeGenerator.generate(existingCodes, collectionName);
   }

   private void checkProjectWriteRole() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      Project project = workspaceKeeper.getProject().get();
      permissionsChecker.checkRole(project, Role.WRITE);
   }

   private List<Document> parseCSVFile(String data) {
      if (data == null || data.trim().isEmpty()) {
         return Collections.emptyList();
      }
      CsvParserSettings settings = new CsvParserSettings();
      settings.detectFormatAutomatically();
      settings.setHeaderExtractionEnabled(true);

      RowListProcessor rowProcessor = new RowListProcessor();
      settings.setProcessor(rowProcessor);

      CsvParser parser = new CsvParser(settings);
      parser.parse(new StringReader(data));

      String[] headers = rowProcessor.getHeaders();
      List<String[]> rows = rowProcessor.getRows();

      if (headers.length == 0 || rows.isEmpty()) {
         return Collections.emptyList();
      }

      return createDocumentsFromHeaderAndRows(headers, rows);
   }

   private List<Document> createDocumentsFromHeaderAndRows(String[] headers, List<String[]> rows) {
      List<DataDocument> dataDocuments = new LinkedList<>();
      for (int i = 0; i < rows.size(); i++) {
         dataDocuments.add(new DataDocument());
      }

      for (int i = 0; i < headers.length; i++) {
         String header = headers[i];
         for (int j = 0; j < rows.size(); j++) {
            String[] row = rows.get(j);
            if (row.length > i) {
               dataDocuments.get(j).append(header, row[i]);
            }
         }
      }

      return dataDocuments.stream().map(JsonDocument::new).collect(Collectors.toList());
   }
}
