package com.SpringBootMVC.ExpensesTracker.security;

import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Role;
import com.SpringBootMVC.ExpensesTracker.entity.User;
import com.SpringBootMVC.ExpensesTracker.repository.RoleRepository;
import com.SpringBootMVC.ExpensesTracker.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExpenseTrackerOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ExpenseTrackerOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User providerUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = providerUser.getAttributes();
        String subject = providerUser.getName();
        String email = attributeAsString(attributes, "email");

        if (!StringUtils.hasText(email) || !StringUtils.hasText(subject)) {
            throw authenticationFailure("The identity provider did not return a usable email and user ID.");
        }
        if ("google".equals(provider) && !Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw authenticationFailure("The identity provider did not verify this email address.");
        }

        User user = userRepository.findByOauthProviderAndOauthSubject(provider, subject);
        if (user == null) {
            user = createSocialUser(provider, subject, email, attributes);
        }
        if (user.getClient() == null) {
            throw authenticationFailure("This social account is not associated with an application profile.");
        }

        Map<String, Object> applicationAttributes = new HashMap<>(attributes);
        applicationAttributes.put("appClientId", user.getClient().getId());
        Collection<? extends GrantedAuthority> authorities = providerUser.getAuthorities();
        return new DefaultOAuth2User(
                authorities,
                applicationAttributes,
                userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName());
    }

    private User createSocialUser(
            String provider,
            String subject,
            String email,
            Map<String, Object> attributes) {
        Role standardRole = roleRepository.findByName("ROLE_STANDARD");
        if (standardRole == null) {
            throw new IllegalStateException("Standard user role has not been initialized");
        }

        Client client = new Client(
                firstNonBlank(attributeAsString(attributes, "given_name"),
                        attributeAsString(attributes, "first_name"),
                        attributeAsString(attributes, "name"),
                        provider + " user"),
                firstNonBlank(attributeAsString(attributes, "family_name"),
                        attributeAsString(attributes, "last_name"), ""),
                email);

        User user = new User();
        user.setUserName(provider + "_" + hashedSubject(subject));
        user.setPassword(null);
        user.setEnabled(true);
        user.setOauthProvider(provider);
        user.setOauthSubject(subject);
        user.setClient(client);
        user.setRoles(List.of(standardRole));
        return userRepository.save(user);
    }

    private static String hashedSubject(String subject) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(subject.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String attributeAsString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String string ? string : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static OAuth2AuthenticationException authenticationFailure(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info", description, null));
    }
}
