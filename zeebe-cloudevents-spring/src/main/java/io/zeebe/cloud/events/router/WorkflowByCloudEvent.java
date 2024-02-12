package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowByCloudEvent {
    private String cloudEventType;
    private String bpmnProcessId;
    private String processDefinitionKey;
    private String version;

    public WorkflowByCloudEvent() {
    }

    public WorkflowByCloudEvent(String cloudEventType, String bpmnProcessId) {
        this.cloudEventType = cloudEventType;
        this.bpmnProcessId = bpmnProcessId;
    }

    public WorkflowByCloudEvent(String cloudEventType, String bpmnProcessId, String version) {
        this.cloudEventType = cloudEventType;
        this.bpmnProcessId = bpmnProcessId;
        this.version = version;
    }

    public String getCloudEventType() {
        return cloudEventType;
    }

    public void setCloudEventType(String cloudEventType) {
        this.cloudEventType = cloudEventType;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public String toString() {
        return "WorkflowByCloudEvent{" +
                "cloudEventType='" + cloudEventType + '\'' +
                ", bpmnProcessId='" + bpmnProcessId + '\'' +
                ", processDefinitionKey='" + processDefinitionKey + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
