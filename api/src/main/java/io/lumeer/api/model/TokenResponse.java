package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TokenResponse {

   private final String accessToken;
   private final String refreshToken;
   private final String tokenType;
   private final long expiresIn;

   public TokenResponse(final String accessToken, final String refreshToken, final String tokenType, final long expiresIn) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      this.tokenType = tokenType;
      this.expiresIn = expiresIn;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getRefreshToken() {
      return this.refreshToken;
   }

   public String getTokenType() {
      return this.tokenType;
   }

   public long getExpiresIn() {
      return this.expiresIn;
   }
}

