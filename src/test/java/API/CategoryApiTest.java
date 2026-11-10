package API;

import API.Records.*;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryApiTest extends TestBaseAPI{

    public static final String URL = "http://localhost:8085";

    @Test
    @Owner("kriksson")
    @Order(1)
    @DisplayName("При отсутствии категорий возвращается пустой список")
    public void getEmptyCategoriesTest() {
        List<CategoryDTO> categories = Allure.step("Запрашиваем список категорий без авторизации", () -> given()
                .when()
                .get(URL+"/api/games")
                .then()
                .statusCode(200)
                .log().all()
                .extract().jsonPath().getList(".", CategoryDTO.class));

        Allure.step("Проверяем что список пуст", () -> Assert.assertTrue(categories.isEmpty()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор создаёт категорию")
    public void createCategoryTest() {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = given()
                .filter(filter).when().get(URL+"/csrf").then().extract().body().as(CsrfResponse.class);
        Allure.step("Администратор авторизуется", () -> given()
                .auth().preemptive().basic("ADMIN", "Admin123!")
                .filter(filter)
                .get(URL+"/api/user/me")
                .then()
                .log().all())
                .body("userRole", equalTo("ADMIN"))
                .statusCode(200);

        CategoryDTO category = Allure.step("Администратор создаёт категорию", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody("ChatGPT"))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(CategoryDTO.class)
        );

        Allure.step("Проверяем что название созданной категории как в запросе", () ->
                Assert.assertEquals("ChatGPT", category.name()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Обычный пользователь пытается создать категорию")
    public void unsuccessfulCreateCategoryTest() {
        String name = "test"+ UUID.randomUUID().toString().replace("-","").substring(0, 12);
        String password = UUID.randomUUID().toString().replace("-","").substring(0, 19);
        UtilityRegister util = ApiUtility.registerAndReturnData(URL, name, password);

        ApiError api = Allure.step("Пользователь пытается создать категорию", () -> given()
                .filter(util.sessionFilter())
                .auth().preemptive().basic(name, password)
                .header(util.csrf().headerName(), util.csrf().token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody("Claude Code"))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(403)
                .extract().body().as(ApiError.class)
        );

        Allure.step("Проверяем что ошибка с связана с отсутствием прав", () -> Assert.assertEquals("no have permissions to create game", api.message()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Возвращается список категорий")
    public void getCategoriesTest() {
        List<CategoryDTO> categories = Allure.step("Запрашиваем список категорий без авторизации", () -> given()
                .when()
                .get(URL+"/api/games")
                .then()
                .statusCode(200)
                .log().all()
                .extract().jsonPath().getList(".", CategoryDTO.class));

        Allure.step("Проверяем что в списке есть ожидаемая категория", () ->
                Assert.assertTrue(categories.stream()
                        .anyMatch(category -> category.name().equals("ChatGPT"))));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @NullAndEmptySource
    @ValueSource(strings = {
            "     ",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("Пустое название, пробелы, null или слишком длинное название при создании категории")
    public void unsuccessfulCreateCategoryWithNullNameTest(String name) {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = given()
                .filter(filter).when().get(URL+"/csrf").then().extract().body().as(CsrfResponse.class);
        Allure.step("Администратор авторизуется", () -> given()
                        .auth().preemptive().basic("ADMIN", "Admin123!")
                        .filter(filter)
                        .get(URL+"/api/user/me")
                        .then()
                        .log().all())
                .body("userRole", equalTo("ADMIN"))
                .statusCode(200);

        Allure.step("Администратор не может создать категорию с пустым названием", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody(name))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(400));
    }

    @ParameterizedTest
    @Owner("kriksson")
    @ValueSource(strings = {
            "a",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("Названия длиной 1 и 50 символов принимаются")
    public void successfulCreateCategoryTest(String name) {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = given()
                .filter(filter).when().get(URL+"/csrf").then().extract().body().as(CsrfResponse.class);
        Allure.step("Администратор авторизуется", () -> given()
                        .auth().preemptive().basic("ADMIN", "Admin123!")
                        .filter(filter)
                        .get(URL+"/api/user/me")
                        .then()
                        .log().all())
                .body("userRole", equalTo("ADMIN"))
                .statusCode(200);

        Allure.step("Администратор создаёт категорию", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody(name))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(201))
                .body("name", equalTo(name));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Создание категории с занятым названием отклоняется")
    public void unsuccessfulCreateDuplicateCategoryTest() {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = given()
                .filter(filter).when().get(URL+"/csrf").then().extract().body().as(CsrfResponse.class);
        Allure.step("Администратор авторизуется", () -> given()
                        .auth().preemptive().basic("ADMIN", "Admin123!")
                        .filter(filter)
                        .get(URL+"/api/user/me")
                        .then()
                        .log().all())
                .body("userRole", equalTo("ADMIN"))
                .statusCode(200);

        Allure.step("Администратор создаёт категорию №1", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody("ChatGPT"))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(201)
                .body("name", equalTo("ChatGPT"))
        );

        ApiError api = Allure.step("Администратор создаёт категорию №2", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody("ChatGPT"))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(409)
                .extract().body().as(ApiError.class)
        );

        Allure.step("Проверяем что ошибка создания связана с тем что категория уже создана", () ->
                Assert.assertEquals("game already created", api.message()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор архивирует категорию и она пропадает из общего списка")
    public void successfulDeleteCategoryTest() {
        SessionFilter filter = new SessionFilter();
        CsrfResponse csrf = given()
                .filter(filter).when().get(URL+"/csrf").then().extract().body().as(CsrfResponse.class);
        Allure.step("Администратор авторизуется", () -> given()
                        .auth().preemptive().basic("ADMIN", "Admin123!")
                        .filter(filter)
                        .get(URL+"/api/user/me")
                        .then()
                        .log().all())
                .body("userRole", equalTo("ADMIN"))
                .statusCode(200);

        CategoryDTO createdCategory = Allure.step("Администратор создаёт категорию", () -> given()
                .filter(filter)
                .header(csrf.headerName(), csrf.token())
                .contentType(ContentType.JSON)
                .body(new CategoryBody("Grand Theft Auto VI"))
                .when()
                .post(URL+"/api/games")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().as(CategoryDTO.class)
        );

        Allure.step("Проверяем что категория появилась в списках", () -> {
            List<CategoryDTO> categories = given()
                    .when()
                    .get(URL+"/api/games")
                    .then()
                    .log().all()
                    .extract().jsonPath().getList(".", CategoryDTO.class);

            Assert.assertTrue(categories.stream().anyMatch(x -> x.name().equals("Grand Theft Auto VI")));
        });

        Allure.step("Администратор архивирует категорию", () -> {
            given()
                    .filter(filter)
                    .header(csrf.headerName(), csrf.token())
                    .when()
                    .patch(URL+"/api/games/"+createdCategory.id()+"/delete")
                    .then()
                    .log().all()
                    .statusCode(204);
        });

        Allure.step("Проверяем что категория больше не в списках", () -> {
            List<CategoryDTO> categories = given()
                    .when()
                    .get(URL+"/api/games")
                    .then()
                    .log().all()
                    .extract().jsonPath().getList(".", CategoryDTO.class);

            Assert.assertFalse(categories.stream().anyMatch(x -> x.name().equals("Grand Theft Auto VI")));
        });
    }
}
