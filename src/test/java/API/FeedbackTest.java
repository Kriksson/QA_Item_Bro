package API;

import config.TestConfig;
import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class FeedbackTest extends TestBaseAPI {

    public static final String URL = TestConfig.API_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель добавляет и изменяет оценку и отзыв")
    public void successfulAddAndUpdateOrderFeedbackByBuyer() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель оставляет отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 5);
            body.put("feedbackText", "Отличная работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что заказ содержит оставленный отзыв", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("Отличная работа!", order.feedbackText());
            Assert.assertEquals(Integer.valueOf(5), order.rating());
        });

        Allure.step("Покупатель изменяет отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 1);
            body.put("feedbackText", "Плохая работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем заказ, он должен содержать оставленный отзыв", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("Плохая работа!", order.feedbackText());
            Assert.assertEquals(Integer.valueOf(1), order.rating());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Продавец оставляет ответ на отзыв")
    public void successfulAddSellerResponse() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель оставляет отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 5);
            body.put("feedbackText", "Отличная работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Продавец оставляет ответ на отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("responseText", "Спасибо за отзыв!");
            given()
                    .filter(sellerUtil.sessionFilter())
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/response")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем заказ, он должен содержать оставленный отзыв и ответ продавца", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("Отличная работа!", order.feedbackText());
            Assert.assertEquals(Integer.valueOf(5), order.rating());
            Assert.assertEquals("Спасибо за отзыв!", order.sellerResponseText());
        });
    }

    static Stream<Arguments> invalidRatings() {
        return Stream.of(
                Arguments.of(0, "must be greater than or equal to 1"),
                Arguments.of(6, "must be less than or equal to 5"),
                Arguments.of(null, "must not be null")
        );
    }

    @ParameterizedTest
    @Owner("kriksson")
    @MethodSource("invalidRatings")
    @DisplayName("Невалидная оценка отклоняется")
    public void unsuccessfulAddOrderFeedbackWithInvalidRating(Integer rating, String message) {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель оставляет отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 5);
            body.put("feedbackText", "Отличная работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Покупатель пытается оставить отзыв с невалидной оценкой", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", rating);
            body.put("feedbackText", "Плохая работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .body("message", equalTo(message))
                    .statusCode(400);
        });

        Allure.step("Проверяем что невалидная оценка не изменила отзыв", () -> {
            OrderDTO order = given()
                    .filter(majorSes.sessionFilter())
                    .when()
                    .get(URL+"/api/orders/"+createdOrder.id())
                    .then()
                    .log().all()
                    .statusCode(200)
                    .extract().body().as(OrderDTO.class);

            Assert.assertEquals("Отличная работа!", order.feedbackText());
            Assert.assertEquals(Integer.valueOf(5), order.rating());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Продавец не может написать отзыв")
    public void unsuccessfulAddOrderFeedbackBySeller() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Продавец не может оставить отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 5);
            body.put("feedbackText", "Отличная работа!");
            given()
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .filter(sellerUtil.sessionFilter())
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body("message", equalTo("У вас нет прав!"));
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель не может оставить ответ на отзыв")
    public void unsuccessfulAddSellerResponseByBuyer() {
        String sellerName = ApiUtility.genNick();
        String sellerPassword = ApiUtility.genPass();
        UtilityRegister sellerUtil =  ApiUtility.registerAndReturnData(URL, sellerName, sellerPassword);

        AuthorizedSession majorSes = ApiUtility.authMajor(URL);

        CategoryDTO createdCategory = ApiUtility.createTestCategory(URL, ApiUtility.genNick());

        AdviceDTO createdAdvice =  Allure.step("Продавец создаёт объявление", () -> {
            HashMap<String, Object> createBody = new HashMap<>();
            createBody.put("name", ApiUtility.genNick());
            createBody.put("game_id", createdCategory.id());
            createBody.put("price", BigDecimal.valueOf(199));
            return given()
                    .header(sellerUtil.csrf().headerName(), sellerUtil.csrf().token())
                    .filter(sellerUtil.sessionFilter())
                    .auth().preemptive().basic(sellerName, sellerPassword)
                    .contentType(ContentType.JSON)
                    .body(createBody)
                    .when()
                    .post(URL+"/api/advices")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(AdviceDTO.class);
        });

        OrderDTO createdOrder = Allure.step("Покупатель покупает товар", () -> {
            Map<String, Long> body =  new HashMap<>();
            body.put("id", createdAdvice.id());
            return given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .body(body)
                    .contentType(ContentType.JSON)
                    .when()
                    .post(URL+"/api/orders/")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .extract().body().as(OrderDTO.class);
        });

        Allure.step("Покупатель оставляет отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 5);
            body.put("feedbackText", "Отличная работа!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/feedback")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Покупатель не может оставить ответ на отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("responseText", "Спасибо за отзыв!");
            given()
                    .filter(majorSes.sessionFilter())
                    .header(majorSes.csrf().headerName(), majorSes.csrf().token())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .patch(URL+"/api/orders/"+createdOrder.id()+"/response")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body("message", equalTo("У вас нет прав!"));
        });
    }

}
