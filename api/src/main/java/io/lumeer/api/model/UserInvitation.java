package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class UserInvitation {

   public static final String EMAIL = "email";
   public static final String INVITATION = "invitationType";

   private final String email;
   private final InvitationType invitationType;

   @JsonCreator
   public UserInvitation(
         @JsonProperty(EMAIL) final String email,
         @JsonProperty(INVITATION) final InvitationType invitationType) {
      this.email = email != null ? email.toLowerCase() : "";
      this.invitationType = Objects.requireNonNullElse(invitationType, InvitationType.JOIN_ONLY);
   }

   public String getEmail() {
      return email;
   }

   public InvitationType getInvitationType() {
      return invitationType;
   }
}
