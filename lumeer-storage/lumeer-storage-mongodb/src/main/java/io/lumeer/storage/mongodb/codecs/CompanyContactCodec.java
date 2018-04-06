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
      String code = bson.getString(CompanyContact.CODE);
      String company = bson.getString(CompanyContact.COMPANY);
      String firstName = bson.getString(CompanyContact.FIRST_NAME);
      String surname = bson.getString(CompanyContact.LAST_NAME);
      String address1 = bson.getString(CompanyContact.ADDRESS1);
      String address2 = bson.getString(CompanyContact.ADDRESS2);
      String city = bson.getString(CompanyContact.CITY);
      String zip = bson.getString(CompanyContact.ZIP);
      String state = bson.getString(CompanyContact.STATE);
      String country = bson.getString(CompanyContact.COUNTRY);
      String email = bson.getString(CompanyContact.EMAIL);
      String phone = bson.getString(CompanyContact.PHONE);
      String ic = bson.getString(CompanyContact.IC);
      String dic = bson.getString(CompanyContact.DIC);

      return new CompanyContact(id, code, company, firstName, surname, address1, address2, city, zip, state, country, email, phone, ic, dic);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final CompanyContact companyContact, final EncoderContext encoderContext) {
      Document document = (documentHasId(companyContact) ? new Document(ID, getDocumentId(companyContact)) : new Document())
            .append(CompanyContact.CODE, companyContact.getCode())
            .append(CompanyContact.COMPANY, companyContact.getCompany())
            .append(CompanyContact.FIRST_NAME, companyContact.getFirstName())
            .append(CompanyContact.LAST_NAME, companyContact.getLastName())
            .append(CompanyContact.ADDRESS1, companyContact.getAddress1())
            .append(CompanyContact.ADDRESS2, companyContact.getAddress2())
            .append(CompanyContact.CITY, companyContact.getCity())
            .append(CompanyContact.ZIP, companyContact.getZip())
            .append(CompanyContact.STATE, companyContact.getState())
            .append(CompanyContact.COUNTRY, companyContact.getCountry())
            .append(CompanyContact.EMAIL, companyContact.getEmail())
            .append(CompanyContact.PHONE, companyContact.getPhone())
            .append(CompanyContact.IC, companyContact.getIc())
            .append(CompanyContact.DIC, companyContact.getDic());

      documentCodec.encode(bsonWriter, document, encoderContext);
   }

   @Override
   public Class<CompanyContact> getEncoderClass() {
      return CompanyContact.class;
   }
}
