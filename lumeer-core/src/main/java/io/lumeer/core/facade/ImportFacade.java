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
import io.lumeer.api.model.ImportType;
import io.lumeer.api.model.ImportedCollection;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.core.adapter.SearchAdapter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.ImportCollectionContent;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
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

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private Event<ImportCollectionContent> importCollectionContentEvent;

   private SearchAdapter searchAdapter;

   @PostConstruct
   public void init() {
      ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);

      searchAdapter = new SearchAdapter(permissionsChecker.getPermissionAdapter(), constraintManager, documentDao, dataDao, linkInstanceDao, linkDataDao);
   }

   public Collection importDocuments(String format, ImportedCollection importedCollection) {
      Collection collectionToCreate = importedCollection.getCollection();
      collectionToCreate.setName(generateCollectionName(collectionToCreate.getName()));
      Collection collection = collectionFacade.createCollection(collectionToCreate);

      return importDocuments(collection, format, importedCollection.getData(), ImportType.APPEND, null);
   }

   public Collection importDocuments(String collectionId, String format, ImportedCollection importedCollection) {
      Collection collection = collectionFacade.getCollection(collectionId);

      return importDocuments(collection, format, importedCollection.getData(), importedCollection.getType(), importedCollection.getMergeAttributeId());
   }

   private Collection importDocuments(Collection collection, String format, String data, ImportType type, String key) {
      permissionsChecker.checkCreateDocuments(collection);

      switch (type) {
         case REPLACE:
            permissionsChecker.checkRole(collection, RoleType.DataDelete);
            documentFacade.deleteAllDocuments(collection);
            break;
         case UPDATE:
            permissionsChecker.checkRole(collection, RoleType.DataWrite);
            break;
      }

      switch (format.toLowerCase()) {
         case FORMAT_CSV:
            return parseCSVFile(collection, data, type, key);
         default:
            return collection;
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

   private Collection parseCSVFile(Collection collection, String data, ImportType importType, String mergeAttributeId) {
      if (data == null || data.trim().isEmpty()) {
         return collection;
      }
      CsvParserSettings settings = new CsvParserSettings();
      settings.setMaxCharsPerColumn(16 * 1024);
      settings.detectFormatAutomatically(',', ';');
      settings.setHeaderExtractionEnabled(true);

      CsvParser parser = new CsvParser(settings);
      parser.beginParsing(new StringReader(data));

      String[] headers = parseHeaderNames(parser.getRecordMetadata().headers());

      if (headers.length == 0) {
         return collection;
      }

      collection = createAttributes(collection, headers);
      String[] headerIds = createHeaderIds(collection, headers);

      Attribute mergeAttribute = ImportType.UPDATE.equals(importType) ?
            collection.getAttributes().stream().filter(attr -> attr.getId().equals(mergeAttributeId)).findFirst().orElse(null) : null;
      Map<String, List<Document>> mergeDocuments = getDocumentsByKey(collection, mergeAttribute);

      int[] counts = new int[headers.length];
      long documentsCount = 0;

      List<Document> documentsToCreate = new ArrayList<>();
      List<Document> documentsToUpdate = new ArrayList<>();
      String[] row;
      while ((row = parser.parseNext()) != null) {
         Document document = createDocumentFromRow(headerIds, row, counts);
         Document toMerge = checkMergeDocument(document, mergeAttribute, mergeDocuments);
         if (toMerge != null) {
            toMerge.setData(document.getData());
            documentsToUpdate.add(toMerge);
         } else {
            addDocumentMetadata(collection.getId(), document);
            documentsToCreate.add(document);
         }

      }

      if (documentsToUpdate.size() > 0) {
         documentFacade.updateDocumentsMetaData(collection, documentsToUpdate, false);
         documentFacade.updateDocumentsData(collection, documentsToUpdate, false);
      }

      while (documentsToCreate.size() > 0) {
         List<Document> slice = Utils.sublistAndRemove(documentsToCreate, 0, MAX_PARSED_DOCUMENTS);
         documentsCount += documentFacade.createDocuments(collection.getId(), slice, false).size();
      }

      parser.stopParsing();

      addCollectionMetadata(collection, headerIds, counts, documentsCount);

      if (importCollectionContentEvent != null) {
         importCollectionContentEvent.fire(new ImportCollectionContent(collection));
      }

      return collection;
   }

   private String[] parseHeaderNames(String[] rawHeaders) {
      Map<String, Integer> sameNameCounts = new HashMap<>();
      List<String> names = new ArrayList<>();

      for (String header : rawHeaders) {
         String nonNullHeader = AttributeUtil.cleanAttributeName(header != null ? header : "Untitled");
         Integer headerCount = sameNameCounts.get(nonNullHeader);
         if (headerCount != null) {
            names.add(nonNullHeader + "_" + (headerCount + 1));
            sameNameCounts.put(nonNullHeader, headerCount + 1);
         } else {
            names.add(nonNullHeader);
            sameNameCounts.put(nonNullHeader, 1);
         }
      }

      return names.toArray(String[]::new);
   }

   private String[] createHeaderIds(Collection collection, String[] headerNames) {
      Map<String, String> attributeNamesToId = collection.getAttributes().stream().collect(Collectors.toMap(Attribute::getName, Attribute::getId));
      return Arrays.stream(headerNames).map(attributeNamesToId::get)
                   .toArray(String[]::new);
   }

   private Document checkMergeDocument(Document document, Attribute attribute, Map<String, List<Document>> allDocuments) {
      if (attribute == null) {
         return null;
      }

      String mergeKey = document.getData().getString(attribute.getId());
      mergeKey = mergeKey != null ? mergeKey : "";
      List<Document> toMerge = allDocuments.get(mergeKey);
      if (toMerge != null && toMerge.size() > 0) {
         return toMerge.remove(0);
      }

      return null;
   }

   private Map<String, List<Document>> getDocumentsByKey(Collection collection, Attribute attribute) {
      if (attribute == null) {
         return Collections.emptyMap();
      }

      return searchAdapter.getAllDocuments(collection, null, null)
                          .stream()
                          .collect(Collectors.groupingBy(document -> this.getDocumentKey(document, attribute)));
   }

   private String getDocumentKey(Document document, Attribute attribute) {
      Object value = document.getData() != null ? document.getData().get(attribute.getId()) : null;
      if (value != null) {
         return value.toString();
      }
      return "";
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
      Set<String> currentCollectionNames = collection.getAttributes().stream()
                                                     .map(Attribute::getName).collect(Collectors.toSet());
      List<Attribute> attributes = Arrays.stream(headers)
                                         .filter(name -> !currentCollectionNames.contains(name))
                                         .map(Attribute::new).collect(Collectors.toList());
      return collectionFacade.createCollectionAttributes(collection, attributes);
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
