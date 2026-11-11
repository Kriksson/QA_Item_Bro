package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class FeedbackTest extends TestBaseAPI {

    public static final String URL = "http://localhost:8085";

    @Test
    @Owner("kriksson")
    @DisplayName("Покупатель оставляет и изменяет отзыв. Отзыв меньше 1, больше 5 и mull отклоняется")
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

        Allure.step("Проверяем заказ, он должен содержать оставленный отзыв", () -> {
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

        Allure.step("Покупатель не может поставить оценку меньше 1 на отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 0);
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
                    .body("message", equalTo("must be greater than or equal to 1"))
                    .statusCode(400);
        });

        Allure.step("Покупатель не может поставить оценку больше 5 на отзыв", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", 6);
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
                    .body("message", equalTo("must be less than or equal to 5"))
                    .statusCode(400);
        });

        Allure.step("Покупатель не может поставить отзыв без оценки", () -> {
            Map<String, Object> body =  new HashMap<>();
            body.put("rating", null);
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
                    .body("message", equalTo("must not be null"))
                    .statusCode(400);
        });
    }
}
