package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;


import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class SecurityApiTest extends TestBaseAPI {

    public static final String URL = "http://localhost:8085";

    @Test
    @Owner("Kriksson")
    @DisplayName("Успешная регистрация аккаунта")
    public void successfulRegisterTest() {
        String name = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister utilityRegister = ApiUtility.registerAndReturnData(URL, name, password);

        Allure.step("Сравниваем значения с ожидаемыми", () -> {
            Assert.assertEquals(utilityRegister.credentials().name(), utilityRegister.profile().name());
            Assert.assertEquals("USER", utilityRegister.profile().userRole());
            Assert.assertEquals("0", utilityRegister.profile().balance().toString());
            Assert.assertNull(utilityRegister.profile().avatarUrl());
            Assert.assertNull(utilityRegister.profile().age());
            Assert.assertNull(utilityRegister.profile().city());
            Assert.assertNull(utilityRegister.profile().about());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Регистрируем пользователя и пытаемся войти под его данными")
    public void registerAndAuthTest() {
        String name = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister utilityRegister = ApiUtility.registerAndReturnData(URL, name, password);

        RegisterAccountResponse response = Allure.step("Авторизуемся и запрашиваем статистику своего аккаунта", () -> given()
                .filter(utilityRegister.sessionFilter())
                .auth().preemptive().basic(utilityRegister.credentials().name(), utilityRegister.credentials().password())
                .when()
                .get(URL+"/api/user/me")
                .then()
                .log().all()
                .statusCode(200)
                .extract().body().as(RegisterAccountResponse.class));

        Allure.step("Сравниваем значения", () -> {
            Assert.assertEquals(utilityRegister.credentials().name(), response.name());
            Assert.assertEquals(utilityRegister.profile().userRole(), response.userRole());
            Assert.assertEquals(utilityRegister.profile().balance().toString(), response.balance().toString());
            Assert.assertEquals(utilityRegister.profile().avatarUrl(), response.avatarUrl());
            Assert.assertEquals(utilityRegister.profile().age(), response.age());
            Assert.assertEquals(utilityRegister.profile().city(), response.city());
            Assert.assertEquals(utilityRegister.profile().about(), response.about());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Попытка зарегистрировать аккаунт с уже занятым ником должна вернуть 409")
    public void unSuccessfulRegisterWithAlreadyExistNickname() {
        String name = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        Allure.step("Регистрируем аккаунт с исходными данными", () -> {
            ApiUtility.registerAndReturnData(URL, name, password);
        });

        ApiError response = Allure.step("Создаём новую сессию и пробуем создать аккаунт с уже занятым ником", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
             return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(409)
                    .extract().body().as(ApiError.class);
        });
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("Имя занято!", response.message()));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @NullAndEmptySource
    @ValueSource(strings = {
            "     "
    })
    @DisplayName("Попытка зарегистрировать аккаунт с пустым или пробельным ником")
    public void unSuccessfulRegisterAccountWithEmptyOrSpaceName(String name) {
        String password =  ApiUtility.genPass();
        Allure.step("Пробуем создать ник с невалидным аргументом", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().body().as(ApiError.class);
        });
    }

    @ParameterizedTest
    @Owner("kriksson")
    @ValueSource(strings = {
            "aaaaaaaaaaaaaaaaaaaaa",
            "3284839fw8g923dj29t29hf82h2"
    })
    @DisplayName("Попытка зарегистрировать аккаунт с слишком длинным ником")
    public void unSuccessfulRegisterAccountWithInvalidSizeName(String name) {
        String password =  ApiUtility.genPass();
        ApiError response = Allure.step("Пробуем создать ник с невалидным аргументом", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().body().as(ApiError.class);
        });
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("size must be between 5 and 20", response.message()));
    }

    static Stream<String> validNames() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return Stream.of(
                uuid.substring(0, 5),
                uuid.substring(0, 20)
        );
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("validNames")
    @DisplayName("Регистрация ников в рамках границы")
    public void successfulRegisterWithValidNickname(String name) {
        String password =  ApiUtility.genPass();
        RegisterAccountResponse response = Allure.step("Пробуем создать ник с валидной границей аргументом", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
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
        Allure.step("Проверяем успех регистрации", () -> Assert.assertEquals(name, response.name()));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("invalidPasswords")
    @DisplayName("Попытка зарегистрировать аккаунт с слишком длинным и коротким паролем")
    public void unSuccessfulRegisterAccountWithInvalidPassword(String password) {
        String name =  ApiUtility.genNick();
        ApiError response = Allure.step("Пробуем создать аккаунт с невалидным паролем", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().body().as(ApiError.class);
        });
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("size must be between 8 and 64", response.message()));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @NullSource
    @DisplayName("Попытка зарегистрировать аккаунт с null паролем")
    public void unSuccessfulRegisterAccountWithInvalidSizePassword(String password) {
        String name =  ApiUtility.genNick();
        ApiError response = Allure.step("Пробуем создать аккаунт с пустым паролем", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().body().as(ApiError.class);
        });
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("must not be blank", response.message()));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @EmptySource
    @DisplayName("Попытка зарегистрировать аккаунт с пустым паролем")
    public void unSuccessfulRegisterAccountWithEmptyPassword(String password) {
        String name =  ApiUtility.genNick();
        Allure.step("Пробуем создать аккаунт с пустым паролем", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
            return given()
                    .filter(filter)
                    .body(new RegisterAccountBody(name, password))
                    .contentType(ContentType.JSON)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .post(URL+"/api/user")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .extract().body().as(ApiError.class);
        });
    }

    static Stream<String> validPasswords() {
        return Stream.of(
                "a".repeat(8),
                "a".repeat(20),
                "a".repeat(64)
        );
    }

    static Stream<String> invalidPasswords() {
        return Stream.of(
                "a".repeat(7),
                "a".repeat(65)
        );
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("validPasswords")
    @DisplayName("Регистрация аккаунтов с паролем в рамках границы")
    public void successfulRegisterWithValidPassword(String password) {
        String name =  ApiUtility.genNick();
        RegisterAccountResponse response = Allure.step("Пробуем создать аккаунт с валидной границей паролей", () -> {
            SessionFilter filter = new SessionFilter();
            CsrfResponse csrf = given()
                    .filter(filter)
                    .when()
                    .get(URL+"/csrf")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(CsrfResponse.class);
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
        Allure.step("Проверяем успех регистрации", () -> Assert.assertEquals(name, response.name()));
    }

    @Test
    @Owner("Kriksson")
    @DisplayName("Авторизованный пользователь не может зарегистрировать другого пользователя в текущей сессии")
    public void unsuccessfulRegisterFromAuthorizedUser() {
        String name =  ApiUtility.genNick();
        String newName =  ApiUtility.genNick();
        String password =  ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, password);

        Allure.step("Авторизуемся под созданным аккаунтом", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(name, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(name));
        });

        Allure.step("Проверяем что авторизация сохранилась в сессии", () -> {
            given()
                    .filter(util.sessionFilter())
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .statusCode(200)
                    .body("name", equalTo(name));
        });

        Allure.step("Пробуем в той же сессии создать новый аккаунт", () -> given()
                .filter(util.sessionFilter())
                .body(new RegisterAccountBody(newName, password))
                .header(util.csrf().headerName(), util.csrf().token())
                .contentType(ContentType.JSON)
                .when()
                .post(URL+"/api/user")
                .then()
                .log().all()
                .statusCode(403));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Вход с неправильным паролем")
    public void loginWithInvalidPassword() {
        String name =  ApiUtility.genNick();
        String correctPassword =  ApiUtility.genPass();
        String unCorrectPassword =  ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, correctPassword);

        Allure.step("Авторизуемся в созданный аккаунт под неправильным паролем", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(name, unCorrectPassword)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(401);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Вход с несуществующим именем")
    public void loginWithValidNickname() {
        String name =  ApiUtility.genNick();
        String password =  ApiUtility.genPass();
        Allure.step("Авторизуемся в созданный аккаунт под несуществующим ником", () -> {
            given()
                    .auth().preemptive().basic(name, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(401);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Доступ к /api/user/me без авторизации запрещён")
    public void unsuccessfulRequestUserMe() {
        Allure.step("Делаем запрос на /api/user/me без авторизации", () -> {
            given().
                    when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(401);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Изменяющие запросы без CSRF-токена и с неверным токеном отклоняются")
    public void sendPostWithoutCSRFTest() {
        Allure.step("Отправляем запрос на регистрацию без получения CSRF", () -> {
            given()
                    .when()
                    .post(URL+"/api/user/")
                    .then()
                    .log().all()
                    .statusCode(403);
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Выход завершает сессию")
    public void logoutKilledSessionTest() {
        String name =   ApiUtility.genNick();
        String password =  ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, password);

        Allure.step("Авторизовываемся под зарегистрированным ником и делаем GET запрос", () -> {
            given().
                auth().preemptive().basic(name, password)
                    .filter(util.sessionFilter())
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(name));
        });

        Allure.step("Выходим из аккаунта", () -> {
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(), util.csrf().token())
                    .when()
                    .post(URL+"/api/logout")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Пробуем запросить данные об аккаунте по той же сессии", () -> {
            given()
                    .filter(util.sessionFilter())
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(401);
        });
    }
}
