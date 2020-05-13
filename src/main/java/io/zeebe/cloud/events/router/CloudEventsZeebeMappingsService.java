package io.zeebe.cloud.events.router;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//@TODO: refine interface.. to many leaked types here
@Service
public class CloudEventsZeebeMappingsService {
    // WorkflowKey, WorkflowInstanceKey, Jobs
    private Map<String, Map<String, Set<String>>> workflowsPendingJobs = new HashMap<>();

    private Map<String, Set<String>> messagesByWorkflowKey = new HashMap<>();

    private Map<String, String> startWorkflows = new HashMap<>();

    public CloudEventsZeebeMappingsService() {

    }

    public void addPendingJob(String workflowKey, String workflowInstanceKey, String jobKey) {
        if (workflowsPendingJobs.get(workflowKey) == null) {
            workflowsPendingJobs.put(workflowKey, new HashMap<>());
        }
        if (!workflowsPendingJobs.get(workflowKey).containsKey(workflowInstanceKey)) {
            workflowsPendingJobs.get(workflowKey).put(workflowInstanceKey, new HashSet<>());
        }
        workflowsPendingJobs.get(workflowKey).get(workflowInstanceKey).add(jobKey);
    }

    public Map<String, Set<String>> getPendingJobsForWorkflowKey(String workflowKey) {
        return workflowsPendingJobs.get(workflowKey);
    }

    public Map<String, Set<String>> getPendingJobsForWorkflowInstanceKey(String workflowInstanceKey) {

        for (String workflowKey : workflowsPendingJobs.keySet()) {
            if (workflowsPendingJobs.get(workflowKey).containsKey(workflowInstanceKey)) {
                return workflowsPendingJobs.get(workflowInstanceKey);
            }
        }
        return null;
    }

    public Map<String, Map<String, Set<String>>> getAllPendingJobs() {
        return workflowsPendingJobs;
    }


    public void removePendingJobFromWorkflow(String workflowKey, String workflowInstanceKey, String jobKey) {
        workflowsPendingJobs.get(workflowKey).get(workflowInstanceKey).remove(jobKey);
    }

    public void addMessageForWorkflowKey(String workflowKey, String messageName) {
        if (messagesByWorkflowKey.get(String.valueOf(workflowKey)) == null) {
            messagesByWorkflowKey.put(String.valueOf(workflowKey), new HashSet<>());
        }
        messagesByWorkflowKey.get(workflowKey).add(String.valueOf(messageName));
    }

    public Map<String, Set<String>> getAllMessages() {
        return messagesByWorkflowKey;
    }

    public Set<String> getMessagesByWorkflowKey(String workflowKey) {
        return messagesByWorkflowKey.get(workflowKey);
    }

    public void registerStartWorkflowByCloudEvent(String cloudEventType, String bpmnProcessId) {
        startWorkflows.put(cloudEventType, bpmnProcessId);
    }

    public String getStartWorkflowByCloudEvent(String cloudEventType) {
        return startWorkflows.get(cloudEventType);
    }

    public Map<String, String> getStartWorkflowByCloudEvents() {
        return startWorkflows;
    }


}
