package io.zeebe.cloud.events.router;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//@TODO: refine interface.. to many leaked types here
@Service
public class CloudEventsZeebeMappingsService {
    // ProcessDefinitionKey, ProcessInstanceKey, Jobs
    private Map<String, Map<String, Set<String>>> workflowsPendingJobs = new HashMap<>();

    private Map<String, Set<String>> messagesByProcessDefinitionKey = new HashMap<>();

    private Map<String, WorkflowByCloudEvent> startWorkflows = new HashMap<>();

    public CloudEventsZeebeMappingsService() {

    }

    public void addPendingJob(String processDefinitionKey, String processInstanceKey, String jobKey) {
        if (workflowsPendingJobs.get(processDefinitionKey) == null) {
            workflowsPendingJobs.put(processDefinitionKey, new HashMap<>());
        }
        if (!workflowsPendingJobs.get(processDefinitionKey).containsKey(processInstanceKey)) {
            workflowsPendingJobs.get(processDefinitionKey).put(processInstanceKey, new HashSet<>());
        }
        workflowsPendingJobs.get(processDefinitionKey).get(processInstanceKey).add(jobKey);
    }

    public Map<String, Set<String>> getPendingJobsForProcessDefinitionKey(String processDefinitionKey) {
        return workflowsPendingJobs.get(processDefinitionKey);
    }

    public Map<String, Set<String>> getPendingJobsForProcessInstanceKey(String processInstanceKey) {

        for (String processDefinitionKey : workflowsPendingJobs.keySet()) {
            if (workflowsPendingJobs.get(processDefinitionKey).containsKey(processInstanceKey)) {
                return workflowsPendingJobs.get(processInstanceKey);
            }
        }
        return null;
    }

    public Map<String, Map<String, Set<String>>> getAllPendingJobs() {
        return workflowsPendingJobs;
    }


    public void removePendingJobFromWorkflow(String processDefinitionKey, String processInstanceKey, String jobKey) {
        workflowsPendingJobs.get(processDefinitionKey).get(processInstanceKey).remove(jobKey);
    }

    public void addMessageForProcessDefinitionKey(String processDefinitionKey, String messageName) {
        if (messagesByProcessDefinitionKey.get(String.valueOf(processDefinitionKey)) == null) {
            messagesByProcessDefinitionKey.put(String.valueOf(processDefinitionKey), new HashSet<>());
        }
        messagesByProcessDefinitionKey.get(processDefinitionKey).add(String.valueOf(messageName));
    }

    public Map<String, Set<String>> getAllMessages() {
        return messagesByProcessDefinitionKey;
    }

    public Set<String> getMessagesByProcessDefinitionKey(String processDefinitionKey) {
        return messagesByProcessDefinitionKey.get(processDefinitionKey);
    }

    public void registerStartWorkflowByCloudEvent(WorkflowByCloudEvent wbce) {
        startWorkflows.put(wbce.getCloudEventType(), wbce);
    }

    public WorkflowByCloudEvent getStartWorkflowByCloudEvent(String cloudEventType) {
        return startWorkflows.get(cloudEventType);
    }

    public Map<String, WorkflowByCloudEvent> getStartWorkflowByCloudEvents() {
        return startWorkflows;
    }


}
