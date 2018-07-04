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

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.dto.JsonDocument;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.ImportedCollection;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class ImportFacade extends AbstractFacade {

   public static final String FORMAT_CSV = "csv";

   private static final int MAX_PARSED_DOCUMENTS = 1000;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private PermissionsChecker permissionsChecker;

   public Collection importDocuments(String format, ImportedCollection importedCollection) {
      Collection collectionToCreate = importedCollection.getCollection();
      collectionToCreate.setName(generateCollectionName(collectionToCreate.getName()));
      Collection collection = collectionFacade.createCollection(collectionToCreate);

      switch (format.toLowerCase()) {
         case FORMAT_CSV:
            parseCSVFile(collection, importedCollection.getData());
            break;
      }

      return collection;
   }

   private String generateCollectionName(String collectionName) {
      String name = collectionName != null && !collectionName.isEmpty() ? collectionName : "ImportedCollection";
      Set<String> collectionNames = collectionDao.getAllCollectionNames();
      if (!collectionNames.contains(name)) {
         return name;
      }

      int num = 2;
      String nameWithSuffix = name;
      while (collectionNames.contains(nameWithSuffix)) {
         nameWithSuffix = name + "(" + num + ")";
         num++;
      }
      return nameWithSuffix;
   }

   private void parseCSVFile(Collection collection, String data) {
      if (data == null || data.trim().isEmpty()) {
         return;
      }
      CsvParserSettings settings = new CsvParserSettings();
      settings.detectFormatAutomatically();
      settings.setHeaderExtractionEnabled(true);

      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(new StringReader(data));

      String[] headers = Arrays.stream(parser.getRecordMetadata().headers())
                               .filter(Objects::nonNull)
                               .toArray(String[]::new);

      if (headers.length == 0) {
         return;
      }

      List<Attribute> createdAttributes = createAttributes(collection.getId(), headers);
      collection.setAttributes(new HashSet<>(createdAttributes));
      collection.setLastAttributeNum(collection.getLastAttributeNum() + createdAttributes.size());
      String[] headerIds = createdAttributes.stream().map(Attribute::getId).toArray(String[]::new);

      int[] counts = new int[headers.length];

      int documentsCount = 0;
      List<Document> documents = new ArrayList<>();
      String[] row;
      while ((row = parser.parseNext()) != null) {
         Document d = createDocumentFromRow(headerIds, row, counts);
         addDocumentMetadata(collection.getId(), d);

         documents.add(d);

         if (documents.size() >= MAX_PARSED_DOCUMENTS) {
            addDocumentsToDb(collection.getId(), documents);
            documents.clear();
         }

         documentsCount++;
      }

      if (!documents.isEmpty()) {
         addDocumentsToDb(collection.getId(), documents);
      }

      parser.stopParsing();

      addCollectionMetadata(collection, headerIds, counts, documentsCount);
   }

   private void addCollectionMetadata(Collection collection, String[] headersIds, int[] counts, int documentsCount) {
      collection.getAttributes().forEach(attr -> {
         int index = Arrays.asList(headersIds).indexOf(attr.getId());
         attr.setUsageCount(counts[index]);
      });

      collection.setDocumentsCount(documentsCount);
      collection.setLastTimeUsed(ZonedDateTime.now());
      collectionDao.updateCollection(collection.getId(), collection);
   }

   private List<Attribute> createAttributes(String collectionId, String[] headers) {
      List<Attribute> attributes = Arrays.stream(headers).map(header -> new JsonAttribute(header, header, Collections.emptySet(), 0)).collect(Collectors.toList());
      return new ArrayList<>(collectionFacade.createCollectionAttributes(collectionId, attributes));
   }

   private void addDocumentsToDb(String collectionId, List<Document> documents) {
      permissionsChecker.checkDocumentLimits(documents);

      List<Document> storedDocuments = documentDao.createDocuments(documents);

      List<DataDocument> dataDocuments = new LinkedList<>();
      for (int i = 0; i < documents.size(); i++) {
         DataDocument dataDocument = documents.get(i).getData();
         dataDocument.setId(storedDocuments.get(i).getId());
         dataDocuments.add(dataDocument);
      }
      dataDao.createData(collectionId, dataDocuments);
   }

   private void addDocumentMetadata(String collectionId, Document document) {
      document.setCollectionId(collectionId);
      document.setCreatedBy(authenticatedUser.getCurrentUserId());
      document.setCreationDate(ZonedDateTime.now());
      document.setDataVersion(DocumentFacade.INITIAL_VERSION);
   }

   private Document createDocumentFromRow(String[] headers, String[] row, int[] counts) {
      final DataDocument d = new DataDocument();

      for (int i = 0; i < Math.min(headers.length, row.length); i++) {
         if (row[i] != null) {
            d.append(headers[i], row[i]);
            counts[i]++;
         }
      }

      return new JsonDocument(d);
   }
}
