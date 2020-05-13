package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowByCloudEvent {
    private String cloudEventType;
    private String bpmnProcessId;

    public WorkflowByCloudEvent() {
    }

    public WorkflowByCloudEvent(String cloudEventType, String bpmnProcessId) {
        this.cloudEventType = cloudEventType;
        this.bpmnProcessId = bpmnProcessId;
    }

    public String getCloudEventType() {
        return cloudEventType;
    }

    public void setCloudEventType(String cloudEventType) {
        this.cloudEventType = cloudEventType;
    }

    public String getBPMNProcessId() {
        return bpmnProcessId;
    }

    public void setWorkflowKey(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }
}
