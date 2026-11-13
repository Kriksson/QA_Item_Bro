package API;

import config.TestConfig;
import API.Records.*;
import java.util.HashMap;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class AdviceTest extends TestBaseAPI{

    public static final String URL = TestConfig.API_URL;

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
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", adviceName);
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.valueOf(1000));
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
            Assert.assertEquals(0, createdAdvice.price().compareTo(BigDecimal.valueOf(1000L)));
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
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.valueOf(1000));
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

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.valueOf(1000));

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

        HashMap<String, Object> body = new HashMap<>();
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.valueOf(1000));

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

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.valueOf(1000));
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
                BigDecimal.valueOf(-1),
                BigDecimal.ZERO,
                new BigDecimal("0.99"),
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

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", testCategory.id());
        body.put("price", price);

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

        HashMap<String, Object> body = new HashMap<>();
        body.put("name", ApiUtility.genNick());
        body.put("game_id", testCategory.id());

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
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("game_id", testCategory.id());
        body.put("price", BigDecimal.ONE);
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
            Assert.assertEquals(0, createdAdvice.price().compareTo(BigDecimal.ONE));
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
            Assert.assertEquals(0, advices.getFirst().price().compareTo(BigDecimal.ONE));
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
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", "Test");
        body.put("game_id", 100000L);
        body.put("price", BigDecimal.ONE);
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

    @Test
    @Owner("kriksson")
    @DisplayName("Шестое активное объявление отклоняется")
    public void rateAdvicesTest() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);

        Allure.step("Авторизуемся в аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200);
        });

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        for (int i = 0; i < 5; i++) {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", ApiUtility.genNick());
            body.put("game_id", createdCategory.id());
            body.put("price", BigDecimal.ONE);
            Allure.step("Создаём объявление №"+(i+1), () -> {
                given()
                        .filter(util.sessionFilter())
                        .header(util.csrf().headerName(), util.csrf().token())
                        .contentType(ContentType.JSON)
                        .body(body)
                        .when()
                        .post(URL+"/api/advices")
                        .then()
                        .log().all()
                        .statusCode(201)
                        .body("name", equalTo(body.get("name")));
            });
        }

        Allure.step("Пытаемся создать 6 объявление", () -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", ApiUtility.genNick());
            body.put("game_id", createdCategory.id());
            body.put("price", BigDecimal.ONE);
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(), util.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(409)
                    .body("message", equalTo("Limit advice"));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Публичный список содержит только активные объявления выбранной категории")
    public void checkPublicListAdvicesTest() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200);
        });

        Allure.step("Создаём первое объявление", () -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", ApiUtility.genNick());
            body.put("game_id", createdCategory.id());
            body.put("price", BigDecimal.ONE);
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(),  util.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201);
        });

        AdviceDTO dAdvice = Allure.step("Создаём второе объявление", () -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", ApiUtility.genNick());
            body.put("game_id", createdCategory.id());
            body.put("price", BigDecimal.ONE);
            return given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(),  util.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        List<AdviceDTO> adviceDTOList = Allure.step("Получаем объявления выбранной категории", () -> given()
                .filter(util.sessionFilter())
                .queryParam("gameId", createdCategory.id())
                .when()
                .get(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath().getList("content", AdviceDTO.class));

        Allure.step("Проверяем что в списке есть отслеживаемое объявление", () -> Assert.assertTrue(adviceDTOList.stream().anyMatch(x -> x.name().equals(dAdvice.name()))));

        Allure.step("Архивируем объявление", () -> {
            given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(),  util.csrf().token())
                    .when()
                    .delete(URL+"/api/advices/" + dAdvice.id())
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        List<AdviceDTO> newAdviceDTOList = Allure.step("Повторно получаем объявления после архивирования", () -> given()
                .filter(util.sessionFilter())
                .queryParam("gameId", createdCategory.id())
                .when()
                .get(URL+"/api/advices")
                .then()
                .log().all()
                .statusCode(200)
                .extract().jsonPath().getList("content", AdviceDTO.class));

        Allure.step("Проверяем что отслеживаемое объявление пропало из списка", () -> Assert.assertFalse(newAdviceDTOList.stream().anyMatch(x -> x.name().equals(dAdvice.name()))));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Админ может архивировать чужое объявление")
    public void adminDeleteAnyoneAdviceTest() {
        String nick = ApiUtility.genNick();
        String password = ApiUtility.genPass();
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, nick, password);
        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        Allure.step("Авторизуемся в аккаунт", () -> {
            given()
                    .filter(util.sessionFilter())
                    .auth().preemptive().basic(nick, password)
                    .when()
                    .get(URL+"/api/user/me")
                    .then()
                    .log().all()
                    .statusCode(200);
        });

        AdviceDTO advice = Allure.step("Создаём объявление", () -> {
            HashMap<String, Object> body = new HashMap<>();
            body.put("name", ApiUtility.genNick());
            body.put("game_id", createdCategory.id());
            body.put("price", BigDecimal.ONE);
            return given()
                    .filter(util.sessionFilter())
                    .header(util.csrf().headerName(),  util.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        AuthorizedSession adminSes = ApiUtility.authAdmin(URL);
        CsrfResponse csrf = adminSes.csrf();
        SessionFilter filter = adminSes.sessionFilter();

        Allure.step("Администратор архивирует чужое объявление", () -> {
            given()
                    .filter(filter)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .delete(URL+"/api/advices/" + advice.id())
                    .then()
                    .log().all()
                    .statusCode(204);
        });
    }
}
