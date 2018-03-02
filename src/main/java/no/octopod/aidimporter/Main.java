package no.octopod.aidimporter;

import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.client.util.Charsets;
import no.octopod.aidimporter.util.SessionManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Invalid syntax.\nUsage: <customerId> <fileName> <listName>");
            return;
        }

        Set<String> audience;
        try {
            List<String> csv = Files.readAllLines(Paths.get(args[1]), Charsets.UTF_8);
            audience = new HashSet<>(csv);
        } catch (IOException e) {
            System.err.println("Could not find segment at given path.");
            return;
        }

        Set<String> existingIds = new HashSet<>();
        try {
            File f = new File("imported/" + args[2].toLowerCase() + ".csv");
            if (f.exists()) {
                List<String> existing = Files.readAllLines(
                        Paths.get("imported/"+ args[2].toLowerCase() + ".csv"), Charsets.UTF_8);
                existingIds = new HashSet<>(existing);
            } else {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("failed to create imported file");
        }

        // remove ids already written to audience
        audience.removeAll(existingIds);

        boolean success = false;
        AdWordsSession session = SessionManager.createSession(args[0]);
        AudienceImporter importer = new AudienceImporter();

        if (audience.size() > 0) {
            success = importer.insertAudience(session, new ArrayList<>(audience), args[2]);
        } else {
            System.out.println("No new advertisement ID's.");
        }

        if (success) {
            try (FileWriter fw = new FileWriter("imported/" + args[2].toLowerCase() + ".csv", true)) {
                for (String line : audience) {
                    fw.write(line + "\n");
                }
            } catch (IOException e) {
                System.err.println("Failed to write inserted ID's to " + args[2].toLowerCase() + ".csv");
            }
        }

    }
}
