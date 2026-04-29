package com.koustav.kaptur.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

// Only serialize non-null fields — tusd ignores null fields anyway
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TusdHookResponse {

    // Set to true in pre-create to BLOCK the upload
    private Boolean RejectUpload;

    // Customize the HTTP response tusd sends back to the client
    private HttpResponse httpResponse;

    public Boolean getRejectUpload() {
        return RejectUpload;
    }

    public void setRejectUpload(Boolean rejectUpload) {
        RejectUpload = rejectUpload;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HttpResponse {
        private Integer StatusCode;
        private String Body;
        private Map<String, String> Header;

        public Integer getStatusCode() {
            return StatusCode;
        }

        public void setStatusCode(Integer statusCode) {
            StatusCode = statusCode;
        }

        public String getBody() {
            return Body;
        }

        public void setBody(String body) {
            Body = body;
        }

        public Map<String, String> getHeader() {
            return Header;
        }

        public void setHeader(Map<String, String> header) {
            Header = header;
        }
    }
}
