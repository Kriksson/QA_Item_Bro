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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class ApiTest {

    public static final String URL = "http://localhost:8085";


    @Test
    @Owner("Kriksson")
    @DisplayName("Успешная регистрация аккаунта")
    public void successfulRegisterTest() {
        String name = "test_"+ UUID.randomUUID().toString().replace("-","").substring(0, 12);
        String password = UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        String name = "test"+ UUID.randomUUID().toString().replace("-","").substring(0, 12);
        String password = UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        String name = "test"+ UUID.randomUUID().toString().replace("-","").substring(0, 12);
        String password = UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        String password =  UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("must not be blank", response.message()));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @ValueSource(strings = {
            "aaaaaaaaaaaaaaaaaaaaa",
            "3284839fw8g923dj29t29hf82h2"
    })
    @DisplayName("Попытка зарегистрировать аккаунт с слишком длинным ником")
    public void unSuccessfulRegisterAccountWithInvalidSizeName(String name) {
        String password =  UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        Allure.step("Проверяем ошибку регистрации", () -> Assert.assertEquals("size must be between 0 and 20", response.message()));
    }

    static Stream<String> validNames() {
        List<String> values = new ArrayList<>();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        for (int i = 1; i <= 20; i++) {
            values.add(uuid.substring(0, i));
        }

        return values.stream();
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("validNames")
    @DisplayName("Регистрация ников в рамках границы")
    public void successfulRegisterWithValidNickname(String name) {
        String password =  UUID.randomUUID().toString().replace("-","").substring(0, 19);
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
        Allure.step("Проверяем успех регистрации", () -> {
            Assert.assertEquals(name, response.name());
        });
    }
}
