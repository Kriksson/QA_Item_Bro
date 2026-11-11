package API.Records;

import java.math.BigDecimal;

public record AdviceDTO(Long id, String name, GameDTO game, UserDTO userSeller, BigDecimal price, String imageUrl) {
}
