package no.octopod.aidimporter;

import com.google.api.ads.adwords.lib.client.AdWordsSession;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static Map<String, AdWordsSession> sessions = new HashMap<String, AdWordsSession>();
}
