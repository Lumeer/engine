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
package io.lumeer.storage.mongodb.dao.organization;

import io.lumeer.api.model.CompanyContact;
import io.lumeer.api.model.Organization;
import io.lumeer.engine.api.event.UpdateCompanyContact;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.codecs.CompanyContactCodec;
import io.lumeer.storage.mongodb.dao.system.SystemScopedDao;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class MongoCompanyContactDao extends SystemScopedDao implements CompanyContactDao {

   public static final String COMPANY_CONTACT_COLLECTION = "companyContact";

   @Inject
   private Event<UpdateCompanyContact> updateCompanyContactEvent;

   @PostConstruct
   public void initCollection() {
      createCompanyContactRepository();
   }

   @Override
   public CompanyContact getCompanyContact(final Organization organization) {
      return databaseCollection().find(companyOrganizationIdFilter(organization.getId())).first();
   }

   private Bson companyOrganizationIdFilter(String organizationId){
      return Filters.eq(CompanyContact.ORGANIZATION_ID, organizationId);
   }

   @Override
   public CompanyContact setCompanyContact(final Organization organization, final CompanyContact companyContact) {
      FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
      try {
         companyContact.setOrganizationId(organization.getId());
         Bson filter = companyOrganizationIdFilter(organization.getId());
         Bson update = new Document("$set", companyContact).append("$inc", new Document(CompanyContactCodec.VERSION, 1L));

         final CompanyContact updatedCompanyContact = databaseCollection().findOneAndUpdate(filter, update, options);
         if (updateCompanyContactEvent != null) {
            updateCompanyContactEvent.fire(new UpdateCompanyContact(updatedCompanyContact));
         }

         return updatedCompanyContact;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update company contact " + companyContact, ex);
      }
   }

   private MongoCollection<CompanyContact> databaseCollection() {
      return database.getCollection(COMPANY_CONTACT_COLLECTION, CompanyContact.class);
   }

   @Override
   public void createCompanyContactRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(COMPANY_CONTACT_COLLECTION)) {
         database.createCollection(COMPANY_CONTACT_COLLECTION);

         MongoCollection<Document> companyContactCollection = database.getCollection(COMPANY_CONTACT_COLLECTION);
         companyContactCollection.createIndex(Indexes.ascending(CompanyContactCodec.ORGANIZATION_ID), new IndexOptions().unique(true));
      }
   }

}
