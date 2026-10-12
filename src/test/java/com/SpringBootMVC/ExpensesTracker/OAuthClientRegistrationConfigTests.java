package com.SpringBootMVC.ExpensesTracker;

import com.SpringBootMVC.ExpensesTracker.security.OAuthClientRegistrationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OAuthClientRegistrationConfigTests {

    private final OAuthClientRegistrationConfig configuration = new OAuthClientRegistrationConfig();

    @Test
    void enablesConfiguredProvidersAndLeavesUnconfiguredProvidersUnavailable() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GOOGLE_CLIENT_ID", "google-test-client")
                .withProperty("GOOGLE_CLIENT_SECRET", "google-test-secret")
                .withProperty("FACEBOOK_CLIENT_ID", "facebook-test-client")
                .withProperty("FACEBOOK_CLIENT_SECRET", "facebook-test-secret");

        ClientRegistrationRepository registrations =
                configuration.clientRegistrationRepository(environment);

        assertNotNull(registrations.findByRegistrationId("google"));
        assertEquals(
                "https://www.facebook.com/v22.0/dialog/oauth",
                registrations.findByRegistrationId("facebook").getProviderDetails()
                        .getAuthorizationUri());
    }

    @Test
    void doesNotExposeProvidersUntilClientCredentialsAreConfigured() {
        ClientRegistrationRepository registrations =
                configuration.clientRegistrationRepository(new MockEnvironment());

        assertNull(registrations.findByRegistrationId("google"));
        assertNull(registrations.findByRegistrationId("facebook"));
    }
}
