package com.SpringBootMVC.ExpensesTracker.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OAuthClientRegistrationConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();
        String googleClientId = environment.getProperty("GOOGLE_CLIENT_ID", "");
        String googleClientSecret = environment.getProperty("GOOGLE_CLIENT_SECRET", "");
        if (!googleClientId.isBlank() && !googleClientSecret.isBlank()) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .build());
        }

        String facebookClientId = environment.getProperty("FACEBOOK_CLIENT_ID", "");
        String facebookClientSecret = environment.getProperty("FACEBOOK_CLIENT_SECRET", "");
        if (!facebookClientId.isBlank() && !facebookClientSecret.isBlank()) {
            String apiVersion = environment.getProperty("app.oauth.facebook.api-version", "v22.0");
            registrations.add(ClientRegistration.withRegistrationId("facebook")
                    .clientId(facebookClientId)
                    .clientSecret(facebookClientSecret)
                    .clientName("Facebook")
                    .authorizationGrantType(
                            org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("email", "public_profile")
                    .authorizationUri("https://www.facebook.com/" + apiVersion + "/dialog/oauth")
                    .tokenUri("https://graph.facebook.com/" + apiVersion + "/oauth/access_token")
                    .userInfoUri("https://graph.facebook.com/" + apiVersion
                            + "/me?fields=id,name,first_name,last_name,email")
                    .userNameAttributeName("id")
                    .build());
        }

        return registrations.isEmpty()
                ? new EmptyClientRegistrationRepository()
                : new InMemoryClientRegistrationRepository(registrations);
    }

    private static final class EmptyClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public java.util.Iterator<ClientRegistration> iterator() {
            return List.<ClientRegistration>of().iterator();
        }
    }
}
