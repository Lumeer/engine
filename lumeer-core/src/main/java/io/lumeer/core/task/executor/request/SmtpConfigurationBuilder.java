package io.lumeer.core.task.executor.request;

import io.lumeer.core.util.EmailSecurityType;

public class SmtpConfigurationBuilder {
   private String host;
   private Integer port;
   private String user = "";
   private String password = "";
   private String from = "";
   private EmailSecurityType emailSecurityType = EmailSecurityType.NONE;

   public SmtpConfigurationBuilder setHost(final String host) {
      this.host = host;
      return this;
   }

   public SmtpConfigurationBuilder setPort(final Integer port) {
      this.port = port;
      return this;
   }

   public SmtpConfigurationBuilder setUser(final String user) {
      this.user = user;
      return this;
   }

   public SmtpConfigurationBuilder setPassword(final String password) {
      this.password = password;
      return this;
   }

   public SmtpConfigurationBuilder setFrom(final String from) {
      this.from = from;
      return this;
   }

   public SmtpConfigurationBuilder setEmailSecurityType(final EmailSecurityType emailSecurityType) {
      this.emailSecurityType = emailSecurityType;
      return this;
   }

   public SmtpConfiguration build() {
      return new SmtpConfiguration(host, port, user, password, from, emailSecurityType);
   }
}