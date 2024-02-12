package io.zeebe.cloud.events.router;


import io.zeebe.cloudevents.ZeebeCloudEventExtension;
import io.zeebe.cloudevents.CloudEventsHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.zeebe.cloudevents.Headers;
// import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import io.cloudevents.spring.http.CloudEventHttpUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
@EnableZeebeClient
@Service
public class ZeebeCloudEventsRouterWorker {

    private static final Logger log = LoggerFactory.getLogger(ZeebeCloudEventsRouterWorker.class);

    @Autowired
    private CloudEventsZeebeMappingsService mappingsService;

    @Autowired
    private JobClient jobClient;
    
    @Configuration
    public static class CloudEventHandlerConfiguration implements CodecCustomizer {

        @Override
        public void customize(CodecConfigurer configurer) {
            configurer.customCodecs().register(new CloudEventHttpMessageReader());
            configurer.customCodecs().register(new CloudEventHttpMessageWriter());
        }

    }
    
    @ZeebeWorker(name = "cloudevents-router", type = "cloudevents", timeout = 60 * 60 * 24 * 1000)
    public void cloudEventsRouterWorker(final JobClient client, final ActivatedJob job) throws JsonProcessingException {
        logJob(job);

        //from headers
        //@TODO: deal with empty headers for HOST and MODE
        String host = job.getCustomHeaders().get(Headers.HOST);
        String mode = job.getCustomHeaders().get(Headers.MODE);
        
        if (mode != null && mode.equals(ZeebeCloudEventsRouterModes.WAIT_FOR_CLOUD_EVENT.name())) 
        {
            //@TODO: register here as consumer.. this is dynamic consumer
            //mappingsService.registerEventConsumer();
            //waitForCloudEventType = job.getCustomHeaders().get(Headers.CLOUD_EVENT_WAIT_TYPE);
            log.info("WAIT FOR CLOUD EVENT");
            mappingsService.addPendingJob(String.valueOf(job.getProcessDefinitionKey()), String.valueOf(job.getProcessInstanceKey()), String.valueOf(job.getKey()));
            //@TODO: notify the job client that the job was forwarded to an external system. In Node Client this is something like jobCount--;
            // jobClient.newForwardedCommand()
            emitZeebeCloudEventHTTPFromJob(job, host);
        } 
        else if (mode == null || mode.equals("") || mode.equals(ZeebeCloudEventsRouterModes.EMIT_ONLY.name())) 
        {
            emitZeebeCloudEventHTTPFromJob(job, host);
            jobClient.newCompleteCommand(job.getKey()).send().join();

        }

        // log.info("Time now: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // StringBuilder sb = new StringBuilder("worker all jobs: \n");
        // Map<String, Map<String, Set<String>>> all_jobs = mappingsService.getAllPendingJobs();
        // for (Map.Entry<String, Map<String, Set<String>>> entry : all_jobs.entrySet()) {
        //     sb.append(entry.getKey() + " > \n");
        //     for (Map.Entry<String, Set<String>> sub_entry : entry.getValue().entrySet()) {
        //         sb.append("\n " + sub_entry.getKey() + " > " + String.join(",", sub_entry.getValue()));
        //     }
        // }
        // log.info(sb.toString());
    }

    public void emitZeebeCloudEventHTTPFromJob(ActivatedJob job, String host) throws JsonProcessingException {

        final CloudEvent zeebeCloudEvent = createZeebeCloudEventFromJob(job);

        // ZeebeCloudEventExtension zeebeCloudEventExtension = new ZeebeCloudEventExtension();

        // // I need to do the HTTP to Cloud Events mapping here, that means picking up the CorrelationKey header and add it to the Cloud Event
        // zeebeCloudEventExtension.setBpmnActivityId(String.valueOf(job.getElementInstanceKey()));
        // zeebeCloudEventExtension.setBpmnActivityName(job.getElementId());
        // zeebeCloudEventExtension.setJobKey(String.valueOf(job.getKey()));
        // zeebeCloudEventExtension.setProcessDefinitionKey(String.valueOf(job.getProcessDefinitionKey()));
        // zeebeCloudEventExtension.setProcessInstanceKey(String.valueOf(job.getProcessInstanceKey()));
        

        //TODO: 
        // loop counter: search a data_objects field

        // construct a JSONNode
        // search for data_objects node
        // if true -> take it and send it to the client
        // if false -> send directly to the client


        ObjectMapper objectMapper = new ObjectMapper();

        log.info(">>>>> Job Variables: " + objectMapper.writeValueAsString(job.getVariables()));

        JsonNode root = objectMapper.readTree(job.getVariables());

        ArrayNode arrNode = (ArrayNode) root.get("data_objects_collection");

        String body;

        if ((arrNode!=null) && arrNode.isArray()) {
            log.info("IS AN ARRAY");
            int counter = root.get("loopCounter").asInt();

            JsonNode elt = arrNode.get(counter-1);
            body = objectMapper.writeValueAsString(elt);
        }
        else
        {
            log.info("IS NOT AN ARRAY");
            JsonNode t = root.get("data_objects");

            if(t == null)
            {
                log.info("field name is null ");
                return;
            }
            
            body = objectMapper.writeValueAsString(t);

        }

        log.info(">>>>> Activity: " + body);

        // ActivityJob activity = objectMapper.readValue(job.getVariables(), ActivityJob.class);	
        // log.info(">>>>> Activity: " + body);
        
        // String tmp = objectMapper.writeValueAsString(activity);
        // log.info(">>>>> tmp: " + tmp.toString());

        // CloudEvent zeebeCloudEvent = CloudEventBuilder.v1()
        //         .withId(UUID.randomUUID().toString())
        //         .withTime(OffsetDateTime.now()) // bug-> https://github.com/cloudevents/sdk-java/issues/200
        //         .withType(job.getCustomHeaders().get(Headers.CLOUD_EVENT_TYPE)) // from headers
        //         .withSource(URI.create("http://workflow-zeebe.workflow.svc.cluster.local"))
        //         // .withData(job.getVariables().getBytes())
        //         // .withData("application/json", JsonCloudEventData.wrap(variables)) 
        //         // .withData(objectMapper.writeValueAsBytes(activity)) // job.getVariables().getBytes())
        //         // .withData(objectMapper.writeValueAsString(activity).getBytes())//PojoCloudEventData.wrap(activity, objectMapper::writeValueAsBytes))
        //         // .withData("{\"uri\":\"Dave\"}".getBytes())
        //         // .withData(objectMapper.writeValueAsString(activity).getBytes(StandardCharsets.UTF_8))
        //         // .withDataContentType(Headers.CONTENT_TYPE)
        //         .withDataContentType("application/json; charset=UTF-8")
        //         .withSubject("Zeebe Job")
        //         .withExtension(zeebeCloudEventExtension)
        //         .build();


        log.info("cloudEvent >>> " + zeebeCloudEvent.toString());
        // WebClient webClient = WebClient.builder().baseUrl(host)
        // .filters(exchangeFilterFunctions -> {
        //     exchangeFilterFunctions.add(logRequest());
        //     // exchangeFilterFunctions.add(logResponse());
        // }).build();


        HttpHeaders outgoing = CloudEventHttpUtils.toHttp(zeebeCloudEvent);

        // StringBuilder sb = new StringBuilder("Request: \n");
        // //append clientRequest method and url
        // log.info("Request: " + "POST - " + host);        
        // outgoing.forEach((name, values) -> values.forEach(value -> sb.append(name + "=" + value + " ")));

        // log.info(sb.toString());

        WebClient.builder()
                .baseUrl(host)
                .filters(exchangeFilterFunctions -> {
                    exchangeFilterFunctions.add(logRequest());
                    // exchangeFilterFunctions.add(logResponse());
                })
                .build() //     ProcessInstanceKey
                .post()           
                .headers(httpHeaders -> httpHeaders.putAll(outgoing))
                .bodyValue(body).retrieve().bodyToMono(String.class).doOnError(t -> t.printStackTrace())
                                .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();
                // .post() //
                // .bodyValue(zeebeCloudEvent) //
                // .retrieve()
                // .bodyToMono(String.class)
				// .doOnError(t -> t.printStackTrace())
				// .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();


                // .exchangeToMono(response -> {
                //     if (response.statusCode().equals(HttpStatus.OK)) {
                //         return response.bodyToMono(CloudEvent.class);
                //     }
                //     else {
                //         return response.createException().flatMap(Mono::error);
                //     }
                // });
                
                
                // (response -> response.bodyToMono(CloudEvent.class));
      
                // WebClient.RequestBodySpec uri = webClient.post().uri(uriString);
                // WebClient.RequestHeadersSpec<?> headersSpec = uri.body(BodyInserters.fromValue(cloudEvent.getData()));
                // WebClient.RequestHeadersSpec<?> header = headersSpec
                //         .header(CE_ID, cloudEvent.getId())
                //         .header(CE_SPECVERSION, cloudEvent.getSpecVersion().toString())
                //         .header(CONTENT_TYPE, APPLICATION_JSON)
                //         .header(CE_TYPE, cloudEvent.getType())
                //         .header(CE_TIME, (cloudEvent.getTime() != null)?cloudEvent.getTime().toString(): OffsetDateTime.now().toZonedDateTime().toString())
                //         .header(CE_SOURCE, (cloudEvent.getSource() != null)?cloudEvent.getSource().toString():"")
                //         .header(CE_SUBJECT, cloudEvent.getSubject());
        
        
                //@TODO: improve extensions handling, at least now we will have a string version of the extension
                // for (String key : cloudEvent.getExtensionNames()) {
                //     header.header(key, cloudEvent.getExtension(key).toString());
                // }
                // return header.retrieve();

        // WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, myCloudEvent);

        // log.info("WebClient.ResponseSpec: " + postCloudEvent.toString());

        // postCloudEvent.bodyToMono(String.class).doOnError(t -> t.printStackTrace())
        //         .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();
    }

    /*
     * This method will create a Zeebe Cloud Event from an ActivatedJob inside a worker, this allow other systems to consume
     * this Cloud Event and
     */
    public CloudEvent createZeebeCloudEventFromJob(ActivatedJob job) throws JsonProcessingException {


        final ZeebeCloudEventExtension zeebeCloudEventExtension = new ZeebeCloudEventExtension();

        // I need to do the HTTP to Cloud Events mapping here, that means picking up the CorrelationKey header and add it to the Cloud Event
        zeebeCloudEventExtension.setBpmnActivityId(String.valueOf(job.getElementInstanceKey()));
        zeebeCloudEventExtension.setBpmnActivityName(job.getElementId());
        zeebeCloudEventExtension.setJobKey(String.valueOf(job.getKey()));
        zeebeCloudEventExtension.setProcessDefinitionKey(String.valueOf(job.getProcessDefinitionKey()));
        zeebeCloudEventExtension.setProcessInstanceKey(String.valueOf(job.getProcessInstanceKey()));
        

        // ObjectMapper objectMapper = new ObjectMapper();

        // JsonNode root = objectMapper.readTree(job.getVariables());

        // JsonNode data_objects = root.get("data_objects_collection");

        // String body = objectMapper.writeValueAsString(data_objects);
        
        // log.info(">>>>> Job Variables: " + objectMapper.writeValueAsString(job.getVariables()));
        // log.info(">>>>> Activity: " + body);

        // ObjectMapper objectMapper = new ObjectMapper();
        // log.info(">>>>> Job Variables: " + objectMapper.writeValueAsString(job.getVariables()));


        // ActivityJob activity = objectMapper.readValue(job.getVariables(), ActivityJob.class);	
        // log.info(">>>>> Activity: " + activity.toString());
        
 
        // String tmp = objectMapper.writeValueAsString(activity);
        // log.info(">>>>> tmp: " + tmp.toString());

        final CloudEvent zeebeCloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withTime(OffsetDateTime.now()) // bug-> https://github.com/cloudevents/sdk-java/issues/200
                .withType(job.getCustomHeaders().get(Headers.CLOUD_EVENT_TYPE)) // from headers
                .withSource(URI.create("workflow-zeebe.workflow.svc.cluster.local"))
                // .withData(job.getVariables().getBytes())
                // .withData("application/json", JsonCloudEventData.wrap(variables)) 
                // .withData(objectMapper.writeValueAsBytes(activity)) // job.getVariables().getBytes())
                // .withData(objectMapper.writeValueAsString(activity).getBytes())//PojoCloudEventData.wrap(activity, objectMapper::writeValueAsBytes))
                //.withDataContentType(Headers.CONTENT_TYPE)          
                // .withData(tmp.getBytes())
                .withDataContentType("application/json; charset=UTF-8")
                .withSubject("Zeebe Job")
                .withExtension(zeebeCloudEventExtension)
                .build();

        return zeebeCloudEvent;
    }

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Request: \n");
                //append clientRequest method and url
                log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
                clientRequest
                  .headers()
                  .forEach((name, values) -> values.forEach(value -> sb.append(name + "=" + value + " ")));
                log.info(sb.toString());
            // }
            return Mono.just(clientRequest);
        });
    }
    

    private static void logJob(final ActivatedJob job) {
        log.info(
                "complete job\n>>> [type: {}, key: {}, element: {}, process instance: {}]\n{deadline; {}]\n[headers: {}]\n[variables: {}]",
                job.getType(),
                job.getKey(),
                job.getElementId(),
                job.getProcessInstanceKey(),
                Instant.ofEpochMilli(job.getDeadline()),
                job.getCustomHeaders(),
                job.getVariables());
    }
}
