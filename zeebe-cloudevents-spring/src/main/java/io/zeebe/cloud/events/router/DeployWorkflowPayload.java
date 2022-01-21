package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeployWorkflowPayload {
    private String name;
    private String workflowDefinition;

    public DeployWorkflowPayload() {
    }

    public DeployWorkflowPayload(String name, String workflowDefinition) {
        this.name = name;
        this.workflowDefinition = workflowDefinition;
    }

    public String getWorkflowDefinition() {
        return workflowDefinition;
    }

    public void setWorkflowDefinition(String workflowDefinition) {
        this.workflowDefinition = workflowDefinition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DeployWorkflowPayload{" +
                "name='" + name + '\'' +
                ", workflowDefinition='" + workflowDefinition + '\'' +
                '}';
    }
}
