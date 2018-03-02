package no.octopod.aidimporter.util;

import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.conf.ConfigurationLoadException;
import com.google.api.client.auth.oauth2.Credential;

import javax.xml.bind.ValidationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing multiple AdWordsSessions.
 */
public class SessionManager {
    private static Map<String, AdWordsSession> sessions = new HashMap<String, AdWordsSession>();
    private static Credential credential;

    /**
     * Fetches a AdWordsSession from saved sessions.
     * It creates a new session if it doesn't already exists.
     * @param customerId Client ID from AdWords
     * @return AdWordsSession for given Client ID
     */
    public static AdWordsSession getSession(String customerId) {
        AdWordsSession session = sessions.get(customerId);
        if (session != null) {
            return session;
        } else {
            return createSession(customerId);
        }
    }

    /**
     * Creates a new AdWordsSession.
     * @param customerId Client ID to create a session for
     * @return AdWordsSession for given Client ID
     */
    public static AdWordsSession createSession(String customerId) {
        if (credential == null) {
            try {
                credential = getOAuth2Credential();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create OAuth2 Credentials using the provided properties.");
            }
        }

        AdWordsSession session = null;
        try {
            session = new AdWordsSession.Builder()
                    .fromFile()
                    .withOAuth2Credential(credential)
                    .withClientCustomerId(customerId)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to create AdWordsSession.");
        }

        if (session != null) {
            sessions.put(customerId, session);
        }

        return session;
    }

    /**
     * Get OAuth2 credentials using the developer token, client ID, client secret and
     * refresh token from ads.properties.
     * @return Credential containing the OAuth2
     * @throws Exception If ads.properties is missing configuration or if validation fails
     */
    private static Credential getOAuth2Credential() throws Exception {
        return new OfflineCredentials.Builder()
                .forApi(OfflineCredentials.Api.ADWORDS)
                .fromFile()
                .build()
                .generateCredential();
    }
}
