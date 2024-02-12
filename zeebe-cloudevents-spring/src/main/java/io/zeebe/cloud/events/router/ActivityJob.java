package io.zeebe.cloud.events.router;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class ActivityJob {

	private Map<String, String> uris;
    private String format;

	public ActivityJob() {
	}

	public ActivityJob(Map<String, String> uris, String format) {
		this.uris = uris;
        this.format = format;
	}

	public Map<String, String> getUris() {
		return this.uris;
	}

	public void setUris(Map<String, String> uris) {
		this.uris = uris;
	}

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }


    @Override
    public String toString() {
        return "ActivityJob{" +
                "uris='" + uris + '\'' +
                ", format=" + format +
                '}';
    }

}