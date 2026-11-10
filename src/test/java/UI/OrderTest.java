package UI;

import UI.Pages.MainPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Owner("Kriksson")
@DisplayName("Тестирование заказов")
public class OrderTest extends TestBaseUI {

    public static final String URL = "http://localhost:5173/";

    public static final String ADMIN_LOGIN = "kriksson";
    public static final String ADMIN_PASSWORD = "kriksson";


    @Test
    @Owner("Kriksson")
    @DisplayName("Создать объявление. Проверить что оно создалась с теми параметрами что указали")
    public void createNewOrderAndCheckHimTest() {
        MainPage mainPage = Allure.step("Открываем сайт и авторизовываемся", () -> {
            MainPage p = new MainPage(URL);
            p.authorize(ADMIN_LOGIN, ADMIN_PASSWORD);
            return p;
        });
        Allure.step("Создаём объявление и ищем его среди своих", () -> {
            mainPage.createAdvice("ChatGPT", "Подписка ChatGPT", "100");
            mainPage.searchFromMyAdvices("Подписка ChatGPT");
        });

        Allure.step("Проверяем сохранённое объявление с ожиданиями", () -> {
            Assert.assertTrue(mainPage.advicesContainsName("Подписка ChatGPT"));
            Assert.assertEquals("100", mainPage.getPrice());
            Assert.assertEquals("Подписка ChatGPT", mainPage.getNameOfAdvice());
            Assert.assertEquals("ChatGPT", mainPage.getCategory());
        });
    }

    @Test
    @Owner("Kriksson")
    @DisplayName("Попытка создать объявление с отрицательной")
    public void createOrderWithNegativePriceTest() {
        MainPage mainPage = Allure.step("Открываем сайт и авторизовываемся", () -> {
            MainPage p = new MainPage(URL);
            p.authorize(ADMIN_LOGIN, ADMIN_PASSWORD);
            return p;
        });

        Allure.step("Пытаемся создать объявление", () -> {
            mainPage.createAdvice("Claude Code", "Подписка Claude Code", "-1");
            mainPage.isInputInvalid();
            mainPage.closeForm();
        });

        Allure.step("Проверяем что такое объявление не создалось", () -> {
            mainPage.openCategoryByName("Claude Code");
            Assert.assertFalse(mainPage.advicesContainsName("Подписка Claude Code"));
        });
    }

    @Test
    @Owner("Kriksson")
    @DisplayName("Попытка создать объявление с отрицательной")
    public void createOrderWithZeroPriceTest() {
        MainPage mainPage = Allure.step("Открываем сайт и авторизовываемся", () -> {
            MainPage p = new MainPage(URL);
            p.authorize(ADMIN_LOGIN, ADMIN_PASSWORD);
            return p;
        });

        Allure.step("Пытаемся создать объявление", () -> {
            mainPage.createAdvice("Claude Code", "Подписка Claude Code", "0");
            mainPage.isInputInvalid();
            mainPage.closeForm();
        });

        Allure.step("Проверяем что такое объявление не создалось", () -> {
            mainPage.openCategoryByName("Claude Code");
            Assert.assertFalse(mainPage.advicesContainsName("Подписка Claude Code"));
        });
    }

    @Test
    @Owner("Kriksson")
    @DisplayName("Удаление своего объявления")
    public void deleteOrderTest() {
        MainPage mainPage = Allure.step("Открываем сайт и авторизовываемся", () -> {
            MainPage p = new MainPage(URL);
            p.authorize(ADMIN_LOGIN, ADMIN_PASSWORD);
            return p;
        });
        Allure.step("Создаём объявление и ищем его среди своих", () -> {
            mainPage.createAdvice("ChatGPT", "Ключ Codex", "10993.2");
            mainPage.searchFromMyAdvices("Ключ Codex");
        });

        Allure.step("Проверяем что объявление создалось", () -> {
            Assert.assertTrue(mainPage.advicesContainsName("Ключ Codex"));
        });

        Allure.step("Удаляем объявление и проверяем что оно удалилось", () -> {
            mainPage.deleteAdvice("Ключ Codex");
            Assert.assertFalse(mainPage.advicesContainsName("Ключ Codex"));
        });
    }
}
