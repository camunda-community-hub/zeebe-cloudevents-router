package io.zeebe.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;


public class ZeebeCloudEventBuilder {
    private CloudEventBuilder cloudEventBuilder;
    private final ZeebeCloudEventExtension zeebeCloudEventExtension;

    public ZeebeCloudEventBuilder(CloudEventBuilder cloudEventBuilder) {
        this.cloudEventBuilder = cloudEventBuilder;
        zeebeCloudEventExtension = new ZeebeCloudEventExtension();
    }

    public ZeebeCloudEventBuilder withCorrelationKey(String correlationKey) {
        zeebeCloudEventExtension.setCorrelationKey(correlationKey);
        return this;
    }

    public ZeebeCloudEventBuilder withJobKey(String jobKey) {
        zeebeCloudEventExtension.setJobKey(jobKey);
        return this;
    }

    public ZeebeCloudEventBuilder withWorkflowKey(String workflowKey) {
        zeebeCloudEventExtension.setWorkflowKey(workflowKey);
        return this;
    }

    public ZeebeCloudEventBuilder withWorkflowInstanceKey(String workflowInstanceKey) {
        zeebeCloudEventExtension.setWorkflowInstanceKey(workflowInstanceKey);
        return this;
    }

    public ZeebeCloudEventBuilder withBpmnActivityId(String bpmnActivityId) {
        zeebeCloudEventExtension.setBpmnActivityId(bpmnActivityId);
        return this;
    }

    public ZeebeCloudEventBuilder withBpmnActivityName(String bpmnActivityName) {
        zeebeCloudEventExtension.setBpmnActivityName(bpmnActivityName);
        return this;
    }

    public CloudEvent build() {
        return cloudEventBuilder
                .withExtension(zeebeCloudEventExtension)
                .build();
    }
}
