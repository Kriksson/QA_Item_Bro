package UI;

import API.ApiUtility;
import UI.Pages.MainPage;
import com.codeborne.selenide.Selenide;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AuthTest extends TestBaseUI{

    public static final String URL = "http://localhost:5173/";

    @Test
    @Owner("kriksson")
    @DisplayName("Регистрация через форму выполняет вход и отображает созданный аккаунт")
    public void successfulRegisterThroughForm() {
        MainPage mainPage = new MainPage(URL);
        String nick = ApiUtility.genNick();
        mainPage.registerUser(nick, ApiUtility.genPass());
        Allure.step("Проверяем что после регистрации на сайте, ник такой же", () -> Assert.assertEquals(nick, mainPage.getUsername()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Регистрация с занятым именем показывает ошибку")
    public void unsuccessfulRegisterWithAlreadyExistNickname() {
        MainPage mainPage = new MainPage(URL);
        String nick = ApiUtility.genNick();
        mainPage.registerUser(nick, ApiUtility.genPass());
        mainPage.logOut();
        Allure.step("Пытаемся заново зарегистрироваться на тот же ник", () -> mainPage.registerUser(nick, ApiUtility.genPass()));

        Allure.step("Ожидаем ошибку \"Имя занято!\"", () -> Assert.assertEquals("Имя занято!", mainPage.getFormError()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Вход отображает данные аккаунта, F5 сохраняет сессию, смена пользователя обновляет данные")
    public void successfulLoginAndSwitchAccount() {
        String adminLogin = "ADMIN";
        String adminPassword = "Admin123!";
        String majorLogin = "MAJOR";
        String majorPassword = "Major123!";
        MainPage main = new MainPage(URL);
        main.authUser(adminLogin, adminPassword);
        Allure.step("Проверяем что авторизация выполнилась", () -> Assert.assertEquals(adminLogin, main.getUsername()));
        Allure.step("Перезагружаем страницу и ожидаем что Username тот же", () -> {
            Selenide.refresh();
            Assert.assertEquals(adminLogin, main.getUsername());
        });
        main.logOut();

        main.authUser(majorLogin, majorPassword);
        Allure.step("Проверяем что авторизация выполнилась", () -> Assert.assertEquals(majorLogin, main.getUsername()));
        Allure.step("Перезагружаем страницу и ожидаем что Username тот же", () -> {
            Selenide.refresh();
            Assert.assertEquals(majorLogin, main.getUsername());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Неправильный пароль показывает ошибку, пользователь остаётся гостем")
    public void unsuccessfulLoginWithInvalidPassword() {
        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("ADMIN", "admin");
        Allure.step("Ожидаем ошибку \"Имя занято!\"", () -> Assert.assertEquals("Неверное имя или пароль. Войди заново.", mainPage.getFormError()));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Управление доступно только ADMIN и скрывается после смены аккаунта на USER")
    public void checkAdminControlsVisibilityByRole() {
        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("ADMIN", "Admin123!");
        Allure.step("Проверяем что раздел управления доступен", () -> Assert.assertTrue(mainPage.isAdminFunctionVisible()));
        mainPage.logOut();
        mainPage.authUser("MAJOR", "Major123!");
        Allure.step("Проверяем что раздел управления недоступен", () -> Assert.assertFalse(mainPage.isAdminFunctionVisible()));
    }

}
