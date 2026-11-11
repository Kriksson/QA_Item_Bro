package API.Records;

import io.restassured.filter.session.SessionFilter;

import java.util.HashMap;

public record UtilityRegister(CsrfResponse csrf, SessionFilter sessionFilter,
                              RegisterAccountResponse profile, HashMap<String, String> credentials) {
}
