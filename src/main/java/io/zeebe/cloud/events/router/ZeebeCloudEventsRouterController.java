package io.zeebe.cloud.events.router;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.cloudevents.helper.CloudEventsHelper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;

import io.cloudevents.jackson.JsonFormat;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.cloudevents.ZeebeCloudEventExtension;
import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ZeebeCloudEventsRouterController {

    @Autowired
    private ZeebeClientLifecycle zeebeClient;


    @Autowired
    private CloudEventsZeebeMappingsService mappingsService;

    @Autowired
    private JobClient jobClient;

    @GetMapping("/status")
    public String getStatus() {
        log.info("> Broker Contact Point: " + zeebeClient.getConfiguration().getBrokerContactPoint());
        log.info("> Plain Text Connection Enabled: " + zeebeClient.getConfiguration().isPlaintextConnectionEnabled());
        return "{ \"zeebe.broker.contactPoint\": " + zeebeClient.getConfiguration().getBrokerContactPoint() + ", " +
                "\"plainTextConnection\":" + zeebeClient.getConfiguration().isPlaintextConnectionEnabled() + "}";
    }

    @GetMapping("/jobs")
    public String printPendingJobs() {
        Map<String, Map<String, Set<String>>> jobs = mappingsService.getAllPendingJobs();
        return jobs.keySet().stream()
                .map(key -> key + "=" + jobs.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @GetMapping("/messages")
    public String messages() {
        Map<String, Set<String>> allExpectedBPMNMessages = mappingsService.getAllMessages();
        return allExpectedBPMNMessages.keySet().stream()
                .map(key -> key + "=" + allExpectedBPMNMessages.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    @PostMapping("/")
    public String receiveCloudEvent(@RequestHeader HttpHeaders headers, @RequestBody Object body) {
        CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);

        logCloudEvent(cloudEvent);

        String workflowKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.WORKFLOW_KEY);
        String workflowInstanceKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.WORKFLOW_INSTANCE_KEY);
        String jobKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.JOB_KEY);

        if(workflowKey == null || workflowInstanceKey == null || jobKey == null){
            throw new IllegalStateException("Cloud Event missing Zeebe Extension fields, which are required to complete a job");
        }

        Set<String> pendingJobs = mappingsService.getPendingJobsForWorkflowKey(workflowKey).get(workflowInstanceKey);
        if (pendingJobs != null) {
            if (!pendingJobs.isEmpty()) {
                if (pendingJobs.contains(jobKey)) {
                    //@TODO: deal with Optionals for Data
                    jobClient.newCompleteCommand(Long.valueOf(jobKey)).variables(new String(cloudEvent.getData())).send().join();
                    mappingsService.removePendingJobFromWorkflow(workflowKey, workflowInstanceKey, jobKey);
                } else {
                    log.error("Job Key: " + jobKey + " not found");
                    throw new IllegalStateException("Job Key: " + jobKey + " not found");
                }
            } else {
                log.error("This workflow instance key: " + workflowInstanceKey + " doesn't have any pending jobs");
                throw new IllegalStateException("This workflow instance key: " + workflowInstanceKey + " doesn't have any pending jobs");
            }
        } else {
            log.error("Workflow instance key: " + workflowInstanceKey + " not found");
            throw new IllegalStateException("Workflow instance key: " + workflowInstanceKey + " not found");
        }

        // @TODO: decide on return types
        return "OK!";
    }

    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);
        log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));
    }

    @PostMapping("/workflows")
    public void addStartWorkflowCloudEventMapping(@RequestBody WorkflowByCloudEvent wbce) {
        mappingsService.registerStartWorkflowByCloudEvent(wbce);
    }

    @GetMapping("/workflows")
    public Map<String, WorkflowByCloudEvent> getStartWorkflowCloudEventMapping() {
        return mappingsService.getStartWorkflowByCloudEvents();
    }

    @DeleteMapping("/workflow")
    public void cancelWorkflow(@RequestHeader HttpHeaders headers, @RequestBody Map<String, String> body) {
        CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
        logCloudEvent(cloudEvent);
        String workflowInstanceKey = (String)cloudEvent.getExtension(ZeebeCloudEventExtension.WORKFLOW_INSTANCE_KEY);
        if(workflowInstanceKey != null && !workflowInstanceKey.equals("")) {
            zeebeClient.newCancelInstanceCommand(Long.valueOf(workflowInstanceKey))
                    .send().join();
            log.info("Cancelling Workflow Instance: " + workflowInstanceKey);
        }else{
            log.error("There is no Workflow Instance Key available in the Cloud Event: " + cloudEvent);
        }

    }

    @PostMapping("/workflow")
    public void startWorkflow(@RequestHeader HttpHeaders headers, @RequestBody Map<String, String> body) {
        CloudEvent cloudEvent = CloudEventsHelper.parseFromRequest(headers.toSingleValueMap(), body);
        WorkflowByCloudEvent workflowByCloudEvent = mappingsService.getStartWorkflowByCloudEvent(cloudEvent.getType());
        if (workflowByCloudEvent.getBpmnProcessId() != null && !workflowByCloudEvent.getBpmnProcessId().equals("")) {
            //@TODO: deal with empty body for variables
            if (workflowByCloudEvent.getVersion() == null || workflowByCloudEvent.getVersion().equals("")) {
                zeebeClient.newCreateInstanceCommand().bpmnProcessId(workflowByCloudEvent.getBpmnProcessId())
                        .latestVersion().variables(body)
                        .send()
                        .join();
            } else {
                zeebeClient.newCreateInstanceCommand().bpmnProcessId(workflowByCloudEvent.getBpmnProcessId())
                        .version(Integer.valueOf(workflowByCloudEvent.getVersion()))
                        .variables(body)
                        .send()
                        .join();
            }
        } else if (workflowByCloudEvent.getWorkflowKey() != null && !workflowByCloudEvent.getWorkflowKey().equals("")) {
            zeebeClient.newCreateInstanceCommand().workflowKey(Long.valueOf(workflowByCloudEvent.getWorkflowKey()))
                    .variables(body)
                    .send()
                    .join();
        } else {
            log.error("No workflow was started with: " + workflowByCloudEvent.toString());
        }
    }

    @PostMapping("/error")
    public void receiveCloudEventForError(@RequestHeader HttpHeaders headers, @RequestBody Object body){
        CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
        logCloudEvent(cloudEvent);

        log.info("I should emit a BPMN Error here... ");
        //zeebeClient.newThrowErrorCommand().errorCode()
    }

    @PostMapping("/messages")
    public void addExpectedMessage(@RequestBody MessageForWorkflowKey messageForWorkflowKey) {
        //@TODO: Next step check and advertise which messages are expected by which workflows
        //       This can be scanned on Deploy Workflow, and we can use that to register the workflow as a consumer of events
        mappingsService.addMessageForWorkflowKey(messageForWorkflowKey.getWorkflowKey(), messageForWorkflowKey.getMessageName());
    }

    @PostMapping("/message")
    public String receiveCloudEventForMessage(@RequestHeader HttpHeaders headers, @RequestBody Object body) throws JsonProcessingException {
        CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
        logCloudEvent(cloudEvent);

        //@TODO: deal with empty type and no correlation key.
        String cloudEventType = cloudEvent.getType();
        String correlationKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.CORRELATION_KEY);

        //@TODO: deal with optional for Data, for empty Data
        zeebeClient.newPublishMessageCommand()
                .messageName(cloudEventType)
                .correlationKey(correlationKey)
                .variables(new String(cloudEvent.getData()))
                .send().join();

        // @TODO: decide on return types
        return "OK!";
    }
}
