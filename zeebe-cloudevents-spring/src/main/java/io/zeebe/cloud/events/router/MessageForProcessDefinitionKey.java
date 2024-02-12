package io.zeebe.cloud.events.router;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageForProcessDefinitionKey {
    private String processDefinitionKey;
    private String messageName;

    public MessageForProcessDefinitionKey() {
    }

    public MessageForProcessDefinitionKey(String processDefinitionKey, String messageName) {
        this.processDefinitionKey = processDefinitionKey;
        this.messageName = messageName;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }
}
