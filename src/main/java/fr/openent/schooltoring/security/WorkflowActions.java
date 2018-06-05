package fr.openent.schooltoring.security;

public enum WorkflowActions {
    VIEW ("schooltoring.view");

    private final String actionName;

    WorkflowActions(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public String toString () {
        return this.actionName;
    }
}
