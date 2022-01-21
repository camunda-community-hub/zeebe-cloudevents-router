package io.zeebe.cloudevents;

import io.cloudevents.CloudEvent;


import io.cloudevents.Extension;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

public class CloudEventsHelper {

    public static final String CE_ID = "Ce-Id";
    public static final String CE_TYPE = "Ce-Type";
    public static final String CE_SOURCE = "Ce-Source";
    public static final String CE_SPECVERSION = "Ce-Specversion";
    public static final String CE_TIME = "Ce-Time";
    public static final String CE_SUBJECT = "Ce-Subject";

    public static final String APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE = "Content-Type";


    public static CloudEvent parseFromRequest(HttpHeaders headers, Object body) throws IllegalStateException {

        return parseFromRequestWithExtension(headers, body, null);
    }


    public static CloudEvent parseFromRequestWithExtension(HttpHeaders headers, Object body, Extension extension){
        if (headers.get(CE_ID) == null || (headers.get(CE_SOURCE) == null || headers.get(CE_TYPE) == null)) {
            throw new IllegalStateException("Cloud Event required fields are not present.");
        }

        CloudEventBuilder builder = CloudEventBuilder.v1()
                .withId(headers.getFirst(CE_ID))
                .withType(headers.getFirst(CE_TYPE))
                .withSource((headers.getFirst(CE_SOURCE) != null) ? URI.create(headers.getFirst(CE_SOURCE)) : null)
                .withTime((headers.getFirst(CE_TIME) != null) ? ZonedDateTime.parse(headers.getFirst(CE_TIME)) : null)
                .withData(headers.getFirst(CONTENT_TYPE), body.toString().getBytes())
                .withSubject(headers.getFirst(CE_SUBJECT))
                .withDataContentType((headers.getFirst(CONTENT_TYPE) != null) ? headers.getFirst(CONTENT_TYPE) : APPLICATION_JSON);

        if (extension != null) {
            builder = builder.withExtension(extension);
        }
        return builder.build();
    }

    public static WebClient.ResponseSpec createPostCloudEvent(WebClient webClient, CloudEvent cloudEvent) {
        return createPostCloudEvent(webClient,"", cloudEvent);
    }

    public static WebClient.ResponseSpec createPostCloudEvent(WebClient webClient, String uriString, CloudEvent cloudEvent) {
        WebClient.RequestBodySpec uri = webClient.post().uri(uriString);
        WebClient.RequestHeadersSpec<?> headersSpec = uri.body(BodyInserters.fromValue(new String(cloudEvent.getData())));
        WebClient.RequestHeadersSpec<?> header = headersSpec
                .header(CE_ID, cloudEvent.getId())
                .header(CE_SPECVERSION, cloudEvent.getSpecVersion().toString())
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .header(CE_TYPE, cloudEvent.getType())
                .header(CE_TIME, (cloudEvent.getTime() != null)?cloudEvent.getTime().toString(): OffsetDateTime.now().toZonedDateTime().toString())
                .header(CE_SOURCE, (cloudEvent.getSource() != null)?cloudEvent.getSource().toString():"")
                .header(CE_SUBJECT, cloudEvent.getSubject());


        //@TODO: improve extensions handling, at least now we will have a string version of the extension
        for (String key : cloudEvent.getExtensionNames()) {
            header.header(key, cloudEvent.getExtension(key).toString());
        }
        return header.retrieve();
    }


}
