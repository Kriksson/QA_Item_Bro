package API.Records;

import java.math.BigDecimal;

public record RegisterAccountResponse(String name, String userRole, BigDecimal balance, String avatarUrl,
                                      Integer age, String city, String about) {
}
