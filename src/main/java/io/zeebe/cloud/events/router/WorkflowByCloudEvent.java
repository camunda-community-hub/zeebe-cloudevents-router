package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowByCloudEvent {
    private String cloudEventType;
    private String workflowKey;

    public WorkflowByCloudEvent() {
    }

    public WorkflowByCloudEvent(String cloudEventType, String workflowKey) {
        this.cloudEventType = cloudEventType;
        this.workflowKey = workflowKey;
    }

    public String getCloudEventType() {
        return cloudEventType;
    }

    public void setCloudEventType(String cloudEventType) {
        this.cloudEventType = cloudEventType;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(String workflowKey) {
        this.workflowKey = workflowKey;
    }
}
