package io.zeebe.cloudevents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.core.extensions.impl.ExtensionUtils;

import java.util.*;

// @TODO: can this live in a library?
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeebeCloudEventExtension implements CloudEventExtension {
    private String correlationKey;
    private String bpmnActivityName;
    private String bpmnActivityId;
    private String workflowKey;
    private String workflowInstanceKey;
    private String jobKey;


    public static final String CORRELATION_KEY = "Ce-CorrelationKey";
    public static final String BPMN_ACTIVITY_ID = "Ce-BpmnActivityId";
    public static final String BPMN_ACTIVITY_NAME = "Ce-BpmnActivityName";
    public static final String WORKFLOW_KEY = "Ce-WorkflowKey";
    public static final String WORKFLOW_INSTANCE_KEY = "Ce-WorkflowInstanceKey";
    public static final String JOB_KEY = "Ce-JobKey";

    private static final Set<String> KEY_SET = Collections
            .unmodifiableSet(
                    new HashSet<>(
                        Arrays.asList(CORRELATION_KEY,
                                BPMN_ACTIVITY_ID,
                                BPMN_ACTIVITY_NAME,
                                WORKFLOW_KEY,
                                WORKFLOW_INSTANCE_KEY,
                                JOB_KEY )));


    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }

    public String getBpmnActivityName() {
        return bpmnActivityName;
    }

    public void setBpmnActivityName(String bpmnActivityName) {
        this.bpmnActivityName = bpmnActivityName;
    }

    public String getBpmnActivityId() {
        return bpmnActivityId;
    }

    public void setBpmnActivityId(String bpmnActivityId) {
        this.bpmnActivityId = bpmnActivityId;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(String workflowKey) {
        this.workflowKey = workflowKey;
    }

    public String getWorkflowInstanceKey() {
        return workflowInstanceKey;
    }

    public void setWorkflowInstanceKey(String workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
    }

    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

//    @Override
//    public String toString() {
//        return Json.encode(this);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZeebeCloudEventExtension that = (ZeebeCloudEventExtension) o;
        return Objects.equals(correlationKey, that.correlationKey) &&
                Objects.equals(bpmnActivityName, that.bpmnActivityName) &&
                Objects.equals(bpmnActivityId, that.bpmnActivityId) &&
                Objects.equals(workflowKey, that.workflowKey) &&
                Objects.equals(workflowInstanceKey, that.workflowInstanceKey) &&
                Objects.equals(jobKey, that.jobKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationKey, bpmnActivityName, bpmnActivityId, workflowKey, workflowInstanceKey, jobKey);
    }

    @Override
    public void readFrom(CloudEventExtensions extensions) {
        Object correlationKey = extensions.getExtension(CORRELATION_KEY);
        if (correlationKey != null) {
            this.correlationKey = correlationKey.toString();
        }
        Object bpmnActivityId = extensions.getExtension(BPMN_ACTIVITY_ID);
        if (bpmnActivityId != null) {
            this.bpmnActivityId = bpmnActivityId.toString();
        }
        Object bpmnActivityName = extensions.getExtension(BPMN_ACTIVITY_NAME);
        if (bpmnActivityName != null) {
            this.bpmnActivityName = bpmnActivityName.toString();
        }
        Object workflowKey = extensions.getExtension(WORKFLOW_KEY);
        if (workflowKey != null) {
            this.workflowKey = workflowKey.toString();
        }
        Object workflowInstanceKey = extensions.getExtension(WORKFLOW_INSTANCE_KEY);
        if (workflowInstanceKey != null) {
            this.workflowInstanceKey = workflowInstanceKey.toString();
        }
        Object jobKey = extensions.getExtension(JOB_KEY);
        if (jobKey != null) {
            this.jobKey = jobKey.toString();
        }
    }

    @Override
    public Object getValue(String key) throws IllegalArgumentException {
        switch (key) {
            case CORRELATION_KEY:
                return this.correlationKey;
            case BPMN_ACTIVITY_ID:
                return this.bpmnActivityId;
            case BPMN_ACTIVITY_NAME:
                return this.bpmnActivityName;
            case WORKFLOW_KEY:
                return this.workflowKey;
            case WORKFLOW_INSTANCE_KEY:
                return this.workflowInstanceKey;
            case JOB_KEY:
                return this.jobKey;
        }
        throw ExtensionUtils.generateInvalidKeyException(this.getClass(), key);
    }

    @Override
    public Set<String> getKeys() {
        return KEY_SET;
    }

}
