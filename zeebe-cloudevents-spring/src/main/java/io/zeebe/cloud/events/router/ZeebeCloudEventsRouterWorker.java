package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.zeebe.cloudevents.Headers;
import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@EnableZeebeClient
@Service
public class ZeebeCloudEventsRouterWorker {

    @Autowired
    private CloudEventsZeebeMappingsService mappingsService;

    @Autowired
    private JobClient jobClient;

    @ZeebeWorker(name = "cloudevents-router", type = "cloudevents", timeout = 60 * 60 * 24 * 1000)
    public void cloudEventsRouterWorker(final JobClient client, final ActivatedJob job) throws JsonProcessingException {
        logJob(job);
        //from headers
        //@TODO: deal with empty headers for HOST and MODE
        String host = job.getCustomHeaders().get(Headers.HOST);
        String mode = job.getCustomHeaders().get(Headers.MODE);
        String waitForCloudEventType = "";


        if (mode != null && mode.equals(ZeebeCloudEventsRouterModes.WAIT_FOR_CLOUD_EVENT.name())) {
            //@TODO: register here as consumer.. this is dynamic consumer
            //mappingsService.registerEventConsumer();
            //waitForCloudEventType = job.getCustomHeaders().get(Headers.CLOUD_EVENT_WAIT_TYPE);

            mappingsService.addPendingJob(String.valueOf(job.getWorkflowKey()), String.valueOf(job.getWorkflowInstanceKey()), String.valueOf(job.getKey()));
            //@TODO: notify the job client that the job was forwarded to an external system. In Node Client this is something like jobCount--;
            //jobClient.newForwardedCommand()..
            ZeebeCloudEventsHelper.emitZeebeCloudEventHTTPFromJob(job, host);
        } else if (mode == null || mode.equals("") || mode.equals(ZeebeCloudEventsRouterModes.EMIT_ONLY.name())) {
            ZeebeCloudEventsHelper.emitZeebeCloudEventHTTPFromJob(job, host);
            jobClient.newCompleteCommand(job.getKey()).send().join();

        }


    }

    private static void logJob(final ActivatedJob job) {
        log.info(
                "complete job\n>>> [type: {}, key: {}, element: {}, workflow instance: {}]\n{deadline; {}]\n[headers: {}]\n[variables: {}]",
                job.getType(),
                job.getKey(),
                job.getElementId(),
                job.getWorkflowInstanceKey(),
                Instant.ofEpochMilli(job.getDeadline()),
                job.getCustomHeaders(),
                job.getVariables());
    }
}
