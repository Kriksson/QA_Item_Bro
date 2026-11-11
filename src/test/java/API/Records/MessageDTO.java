package API.Records;

import java.time.Instant;

public record MessageDTO(Long id, Long orderId, String messageType, String eventType, UserDTO sender, String text, Instant sentAt) {
}
