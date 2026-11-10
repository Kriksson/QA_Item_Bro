package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;


import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ApiUtility {

    public static UtilityRegister registerAndReturnData(String URL, String name, String password) {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = Allure.step("Получаем csrf токен", () -> given()
                .filter(filter)
                .when()
                .get(URL+"/csrf")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));
        RegisterAccountResponse response = Allure.step("Регистрируем пользователя", () -> given()
                .filter(filter)
                .body(new RegisterAccountBody(name, password))
                .contentType(ContentType.JSON)
                .header(csrf.headerName(), csrf.token())
                .when()
                .post(URL+"/api/user")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(RegisterAccountResponse.class));

        return new UtilityRegister(csrf, filter,response, new RegisterAccountBody(name, password));
    }

    public static CategoryDTO createTestCategory(String URL, String categoryName) {
        AuthorizedSession session = authAdmin(URL);
        CsrfResponse csrf = session.csrf();
        SessionFilter filter = session.sessionFilter();

        return Allure.step("Создание тестовой категории", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody(categoryName))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(CategoryDTO.class));
    }

    public static AuthorizedSession authAdmin(String URL) {
        SessionFilter filter = new SessionFilter();

        Allure.step("Администратор авторизуется", () -> {
            given()
                    .filter(filter)
                    .auth().preemptive().basic("ADMIN", "Admin123!")
            .when()
                    .get(URL + "/api/user/me")
            .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo("ADMIN"))
                    .body("userRole", equalTo("ADMIN"));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF для сессии администратора", () -> given()
                .filter(filter)
        .when()
                .get(URL + "/csrf")
        .then()
                .log().all()
                .statusCode(200)
                .extract().as(CsrfResponse.class));

        return new AuthorizedSession(filter, csrf);
    }

    public static String genNick() {
        return "test"+ UUID.randomUUID().toString().replace("-","").substring(0, 12);
    }

    public static String genPass() {
        return UUID.randomUUID().toString().replace("-","").substring(0, 19);
    }
}
