package API.Records;

public record AdviceDTO(Long id, String name, GameDTO game, UserDTO userSeller, Long price, String imageUrl) {
}
