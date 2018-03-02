package no.octopod.aidimporter;

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.utils.v201705.SelectorBuilder;
import com.google.api.ads.adwords.axis.v201705.cm.*;
import com.google.api.ads.adwords.axis.v201705.rm.*;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import java.rmi.RemoteException;
import java.util.*;

public class AdvertisementIdImporter {
    private SelectorBuilder builder = new SelectorBuilder();

    public boolean insertAudience(AdWordsSession session, List<String> audience, String userListName) {

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
            Member member = new Member();
            member.setMobileId(uuid);
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


    public Long getUserListIdByName(String name, AdWordsSession session) {
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

    public Long createUserList(String name, AdWordsSession session) {
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
