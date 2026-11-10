package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class AdviceTest extends TestBaseAPI{

    public static final String URL = "http://localhost:8085";

    @Test
    @Owner("kriksson")
    @DisplayName("Авторизованный пользователь создаёт объявление")
    public void successfulCreateAdvice() {
        String nick = ApiUtility.genNick();
        String password =  ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);

        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
            .when()
                    .get(URL+"/api/user/me")
            .then()
                    .log().all()
                    .statusCode(200);
        });
        String adviceName = ApiUtility.genNick();
        CreateAdviceBody body = new CreateAdviceBody(adviceName, testCategory.id(), 1000L);
        AdviceDTO createdAdvice = Allure.step("Создаём объявление в категории", () -> given()
                .filter(util.sessionFilter())
                .header(util.csrf().headerName(), util.csrf().token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        Allure.step("Проверяем созданное объявление", () -> {
            Assert.assertEquals(nick, createdAdvice.userSeller().username());
            Assert.assertEquals(adviceName, createdAdvice.name());
            Assert.assertEquals(Long.valueOf(1000), createdAdvice.price());
            Assert.assertEquals(testCategory.id(), createdAdvice.game().id());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Гость не может создать объявления")
    public void unsuccessfulCreateAdvice() {
        CsrfResponse csrf = given()
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class);


        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());
        CreateAdviceBody body = new CreateAdviceBody(ApiUtility.genNick(), testCategory.id(), 1000L);
        Allure.step("Неудачная попытка создать объявление", () -> given()
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(403));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @NullAndEmptySource
    @ValueSource(strings = {
            "     ",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("Невалидное название объявления отклоняется")
    public void unsuccessfulCreateAdviceWithInvalidName(String name) {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        CreateAdviceBody body = new CreateAdviceBody(name, testCategory.id(), 1000L);

        Allure.step("Пытаемся создать объявление с невалидным названием", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(400));

        Allure.step("Проверяем что объявление не создалось", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertTrue(advices.isEmpty());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Отсутствующее поле name при создании объявления отклоняется")
    public void unsuccessfulCreateAdviceWithoutName() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        CreateAdviceWithoutNameBody body = new CreateAdviceWithoutNameBody(testCategory.id(), 1000L);

        Allure.step("Отправляем объявление без поля name", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(400));

        Allure.step("Проверяем что объявление не создалось", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertTrue(advices.isEmpty());
        });
    }

    @ParameterizedTest
    @Owner("kriksson")
    @ValueSource(strings = {
            "a",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("Названия объявлений длиной 1 и 50 символов принимаются")
    public void successfulCreateAdviceWithValidName(String name) {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        CreateAdviceBody body = new CreateAdviceBody(name, testCategory.id(), 1000L);
        AdviceDTO createdAdvice = Allure.step("Создаём объявление с названием допустимой длины", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        Allure.step("Проверяем название и категорию созданного объявления", () -> {
            Assert.assertEquals(name, createdAdvice.name());
            Assert.assertEquals(testCategory.id(), createdAdvice.game().id());
        });

        Allure.step("Проверяем сохранённое название в списке объявлений", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertEquals(1, advices.size());
            Assert.assertEquals(name, advices.getFirst().name());
        });
    }

    static Stream<Object> invalidAdvicePrices() {
        return Stream.of(
                null,
                -1L,
                0L,
                0.99,
                "not-a-number"
        );
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("invalidAdvicePrices")
    @DisplayName("Невалидная цена объявления отклоняется")
    public void unsuccessfulCreateAdviceWithInvalidPrice(Object price) {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        CreateAdviceWithInvalidPriceBody body = new CreateAdviceWithInvalidPriceBody(ApiUtility.genNick(), testCategory.id(), price);

        Allure.step("Пытаемся создать объявление с невалидной ценой", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(400));

        Allure.step("Проверяем что объявление не создалось", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertTrue(advices.isEmpty());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Отсутствующее поле price при создании объявления отклоняется")
    public void unsuccessfulCreateAdviceWithoutPrice() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        CreateAdviceWithoutPriceBody body = new CreateAdviceWithoutPriceBody(ApiUtility.genNick(), testCategory.id());

        Allure.step("Отправляем объявление без поля price", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(400));

        Allure.step("Проверяем что объявление не создалось", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertTrue(advices.isEmpty());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Объявление с минимальной ценой 1 создаётся")
    public void successfulCreateAdviceWithMinimumPrice() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO testCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в созданный аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("name", equalTo(nick));
        });

        CsrfResponse csrf = Allure.step("Получаем CSRF после авторизации", () -> given()
                .filter(util.sessionFilter())
                .when()
                .get(URL+"/csrf")
                .then()
                .statusCode(200)
                .extract().body().as(CsrfResponse.class));

        String name = ApiUtility.genNick();
        CreateAdviceBody body = new CreateAdviceBody(name, testCategory.id(), 1L);
        AdviceDTO createdAdvice = Allure.step("Создаём объявление с ценой 1", () -> given()
                .filter(util.sessionFilter())
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(AdviceDTO.class));

        Allure.step("Проверяем минимальную цену созданного объявления", () -> {
            Assert.assertEquals(name, createdAdvice.name());
            Assert.assertEquals(Long.valueOf(1), createdAdvice.price());
            Assert.assertEquals(testCategory.id(), createdAdvice.game().id());
        });

        Allure.step("Проверяем сохранённую цену в списке объявлений", () -> {
            List<AdviceDTO> advices = given()
                    .queryParam("gameId", testCategory.id())
                    .when()
                    .get(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().jsonPath().getList("content", AdviceDTO.class);

            Assert.assertEquals(1, advices.size());
            Assert.assertEquals(Long.valueOf(1), advices.getFirst().price());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Отсутствующая, несуществующая категория отклоняются")
    public void unsuccessfulCreateAdviceWithNonExistCategoryTest() {
        String name =  ApiUtility.genNick();
        String password =  ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, password);

        Allure.step("Авторизуемся под созданным аккаунтом", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(name, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200);
        });
        CreateAdviceBody body = new CreateAdviceBody("Test", 100000L, 1L);
        Allure.step("Создаём объявление в несуществующей категории", () -> {
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(), util.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(404);
        });
    }
}
