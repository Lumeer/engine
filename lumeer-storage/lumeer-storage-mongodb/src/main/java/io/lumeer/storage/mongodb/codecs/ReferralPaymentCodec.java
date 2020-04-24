package io.lumeer.storage.mongodb.codecs;/*
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

import io.lumeer.api.model.ReferralPayment;

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

public class ReferralPaymentCodec implements CollectibleCodec<ReferralPayment> {

   public static final String ID = "_id";
   public static final String REFERRAL = "referral";
   public static final String AMOUNT = "amount";
   public static final String CURRENCY = "currency";
   public static final String PAID = "paid";

   private final Codec<Document> documentCodec;

   public ReferralPaymentCodec(final CodecRegistry registry) {
      this.documentCodec = registry.get(Document.class);
   }

   @Override
   public ReferralPayment generateIdIfAbsentFromDocument(final ReferralPayment referralPayment) {
      if (!documentHasId(referralPayment)) {
         referralPayment.setId(new ObjectId().toHexString());
      }
      return referralPayment;
   }

   @Override
   public boolean documentHasId(final ReferralPayment referralPayment) {
      return referralPayment.getId() != null;
   }

   @Override
   public BsonValue getDocumentId(final ReferralPayment referralPayment) {
      if (!documentHasId(referralPayment)) {
         throw new IllegalStateException("The document does not contain an id");
      }

      return new BsonObjectId(new ObjectId(referralPayment.getId()));
   }

   @Override
   public ReferralPayment decode(final BsonReader bsonReader, final DecoderContext decoderContext) {
      Document bson = documentCodec.decode(bsonReader, decoderContext);

      String id = bson.getObjectId(ID).toHexString();
      String referral = bson.getString(REFERRAL);
      Long amount = bson.getLong(AMOUNT);
      String currency = bson.getString(CURRENCY);
      Boolean paid = bson.getBoolean(PAID);

      return new ReferralPayment(id, referral, amount, currency, paid);
   }

   @Override
   public void encode(final BsonWriter bsonWriter, final ReferralPayment referralPayment, final EncoderContext encoderContext) {
      Document bson = referralPayment.getId() != null ? new Document(ID, new ObjectId(referralPayment.getId())) : new Document();
      bson.append(REFERRAL, referralPayment.getReferral())
          .append(AMOUNT, referralPayment.getAmount())
          .append(CURRENCY, referralPayment.getCurrency())
          .append(PAID, referralPayment.isPaid());

      documentCodec.encode(bsonWriter, bson, encoderContext);
   }

   @Override
   public Class<ReferralPayment> getEncoderClass() {
      return ReferralPayment.class;
   }
}
