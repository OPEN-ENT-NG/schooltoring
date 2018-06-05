package fr.openent.schooltoring.security.utils;

import org.entcore.common.user.UserInfos;

import java.util.List;

public class WorkflowActionUtils {

    private WorkflowActionUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class");
    }

    public static boolean hasRight(UserInfos user, String action) {
        List<UserInfos.Action> actions = user.getAuthorizedActions();
        for (UserInfos.Action userAction : actions) {
            if (action.equals(userAction.getDisplayName())) {
                return true;
            }
        }
        return false;
    }
}
