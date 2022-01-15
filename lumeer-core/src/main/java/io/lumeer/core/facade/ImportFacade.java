/*
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
package io.lumeer.core.facade;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.ImportedCollection;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

   private static final CharSequenceTranslator TRANSLATOR = new LookupTranslator(EntityArrays.BASIC_ESCAPE);

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionDao collectionDao;

   public Collection importDocuments(String format, ImportedCollection importedCollection) {
      Collection collectionToCreate = importedCollection.getCollection();
      collectionToCreate.setName(generateCollectionName(collectionToCreate.getName()));

      switch (format.toLowerCase()) {
         case FORMAT_CSV:
            return parseCSVFile(collectionToCreate, importedCollection.getData());
         default:
            throw new IllegalArgumentException("Cannot import Collection with format: " + format);
      }
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

   private Collection parseCSVFile(Collection collectionToCreate, String data) {
      if (data == null || data.trim().isEmpty()) {
         return collectionDao.createCollection(collectionToCreate);
      }
      CsvParserSettings settings = new CsvParserSettings();
      settings.setMaxCharsPerColumn(16 * 1024);
      settings.detectFormatAutomatically(',', ';');
      settings.setHeaderExtractionEnabled(true);

      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(new StringReader(data));

      String[] headers = Arrays.stream(parser.getRecordMetadata().headers())
                               .filter(Objects::nonNull)
                               .toArray(String[]::new);

      if (headers.length == 0) {
         return collectionDao.createCollection(collectionToCreate);
      }

      Collection collection = createAttributes(collectionToCreate, headers);
      Set<Attribute> createdAttributes = collection.getAttributes();
      collection.setLastAttributeNum(collection.getLastAttributeNum() + createdAttributes.size());
      String[] headerIds = createdAttributes.stream().map(Attribute::getId).toArray(String[]::new);

      int[] counts = new int[headers.length];
      long documentsCount = 0;

      List<Document> documents = new ArrayList<>();
      String[] row;
      while ((row = parser.parseNext()) != null) {
         Document d = createDocumentFromRow(headerIds, row, counts);
         addDocumentMetadata(collection.getId(), d);

         documents.add(d);

         if (documents.size() >= MAX_PARSED_DOCUMENTS) {
            documentsCount+= addDocumentsToDb(collection.getId(), documents);
            documents.clear();
         }

      }

      if (!documents.isEmpty()) {
         documentsCount+= addDocumentsToDb(collection.getId(), documents);
      }

      parser.stopParsing();

      addCollectionMetadata(collection, headerIds, counts, documentsCount);
      return collection;
   }

   private void addCollectionMetadata(Collection collection, String[] headersIds, int[] counts, long documentsCount) {
      final Collection originalCollection = collection.copy();
      collection.getAttributes().forEach(attr -> {
         int index = Arrays.asList(headersIds).indexOf(attr.getId());
         attr.setUsageCount(counts[index]);
      });
      collection.setDocumentsCount(documentsCount);

      collection.setLastTimeUsed(ZonedDateTime.now());
      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
   }

   private Collection createAttributes(Collection collection, String[] headers) {
      List<Attribute> attributes = Arrays.stream(headers)
                                         .map(AttributeUtil::cleanAttributeName)
                                         .map(Attribute::new).collect(Collectors.toList());
      collection.setAttributes(attributes);
      return collectionFacade.createCollection(collection);
   }

   private long addDocumentsToDb(String collectionId, List<Document> documents) {
      return documentFacade.createDocuments(collectionId, documents, true).size();
   }

   private void addDocumentMetadata(String collectionId, Document document) {
      document.setCollectionId(collectionId);
      document.setCreatedBy(getCurrentUserId());
      document.setCreationDate(ZonedDateTime.now());
   }

   private Document createDocumentFromRow(String[] headers, String[] row, int[] counts) {
      final DataDocument d = new DataDocument();

      for (int i = 0; i < Math.min(headers.length, row.length); i++) {
         if (row[i] != null) {
            d.append(headers[i], TRANSLATOR.translate(row[i]));
            counts[i]++;
         }
      }

      return new Document(d);
   }
}
