package API.Records;

import java.math.BigDecimal;

public record OrderDTO(Long id, AdviceDTO adviceDTO, UserDTO userBuyerDTO, BigDecimal price,
                       Integer rating, String feedbackText, String sellerResponseText, String orderStatus) {
}
