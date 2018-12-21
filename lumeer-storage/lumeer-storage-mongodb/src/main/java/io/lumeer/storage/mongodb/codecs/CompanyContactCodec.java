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
package io.lumeer.storage.mongodb.codecs;

import io.lumeer.api.model.CompanyContact;

import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class CompanyContactCodec implements CollectibleCodec<CompanyContact> {

   public static final String ID = "_id";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String COMPANY = "company";
   public static final String FIRST_NAME = "firstName";
   public static final String LAST_NAME = "lastName";
   public static final String ADDRESS1 = "address1";
   public static final String ADDRESS2 = "address2";
   public static final String CITY = "city";
   public static final String ZIP = "zip";
   public static final String STATE = "state";
   public static final String COUNTRY = "country";
   public static final String EMAIL = "email";
   public static final String PHONE = "phone";
   public static final String IC = "ic";
   public static final String DIC = "dic";
   public static final String VERSION = "version";

   private final Codec<Document> documentCodec;

   public CompanyContactCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public CompanyContact generateIdIfAbsentFromDocument(final CompanyContact companyContact) {
      if (!documentHasId(companyContact)) {
         companyContact.setId(new ObjectId().toHexString());
      }
      return companyContact;
   }

   @Override
   public boolean documentHasId(final CompanyContact companyContact) {
      return companyContact.getId() != null && !("".equals(companyContact.getId()));
   }

   @Override
   public BsonValue getDocumentId(final CompanyContact companyContact) {
      if (!documentHasId(companyContact)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(companyContact.getId()));
   }

   @Override
   public CompanyContact decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(bsonReader, decoderContext);

      return CompanyContactCodec.convertFromDocument(document);
   }

   private static CompanyContact convertFromDocument(final Document bson) {
      String id = bson.get(ID) != null ? bson.getObjectId(ID).toHexString() : null;
      String organizationId = bson.getString(ORGANIZATION_ID);
      String company = bson.getString(COMPANY);
      String firstName = bson.getString(FIRST_NAME);
      String surname = bson.getString(LAST_NAME);
      String address1 = bson.getString(ADDRESS1);
      String address2 = bson.getString(ADDRESS2);
      String city = bson.getString(CITY);
      String zip = bson.getString(ZIP);
      String state = bson.getString(STATE);
      String country = bson.getString(COUNTRY);
      String email = bson.getString(EMAIL);
      String phone = bson.getString(PHONE);
      String ic = bson.getString(IC);
      String dic = bson.getString(DIC);
      Long version = bson.getLong(VERSION);

      CompanyContact companyContact = new CompanyContact(id, organizationId, company, firstName, surname, address1, address2, city, zip, state, country, email, phone, ic, dic);
      companyContact.setVersion(version == null ? 0 : version);
      return companyContact;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final CompanyContact companyContact, final EncoderContext encoderContext) {
      Document document = (documentHasId(companyContact) ? new Document(ID, getDocumentId(companyContact)) : new Document())
            .append(ORGANIZATION_ID, companyContact.getOrganizationId())
            .append(COMPANY, companyContact.getCompany())
            .append(FIRST_NAME, companyContact.getFirstName())
            .append(LAST_NAME, companyContact.getLastName())
            .append(ADDRESS1, companyContact.getAddress1())
            .append(ADDRESS2, companyContact.getAddress2())
            .append(CITY, companyContact.getCity())
            .append(ZIP, companyContact.getZip())
            .append(STATE, companyContact.getState())
            .append(COUNTRY, companyContact.getCountry())
            .append(EMAIL, companyContact.getEmail())
            .append(PHONE, companyContact.getPhone())
            .append(IC, companyContact.getIc())
            .append(DIC, companyContact.getDic());

      documentCodec.encode(bsonWriter, document, encoderContext);
   }

   @Override
   public Class<CompanyContact> getEncoderClass() {
      return CompanyContact.class;
   }
}
