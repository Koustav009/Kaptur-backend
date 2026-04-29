package com.koustav.kaptur.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TusdHookRequest {

    // Hook type: "pre-create", "post-finish", "post-terminate" etc.
    private String Type;

    private Event Event;

    // --- Getters & Setters ---
    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public Event getEvent() {
        return Event;
    }

    public void setEvent(Event event) {
        Event = event;
    }

    // ---- Nested classes ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private Upload Upload;
        private HttpRequest HTTPRequest;

        public Upload getUpload() {
            return Upload;
        }

        public void setUpload(Upload upload) {
            Upload = upload;
        }

        public HttpRequest getHTTPRequest() {
            return HTTPRequest;
        }

        public void setHTTPRequest(HttpRequest httpRequest) {
            HTTPRequest = httpRequest;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Upload {
        private String ID;
        private Long Size;
        private Long Offset;
        private Map<String, String> MetaData; // filename, filetype, etc.
        private Map<String, String> Storage; // bucket, key (after upload is done)

        public String getID() {
            return ID;
        }

        public void setID(String id) {
            ID = id;
        }

        public Long getSize() {
            return Size;
        }

        public void setSize(Long size) {
            Size = size;
        }

        public Long getOffset() {
            return Offset;
        }

        public void setOffset(Long offset) {
            Offset = offset;
        }

        public Map<String, String> getMetaData() {
            return MetaData;
        }

        public void setMetaData(Map<String, String> metaData) {
            MetaData = metaData;
        }

        public Map<String, String> getStorage() {
            return Storage;
        }

        public void setStorage(Map<String, String> storage) {
            Storage = storage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpRequest {
        private String Method;
        private String URI;
        private String RemoteAddr;
        // Headers come as Map<String, List<String>> because headers can be multi-valued
        private Map<String, List<String>> Header;

        public String getMethod() {
            return Method;
        }

        public void setMethod(String method) {
            Method = method;
        }

        public String getURI() {
            return URI;
        }

        public void setURI(String uri) {
            URI = uri;
        }

        public Map<String, List<String>> getHeader() {
            return Header;
        }

        public void setHeader(Map<String, List<String>> header) {
            Header = header;
        }
    }
}