# Taste (Continuously Learned by [CommandCode][cmd])

[cmd]: https://commandcode.ai/

# Architecture
- All entity primary keys use UUID (not Long auto-increment). Use UuidCreator.getTimeOrderedEpoch() for UUIDv7 where B-tree index performance matters. Confidence: 0.70
- Event membership is binary: existence in EventMembersDtl = accepted member. No separate invitation/pending status field. Confidence: 0.65

# DTO
- Never expose JPA entities in API responses. Use safe DTO fields instead of embedding full entity objects (e.g., AuthResponse uses kptId/email/name/imageUrl, not the full User entity). Confidence: 0.65
