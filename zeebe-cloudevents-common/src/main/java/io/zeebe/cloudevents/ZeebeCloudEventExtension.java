package io.zeebe.cloudevents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.core.extensions.impl.ExtensionUtils;

import java.util.*;

// @TODO: can this live in a library?
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ZeebeCloudEventExtension implements CloudEventExtension {
    /**
     * The key of the {@code CorrelationKey} extension
     */
    public static final String CORRELATION_KEY = "correlationkey";
    public static final String BPMN_ACTIVITY_ID = "bpmnactivityid";
    public static final String BPMN_ACTIVITY_NAME = "bpmnactivityname";
    public static final String PROCESS_DEFINITION_KEY = "processdefinitionkey";
    public static final String PROCESS_INSTANCE_KEY = "processinstancekey";
    public static final String JOB_KEY = "jobkey";

    private static final Set<String> KEY_SET = Collections
            .unmodifiableSet(
                    new HashSet<>(
                        Arrays.asList(CORRELATION_KEY,
                                BPMN_ACTIVITY_ID,
                                BPMN_ACTIVITY_NAME,
                                PROCESS_DEFINITION_KEY,
                                PROCESS_INSTANCE_KEY,
                                JOB_KEY )));

    private String correlationKey;
    private String bpmnActivityName;
    private String bpmnActivityId;
    private String processDefinitionKey;
    private String processInstanceKey;
    private String jobKey;

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

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(String processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
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
                Objects.equals(processDefinitionKey, that.processDefinitionKey) &&
                Objects.equals(processInstanceKey, that.processInstanceKey) &&
                Objects.equals(jobKey, that.jobKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationKey, bpmnActivityName, bpmnActivityId, processDefinitionKey, processInstanceKey, jobKey);
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
        Object processDefinitionKey = extensions.getExtension(PROCESS_DEFINITION_KEY);
        if (processDefinitionKey != null) {
            this.processDefinitionKey = processDefinitionKey.toString();
        }
        Object processInstanceKey = extensions.getExtension(PROCESS_INSTANCE_KEY);
        if (processInstanceKey != null) {
            this.processInstanceKey = processInstanceKey.toString();
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
            case PROCESS_DEFINITION_KEY:
                return this.processDefinitionKey;
            case PROCESS_INSTANCE_KEY:
                return this.processInstanceKey;
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
