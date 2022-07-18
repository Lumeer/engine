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

import io.lumeer.api.model.TokenResponse;
import io.lumeer.core.auth.UserAuth0Utils;
import io.lumeer.engine.api.exception.UnsuccessfulOperationException;

import com.auth0.exception.Auth0Exception;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class AuthFacade extends AbstractFacade {

   @Inject
   private UserAuth0Utils userAuth0Utils;

   public TokenResponse exchangeCode(String code) {
      try {
         var tokenResponse = this.userAuth0Utils.exchangeCode(code);
         return new TokenResponse(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getTokenType(), tokenResponse.getExpiresIn());
      } catch (Auth0Exception e) {
         throw new UnsuccessfulOperationException("Unable to exchange code: " + code, e);
      }
   }

   public TokenResponse refreshToken(String token) {
      try {
         var tokenResponse = this.userAuth0Utils.refreshToken(token);
         return new TokenResponse(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getTokenType(), tokenResponse.getExpiresIn());
      } catch (Auth0Exception e) {
         throw new UnsuccessfulOperationException("Unable to refresh token: " + token, e);
      }
   }
}
