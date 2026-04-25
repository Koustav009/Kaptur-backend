public class EventMembersIds {
    // Must implement Serializable
    // Must override equals() and hashCode()
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class EventMembersId implements Serializable {

        private String event; // ← must match field NAME in entity (not column name)
        private String user; // ← must match field NAME in entity
    }
}
