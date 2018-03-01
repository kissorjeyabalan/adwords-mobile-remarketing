package no.octopod.aidimporter;

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.utils.v201705.SelectorBuilder;
import com.google.api.ads.adwords.axis.v201705.cm.*;
import com.google.api.ads.adwords.axis.v201705.rm.*;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import no.octopod.aidimporter.util.SessionManager;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class App {
    private static SelectorBuilder builder = new SelectorBuilder();

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid syntax.\nUsage: <fileName> <listName>");
        }

        Set<String> audience;
        try {
            String csv = Resources.toString(Resources.getResource(args[0]), Charsets.UTF_8);
            audience = new HashSet<>(Arrays.asList(csv.split("\\r?\\n")));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not find segment at given path.");
        }

        Set<String> existingIds;
        try {
            String existing = Resources.toString(Resources.getResource("existing.csv"), Charsets.UTF_8);
            existingIds = new HashSet<>(Arrays.asList(existing.split("\\r?\\n")));
        } catch (IOException e) {
            throw new RuntimeException("existing.csv missing from class path");
        }

        // remove ids already written to audience
        audience.removeAll(existingIds);

        String cID = "250-493-2376";
        AdWordsSession session = SessionManager.createSession(cID);

        boolean success = false;
        if (audience.size() > 0) {
            success = insertAudience(session, new ArrayList<>(audience), args[1]);
        } else {
            System.out.println("No new advertisement ID's.");
        }

        if (success) {
            try (FileWriter fw = new FileWriter(Resources.getResource("existing.csv").getPath(), true)) {
                for (String line : audience) {
                    fw.write(line + "\n");
                }
            } catch (IOException e) {
                System.err.println("Failed to write inserted ID's to existing.csv");
            }
        }

    }

    private static boolean insertAudience(AdWordsSession session, List<String> audience, String userListName) {

        AdWordsServicesInterface awServices = AdWordsServices.getInstance();
        AdwordsUserListServiceInterface ulService =
                awServices.get(session, AdwordsUserListServiceInterface.class);

        // Create an operation to edit a UserList with a given ID
        MutateMembersOperation mutateMembersOperation = new MutateMembersOperation();
        MutateMembersOperand mutateMembersOperand = new MutateMembersOperand();
        mutateMembersOperand.setUserListId(getUserListIdByName(userListName, session));

        List<Member> members = new ArrayList<>(audience.size());

        // Insert the IDFA/AAID into a new Member object
        for (String uuid : audience) {
            String normalizedMobileId = uuid.trim().toLowerCase();
            Member member = new Member();
            member.setMobileId(normalizedMobileId);
            members.add(member);
        }

        // Create an operation to persist the new members to given UserList
        mutateMembersOperand.setMembersList(members.toArray(new Member[members.size()]));
        mutateMembersOperation.setOperand(mutateMembersOperand);
        mutateMembersOperation.setOperator(Operator.ADD);

        MutateMembersReturnValue mutateResult;

        // Inserts the Audience to the given client's audience
        try {
            mutateResult = ulService.mutateMembers(new MutateMembersOperation[] {mutateMembersOperation});
        } catch (RemoteException e) {
            System.err.println("Something went wrong while inserting audience.");
            return false;
        }

        for (UserList res : mutateResult.getUserLists()) {
            System.out.printf("%d advertisementIds were uploaded to user list '%s' with ID %d.%n",
                    audience.size(), res.getName(), res.getId());
        }

        return true;
    }


    private static Long getUserListIdByName(String name, AdWordsSession session) {
        AdwordsUserListServiceInterface userListService =
                AdWordsServices.getInstance().get(session, AdwordsUserListServiceInterface.class);

        Long id = -1L;
        Selector selector = builder.fields("Id").build();
        UserListPage userListPage;

        try {
            userListPage = userListService.get(selector);
        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
            return id;
        }

        // Attempt to find all CRM based UserList's with matching name, return first one found
        if (userListPage.getEntries() != null) {
            for (UserList entry : userListPage.getEntries()) {
                if (entry.getName().equals(name) && entry.getListType().equals(UserListType.CRM_BASED)) {
                    id = entry.getId();
                    break;
                }
            }
        }

        if (id == -1L) {
            return createUserList(name, session);
        } else {
            return id;
        }
    }

    private static Long createUserList(String name, AdWordsSession session) {
        AdwordsUserListServiceInterface userListService =
                AdWordsServices.getInstance().get(session, AdwordsUserListServiceInterface.class);

        // Create a new CRM based UserList which stores a member for a maximum of 365 days
        CrmBasedUserList userList = new CrmBasedUserList();
        userList.setName(name);
        userList.setMembershipLifeSpan(365L);

        UserListOperation operation = new UserListOperation();
        operation.setOperand(userList);
        operation.setOperator(Operator.ADD);

        UserListReturnValue result = new UserListReturnValue();

        try {
            result = userListService.mutate(new UserListOperation[] {operation});
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return result.getValue(0).getId();

    }
}
