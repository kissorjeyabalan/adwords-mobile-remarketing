package no.octopod.aidimporter.util;

import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.client.auth.oauth2.Credential;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static Map<String, AdWordsSession> sessions = new HashMap<String, AdWordsSession>();
    private static Credential credential;

    public static AdWordsSession getSession(String customerId) {
        AdWordsSession session = sessions.get(customerId);
        if (session != null) {
            return session;
        } else {
            return createSession(customerId);
        }
    }

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

    private static Credential getOAuth2Credential() throws Exception {
        return new OfflineCredentials.Builder()
                .forApi(OfflineCredentials.Api.ADWORDS)
                .fromFile()
                .build()
                .generateCredential();
    }
}
