package API.Records;

public record OrderDTO(Long id, AdviceDTO adviceDTO, UserDTO userBuyerDTO, Long price,
                       Integer rating, String feedbackText, String sellerResponseText, String orderStatus) {
}
