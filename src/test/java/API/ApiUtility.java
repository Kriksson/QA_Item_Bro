package API;

import API.Records.CsrfResponse;
import API.Records.RegisterAccountBody;
import API.Records.RegisterAccountResponse;
import API.Records.UtilityRegister;
import io.qameta.allure.Allure;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;


import static io.restassured.RestAssured.given;

public class ApiUtility {

    public static UtilityRegister registerAndReturnData(String URL, String name, String password) {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = Allure.step("Получаем csrf токен", () -> {
            return given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
        });
        RegisterAccountResponse response = Allure.step("Регистрируем пользователя", () -> {
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(RegisterAccountResponse.class);
        });

        return new UtilityRegister(csrf, filter,response, new RegisterAccountBody(name, password));
    }
}
