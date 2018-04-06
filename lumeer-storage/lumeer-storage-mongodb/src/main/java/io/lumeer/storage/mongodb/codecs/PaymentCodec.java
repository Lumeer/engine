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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PaymentCodec implements CollectibleCodec<Payment> {

   public static final String ID = "_id";

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
      Date date = bson.getDate(Payment.DATE);
      long amount = bson.getLong(Payment.AMOUNT);
      String paymentId = bson.getString(Payment.PAYMENT_ID);
      Date start = bson.getDate(Payment.START);
      Date validUntil = bson.getDate(Payment.VALID_UNTIL);
      Payment.PaymentState state = Payment.PaymentState.fromInt(bson.getInteger(Payment.STATE));
      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.fromInt(bson.getInteger(Payment.SERVICE_LEVEL));
      int users = bson.getInteger(Payment.USERS);
      String language = bson.getString(Payment.LANGUAGE);
      String currency = bson.getString(Payment.CURRENCY);
      String gwUrl = bson.getString(Payment.GW_URL);

      return new Payment(id, date, amount, paymentId, start, validUntil, state, serviceLevel, users, language, currency, gwUrl);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final Payment payment, final EncoderContext encoderContext) {
      Document document = (documentHasId(payment) ? new Document(ID, getDocumentId(payment)) : new Document())
            .append(Payment.DATE, payment.getDate())
            .append(Payment.AMOUNT, payment.getAmount())
            .append(Payment.PAYMENT_ID, payment.getPaymentId())
            .append(Payment.START, payment.getStart())
            .append(Payment.VALID_UNTIL, payment.getValidUntil())
            .append(Payment.STATE, payment.getState().ordinal())
            .append(Payment.SERVICE_LEVEL, payment.getServiceLevel().ordinal())
            .append(Payment.USERS, payment.getUsers())
            .append(Payment.LANGUAGE, payment.getLanguage())
            .append(Payment.CURRENCY, payment.getCurrency())
            .append(Payment.GW_URL, payment.getGwUrl());

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
