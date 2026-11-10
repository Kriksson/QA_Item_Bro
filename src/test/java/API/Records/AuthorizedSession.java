package API.Records;

import io.restassured.filter.session.SessionFilter;

public record AuthorizedSession(SessionFilter sessionFilter, CsrfResponse csrf) {
}
