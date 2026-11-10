package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

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
}
