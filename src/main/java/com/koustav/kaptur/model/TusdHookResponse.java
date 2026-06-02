package com.koustav.kaptur.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Response sent back to TUSd from hook endpoints.
 * 
 * Used in pre-create hooks to:
 * - Reject an upload (RejectUpload = true)
 * - Override the file ID so TUSd uses our application-generated UUID (ChangeFileInfo.ID)
 * - Customize the HTTP response TUSd sends to the client (httpResponse)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TusdHookResponse {

    // Set to true in pre-create to BLOCK the upload (e.g. invalid photoId)
    private Boolean RejectUpload;

    // Used in pre-create response to override the TUSd upload ID with our UUID photoId
    private ChangeFileInfo ChangeFileInfo;

    // Customize the HTTP response tusd sends back to the client
    private HttpResponse httpResponse;

    // --- Getters & Setters ---
    public Boolean getRejectUpload() {
        return RejectUpload;
    }

    public void setRejectUpload(Boolean rejectUpload) {
        RejectUpload = rejectUpload;
    }

    public ChangeFileInfo getChangeFileInfo() {
        return ChangeFileInfo;
    }

    public void setChangeFileInfo(ChangeFileInfo changeFileInfo) {
        ChangeFileInfo = changeFileInfo;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    // --- Nested classes ---

    /**
     * When returned in a pre-create hook response, tusd will use this ID
     * instead of its own generated upload ID.
     * This lets us control the upload URL path so it matches our photoId.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChangeFileInfo {
        private String ID;

        public String getID() {
            return ID;
        }

        public void setID(String id) {
            ID = id;
        }
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