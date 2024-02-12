package io.zeebe.cloud.events.router;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.netty.handler.timeout.TimeoutException;

public class CloudEventsJobHandler implements JobHandler{
    
    private final Logger logger = LoggerFactory.getLogger(CloudEventsJobHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void handle(JobClient jobClient, ActivatedJob job) throws IOException, InterruptedException, ExecutionException, TimeoutException {

    }
}
