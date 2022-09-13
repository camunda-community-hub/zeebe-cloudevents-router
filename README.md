[![Community Extension](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community) [![Lifecycle: Incubating](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-) ![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)

# Zeebe Knative / CloudEvents Worker

This project focuses on providing a bridge between [Zeebe](http://zeebe.io/) + [BPMN](https://www.bpmn.org/) to [Knative](https://knative.dev/) and [CloudEvents](https://cloudevents.io/) to provide CloudEvents orchestration using Zeebe Workflows. 

# Service Task Properties

Properties
- HOST
- TYPE
- MODE: 
  - EMIT_ONLY: 
  - WAIT_FOR_CLOUD_EVENT: 
- WAIT_TYPE: Cloud Event Type to wait 

The worker has two modes:
- EMIT ONLY: It will emit a CloudEvent and complete the Job
- WAIT FOR CLOUD EVENT: It will wait to receive CloudEvent with a specific Type which will be correlated by the workflow and job key to complete the Service Task. 

# Endpoints

The worker exposes HTTP Endpoints to receive CloudEvents that can be propagated to workflows. 

- / POST - > Receive CloudEvent via HTTP that will map to a Job
- /message POST -> Receive a CloudEvent that will be forwarded as a BPMN Message for an Intermediate Catch Event 

You can always access the Open API UI here: http://localhost:8080/swagger-ui.html

# Examples

## EMIT and WAIT

> zbctl deploy emit-wait.bpmn --insecure
>
> zbctl create instance EMIT_WAIT  --insecure
>
> curl -X POST localhost:8080/ -H "Content-Type: application/json" -H "Ce-Id: 536808d3" -H "Ce-Type: <WAIT_TYPE>" -H "Ce-Source: curl" -H "Ce-Subject: <WORKFLOW_KEY>:<WORKFLOW_INSTANCE_KEY>:<JOB_KEY>"  -d '{"name":"salaboy"}'  -v
>

## EMIT and CONTINUE:
> zbctl deploy emit-and-continue.bpmn --insecure
> zbctl create instance EMIT_AND_CONTINUE --variables "{\"myVarId\" : \"123\"}" --insecure
>  curl -X POST localhost:8080/message -H "Content-Type: application/json" -H "Ce-Id: 536808d3" -H "Ce-Type: CloudEvent Response" -H "Ce-Source: curl" -H "CorrelationKey: 123" -d '{"name":"salaboy"}'  -v 

## TICKETS
Deploy workflow
> zbctl deploy tickets.bpmn --insecure

Register Tickets.Purchase event to Start Workflow
> curl -X POST http://localhost:8080/workflows -H "Content-Type: application/json" -d '{"cloudEventType" : "Tickets.Purchase", "workflowKey" : "2251799813690282"}'

Send Tickets.Purchase to start a workflow
> curl -X POST http://localhost:8080/workflow -H "Content-Type: application/json" -H "Ce-Id: 536808d33" -H "Ce-Type: Tickets.Purchase" -H "Ce-Source: curl" -d '{"sessionId":"5" }'
             
# Secrets and Secrets Managers

This project can be used with `External Secrets` to fetch credentials from external services from cloud provider specific Secret Managers.
In order to install `External Secrets` you can follow the project setup instructions: 

[https://github.com/godaddy/kubernetes-external-secrets](https://github.com/godaddy/kubernetes-external-secrets)

(touch)
