package io.zeebe.cloud.events.router;


import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import reactor.core.publisher.Mono;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.zeebe.cloudevents.CloudEventsHelper;


import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;




import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.knative.eventing.v1.TriggerList;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.zeebe.cloudevents.ZeebeCloudEventExtension;
import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.model.bpmn.instance.Definitions;
import io.camunda.zeebe.model.bpmn.instance.Message;
import io.camunda.zeebe.client.ZeebeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.http.codec.CodecConfigurer;

@RestController
public class ZeebeCloudEventsRouterController {

    private static final Logger log = LoggerFactory.getLogger(ZeebeCloudEventsRouterController.class);

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CloudEventsZeebeMappingsService mappingsService;

    @Autowired
    private JobClient jobClient;


    @GetMapping("/status")
    public String getStatus() {
        log.info("> Broker Gateway Address: " + zeebeClient.getConfiguration().getGatewayAddress());
        log.info("> Plain Text Connection Enabled: " + zeebeClient.getConfiguration().isPlaintextConnectionEnabled());
        return "{ \"zeebe.broker.gatewayAddress\": " + zeebeClient.getConfiguration().getGatewayAddress() + ", " +
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
    public void completeProcessInstance(@RequestBody ObjectNode root, @RequestHeader HttpHeaders headers) throws JsonProcessingException {
        
        CloudEvent cloudEvent = CloudEventHttpUtils.fromHttp(headers).build();
        
        String body = objectMapper.writeValueAsString(root);
        String processDefinitionKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.PROCESS_DEFINITION_KEY);
        String processInstanceKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.PROCESS_INSTANCE_KEY);
        String jobKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.JOB_KEY);

        logCloudEvent(cloudEvent);
        log.debug(body);


        if (processDefinitionKey == null || processInstanceKey == null || jobKey == null) {
            throw new IllegalStateException("Cloud Event missing Zeebe Extension fields, which are required to complete a job");
        }

        Set<String> pendingJobs = mappingsService.getPendingJobsForProcessDefinitionKey(processDefinitionKey).get(processInstanceKey);
        if (pendingJobs != null) {
            if (!pendingJobs.isEmpty()) {
                if (pendingJobs.contains(jobKey)) {
                    //@TODO: deal with Optionals for Data
                    jobClient.newCompleteCommand(Long.valueOf(jobKey)).variables(body).send().join();
                    mappingsService.removePendingJobFromWorkflow(processDefinitionKey, processInstanceKey, jobKey);
                } else {
                    log.error("Job Key: " + jobKey + " not found");
                    throw new IllegalStateException("Job Key: " + jobKey + " not found");
                }
            } else {
                log.error("This workflow instance key: " + processInstanceKey + " doesn't have any pending jobs");
                throw new IllegalStateException("This workflow instance key: " + processInstanceKey + " doesn't have any pending jobs");
            }
        } else {
            log.error("Workflow instance key: " + processInstanceKey + " not found");
            throw new IllegalStateException("Workflow instance key: " + processInstanceKey + " not found");
        }

        // @TODO: decide on return types
       // return "OK!";
    }

    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);
        log.info("CloudEvent: " + new String(format.serialize(cloudEvent)));
    }

    
    /**
     * Map a BPMN Process ID with a CloudEvent type
     * @param wbce 
     */
    @PostMapping("/workflows")
    public void addStartWorkflowCloudEventMapping(@RequestBody WorkflowByCloudEvent wbce) {        
        mappingsService.registerStartWorkflowByCloudEvent(wbce);
    }

    /**
     * Retuns all mappings between BPMN Process IDs and CloudEvent types
     * @return
     */
    @GetMapping("/workflows")
    public Map<String, WorkflowByCloudEvent> getStartWorkflowCloudEventMapping() {
        return mappingsService.getStartWorkflowByCloudEvents();
    }

    // @DeleteMapping("/workflow")
    // public void cancelWorkflow(@RequestHeader HttpHeaders headers, @RequestBody Map<String, String> body) {
    //     CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
    //     logCloudEvent(cloudEvent);
    //     String processInstanceKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.PROCESS_INSTANCE_KEY);
    //     if (processInstanceKey != null && !processInstanceKey.equals("")) {
    //         zeebeClient.newCancelInstanceCommand(Long.valueOf(processInstanceKey))
    //                 .send().join();
    //         log.info("Cancelling Workflow Instance: " + processInstanceKey);
    //     } else {
    //         log.error("There is no Workflow Instance Key available in the Cloud Event: " + cloudEvent);
    //     }
        
    // }


    /**
     * Very import, the CE-TYPE states the workflow pipeline to start
     * 
     */
    
    /*
        TODO 
        A pipeline is per scenario
        If the ActivityJob is a set of DataobjectCollections then
        we should start a pipeline for each DataobjectCollection
    */

    @PostMapping("/workflow")
    public void createProcessInstance(@RequestBody ObjectNode root, @RequestHeader HttpHeaders headers) throws JsonProcessingException {

        String body = objectMapper.writeValueAsString(root);        

        CloudEvent attributes = CloudEventHttpUtils.fromHttp(headers).build();

        logCloudEvent(attributes);
        log.info("body : " + body);

        // String body = objectMapper.writeValueAsString(foo);
        
        WorkflowByCloudEvent workflowByCloudEvent = mappingsService.getStartWorkflowByCloudEvent(attributes.getType());

        if(workflowByCloudEvent == null)
        {
            log.error("wrong started event");
            return;
        }
        
        if (workflowByCloudEvent.getBpmnProcessId() != null && !workflowByCloudEvent.getBpmnProcessId().equals("")) {
            //@TODO: deal with empty body for variables
            if (workflowByCloudEvent.getVersion() == null || workflowByCloudEvent.getVersion().equals("")) {
                zeebeClient.newCreateInstanceCommand().bpmnProcessId(workflowByCloudEvent.getBpmnProcessId())
                        .latestVersion().variables(body)
                        .send()
                        .join();
            } else {
                log.info("getBpmnProcessId ok, getVersion ok");                
                zeebeClient.newCreateInstanceCommand().bpmnProcessId(workflowByCloudEvent.getBpmnProcessId())
                        .version(Integer.valueOf(workflowByCloudEvent.getVersion()))
                        .variables(body)
                        .send()
                        .join();
            }
        } else if (workflowByCloudEvent.getProcessDefinitionKey() != null && !workflowByCloudEvent.getProcessDefinitionKey().equals("")) {
            zeebeClient.newCreateInstanceCommand().processDefinitionKey(Long.valueOf(workflowByCloudEvent.getProcessDefinitionKey()))
                    .variables(body)
                    .send()
                    .join();
        } else {
            log.error("No workflow was started with: " + workflowByCloudEvent.toString());
        }


        // StringBuilder sb = new StringBuilder("/workflow all jobs: \n");
        // Map<String, Map<String, Set<String>>> all_jobs = mappingsService.getAllPendingJobs();
        // for (Map.Entry<String, Map<String, Set<String>>> entry : all_jobs.entrySet()) {
        //     sb.append(entry.getKey() + " > \n");
        //     for (Map.Entry<String, Set<String>> sub_entry : entry.getValue().entrySet()) {
        //         sb.append("\n " + sub_entry.getKey() + " > " + String.join(",", sub_entry.getValue()));
        //     }
        // }
        // log.info(sb.toString());
    }

    // @Configuration
	// public static class CloudEventHandlerConfiguration implements CodecCustomizer {

	// 	@Override
	// 	public void customize(CodecConfigurer configurer) {
	// 		configurer.customCodecs().register(new CloudEventHttpMessageReader());
	// 		configurer.customCodecs().register(new CloudEventHttpMessageWriter());
	// 	}

	// }

    
    // @PostMapping("/error")
    // public void receiveCloudEventForError(@RequestHeader HttpHeaders headers, @RequestBody Object body) {
    //     CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
    //     logCloudEvent(cloudEvent);

    //     log.info("I should emit a BPMN Error here... ");
    //     //zeebeClient.newThrowErrorCommand().errorCode()
    // }

    // @PostMapping("/messages")
    // public void addExpectedMessage(@RequestBody MessageForWorkflowKey messageForWorkflowKey) {
    //     //@TODO: Next step check and advertise which messages are expected by which workflows
    //     //       This can be scanned on Deploy Workflow, and we can use that to register the workflow as a consumer of events
    //     mappingsService.addMessageForWorkflowKey(messageForWorkflowKey.getProcessDefinitionKey(), messageForWorkflowKey.getMessageName());
    // }

    @PostMapping("/message")
    public String receiveCloudEventForMessage(@RequestHeader HttpHeaders headers, @RequestBody Object body) throws JsonProcessingException {
        log.info("Request Headers in the Router: " + headers.toString());
        CloudEvent cloudEvent = ZeebeCloudEventsHelper.parseZeebeCloudEventFromRequest(headers, body);
        logCloudEvent(cloudEvent);

        //@TODO: deal with empty type and no correlation key.
        String cloudEventType = cloudEvent.getType();
        String correlationKey = (String) cloudEvent.getExtension(ZeebeCloudEventExtension.CORRELATION_KEY);


        //@TODO: deal with optional for Data, for empty Data
        zeebeClient.newPublishMessageCommand()
                .messageName(cloudEventType)
                .correlationKey(correlationKey)
                .variables(cloudEvent.getData().toString())
                .send().join();

        // @TODO: decide on return types
        return "OK!";
    }
}


// @PostMapping("/deploy")
    // public void deployWorkflow(@RequestBody DeployWorkflowPayload dwp) {
    //     try (KnativeClient kn = new DefaultKnativeClient()) {
    //         // Get all Service objects
    //         TriggerList triggerList = kn.triggers()
    //                 .inNamespace("default")
    //                 .list();
    //         // Iterate through list and print names
    //         for (Trigger trigger : triggerList.getItems()) {
    //             System.out.println(trigger.getMetadata().getName() + " -> Broker: " + trigger.getSpec().getBroker() + " -> Filter attr type: " + trigger.getSpec().getFilter().getAttributes().get("type") + " -> Subscriber: " + trigger.getSpec().getSubscriber().getUri());
    //         }


    //         BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromStream(new ByteArrayInputStream(dwp.getWorkflowDefinition().getBytes()));
    //         Definitions definitions = bpmnModelInstance.getDefinitions();

    //         Collection<Message> messages = definitions.getModelInstance().getModelElementsByType(Message.class);
    //         for (Message m : messages) {
    //             log.info("Message: " + m.getName());
    //             kn.triggers().createOrReplace(
    //                 new TriggerBuilder()
    //                     .withNewMetadata()
    //                         .withName("router-" + m.getName().replace(".", "-").toLowerCase())
    //                     .endMetadata()
    //                     .withNewSpec()
    //                         .withBroker("default")
    //                         .withNewFilter()
    //                             .addToAttributes("type", m.getName())
    //                         .endFilter()
    //                     .withNewSubscriber()
    //                         .withUri("http://zeebe-cloud-events-router.default.svc.cluster.local/message")
    //                     .endSubscriber()
    //                     .endSpec()
    //                     .build());
    //         }


    //         DeploymentEvent deploymentEvent = zeebeClient.newDeployCommand().addProcessModel(bpmnModelInstance, dwp.getName()).send().join();
    //         log.info("Deployment Event: " + deploymentEvent);

    //     }
    // }
