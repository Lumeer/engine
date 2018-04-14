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
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.dao.system.SystemScopedDao;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoCompanyContactDao extends SystemScopedDao implements CompanyContactDao {

   public static final String COMPANY_CONTACT_COLLECTION = "companyContact";

   @PostConstruct
   public void initCollection() {
      createCompanyContactRepository();
   }

   @Override
   public CompanyContact getCompanyContact(final Organization organization) {
      return databaseCollection().find(MongoFilters.companyOrganizationIdFilter(organization.getId())).first();
   }

   @Override
   public CompanyContact setCompanyContact(final Organization organization, final CompanyContact companyContact) {
      final FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER);
      try {
         companyContact.setOrganizationId(organization.getId());
         return databaseCollection().findOneAndReplace(MongoFilters.companyOrganizationIdFilter(organization.getId()), companyContact, options);
      } catch (MongoException ex) {
         throw new StorageException("Cannot update company contact " + companyContact, ex);
      }
   }

   private MongoCollection<CompanyContact> databaseCollection() {
      return database.getCollection(COMPANY_CONTACT_COLLECTION, CompanyContact.class);
   }

   @Override
   public void createCompanyContactRepository() {
      if (database.getCollection(COMPANY_CONTACT_COLLECTION) == null) {
         database.createCollection(COMPANY_CONTACT_COLLECTION);

         MongoCollection<Document> companyContactCollection = database.getCollection(COMPANY_CONTACT_COLLECTION);
         companyContactCollection.createIndex(Indexes.ascending(CompanyContact.ORGANIZATION_ID), new IndexOptions().unique(true));
      }
   }

}
