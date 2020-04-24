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
package io.lumeer.storage.mongodb.codecs;

import io.lumeer.api.model.Payment;

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

import java.util.Date;

public class PaymentCodec implements CollectibleCodec<Payment> {

   public static final String ID = "_id";
   public static final String DATE = "date";
   public static final String AMOUNT = "amount";
   public static final String PAYMENT_ID = "paymentId";
   public static final String START = "start";
   public static final String VALID_UNTIL = "validUntil";
   public static final String STATE = "state";
   public static final String SERVICE_LEVEL = "serviceLevel";
   public static final String USERS = "users";
   public static final String LANGUAGE = "language";
   public static final String CURRENCY = "currency";
   public static final String GW_URL = "gwUrl";
   public static final String VERSION = "version";
   public static final String REFERRAL = "referral";

   private final Codec<Document> documentCodec;

   public PaymentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public Payment decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(bsonReader, decoderContext);

      return PaymentCodec.convertFromDocument(document);
   }

   public static Payment convertFromDocument(Document bson) {
      String id = bson.get(ID) != null ? bson.getObjectId(ID).toHexString() : null;
      Date date = bson.getDate(DATE);
      long amount = bson.getLong(AMOUNT);
      String paymentId = bson.getString(PAYMENT_ID);
      Date start = bson.getDate(START);
      Date validUntil = bson.getDate(VALID_UNTIL);
      Payment.PaymentState state = Payment.PaymentState.fromInt(bson.getInteger(STATE));
      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.fromInt(bson.getInteger(SERVICE_LEVEL));
      int users = bson.getInteger(USERS);
      Long version = bson.getLong(VERSION);
      String language = bson.getString(LANGUAGE);
      String currency = bson.getString(CURRENCY);
      String gwUrl = bson.getString(GW_URL);
      String referral = bson.getString(REFERRAL);

      Payment payment = new Payment(id, date, amount, paymentId, start, validUntil, state, serviceLevel, users, language, currency, gwUrl, referral);
      payment.setVersion(version == null ? 1 : version);
      return payment;
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Payment payment, final EncoderContext encoderContext) {
      Document document = (documentHasId(payment) ? new Document(ID, getDocumentId(payment)) : new Document())
            .append(DATE, payment.getDate())
            .append(AMOUNT, payment.getAmount())
            .append(PAYMENT_ID, payment.getPaymentId())
            .append(START, payment.getStart())
            .append(VALID_UNTIL, payment.getValidUntil())
            .append(STATE, payment.getState().ordinal())
            .append(SERVICE_LEVEL, payment.getServiceLevel().ordinal())
            .append(USERS, payment.getUsers())
            .append(LANGUAGE, payment.getLanguage())
            .append(CURRENCY, payment.getCurrency())
            .append(GW_URL, payment.getGwUrl())
            .append(REFERRAL, payment.getReferral());

      documentCodec.encode(bsonWriter, document, encoderContext);
   }

   @Override
   public Class<Payment> getEncoderClass() {
      return Payment.class;
   }

   @Override
   public Payment generateIdIfAbsentFromDocument(final Payment payment) {
      if (!documentHasId(payment)) {
         payment.setId(new ObjectId().toHexString());
      }
      return payment;
   }

   @Override
   public boolean documentHasId(final Payment payment) {
      return payment.getId() != null && !("".equals(payment.getId()));
   }

   @Override
   public BsonValue getDocumentId(final Payment payment) {
      if (!documentHasId(payment)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(payment.getId()));
   }
}
