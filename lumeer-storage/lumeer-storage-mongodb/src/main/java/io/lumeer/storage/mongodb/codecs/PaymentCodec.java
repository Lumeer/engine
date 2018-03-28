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

import io.lumeer.api.dto.JsonPayment;
import io.lumeer.api.model.Payment;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class PaymentCodec implements Codec<JsonPayment> {

   private final Codec<Document> documentCodec;

   public PaymentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public JsonPayment decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document document = documentCodec.decode(bsonReader, decoderContext);

      return PaymentCodec.convertFromDocument(document);
   }

   public static JsonPayment convertFromDocument(Document bson) {
      LocalDateTime date = LocalDateTime.ofInstant(bson.getDate(Payment.DATE).toInstant(), ZoneId.systemDefault());
      long amount = bson.getLong(Payment.AMOUNT);
      String paymentId = bson.getString(Payment.PAYMENT_ID);
      LocalDateTime validUntil = LocalDateTime.ofInstant(bson.getDate(Payment.VALID_UNTIL).toInstant(), ZoneId.systemDefault());
      Payment.PaymentState state = Payment.PaymentState.values()[bson.getInteger(Payment.STATE)];
      Payment.ServiceLevel serviceLevel = Payment.ServiceLevel.values()[bson.getInteger(Payment.SERVICE_LEVEL)];

      return new JsonPayment(date, amount, paymentId, validUntil, state, serviceLevel);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final JsonPayment payment, final EncoderContext encoderContext) {
      Document document = new Document(Payment.DATE, Date.from(payment.getDate().atZone(ZoneId.systemDefault()).toInstant()))
            .append(Payment.AMOUNT, payment.getAmount())
            .append(Payment.PAYMENT_ID, payment.getPaymentId())
            .append(Payment.VALID_UNTIL, Date.from(payment.getValidUntil().atZone(ZoneId.systemDefault()).toInstant()))
            .append(Payment.STATE, payment.getState().ordinal())
            .append(Payment.SERVICE_LEVEL, payment.getServiceLevel().ordinal());

      documentCodec.encode(bsonWriter, document, encoderContext);
   }

   @Override
   public Class<JsonPayment> getEncoderClass() {
      return JsonPayment.class;
   }
}
