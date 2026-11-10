package API.Records;

public record CsrfResponse(String headerName,
                           String parameterName,
                           String token) {
}
