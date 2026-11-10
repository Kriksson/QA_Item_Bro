package API.Records;

import io.restassured.filter.session.SessionFilter;

public record UtilityRegister(CsrfResponse csrf, SessionFilter sessionFilter,
                              RegisterAccountResponse profile, RegisterAccountBody credentials) {
}
