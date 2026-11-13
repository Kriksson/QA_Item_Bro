package UI;

import API.ApiUtility;
import API.Records.CategoryDTO;
import UI.Data.Advice;
import UI.Pages.AdvicesPage;
import UI.Pages.MainPage;
import com.codeborne.selenide.Condition;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AdviceTest extends TestBaseUI {

    public static final String URL = "http://localhost:5173";

    @Test
    @Owner("kriksson")
    @DisplayName("Пользователь создаёт объявление с правильными названием, категорией и ценой")
    public void successfulCreateAdvice() {
        CategoryDTO categoryDTO = ApiUtility.createTestCategory("http://localhost:8085", ApiUtility.genNick());
        String adviceName = ApiUtility.genNick();

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");

        AdvicesPage advicesPage = mainPage.createAdvice(categoryDTO.name(), adviceName, "10.5");
        Assert.assertTrue(advicesPage.adviceContain(adviceName));

        Advice expectedAdvice = new Advice(categoryDTO.name(), adviceName, "MAJOR", "10.5");
        Advice currentAdvice = advicesPage.getAdviceInfoByName(adviceName);

        Allure.step("Проверяем данные созданного объявления", () -> {
            Assert.assertEquals(expectedAdvice.adviceName(), currentAdvice.adviceName());
            Assert.assertEquals(expectedAdvice.categoryName(), currentAdvice.categoryName());
            Assert.assertEquals(expectedAdvice.price(), currentAdvice.price());
            Assert.assertEquals("MAJOR", currentAdvice.sellerName());
        });
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Пользователь архивирует своё объявление")
    public void successfulArchiveOwnAdvice() {
        CategoryDTO categoryDTO = ApiUtility.createTestCategory("http://localhost:8085", ApiUtility.genNick());
        String adviceName = ApiUtility.genNick();

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");

        AdvicesPage advicesPage = mainPage.createAdvice(categoryDTO.name(), adviceName, "10.5");

        advicesPage.archiveAdvice(adviceName);
        Assert.assertFalse(advicesPage.adviceContain(adviceName));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Ошибки в форме объявления блокируют публикацию")
    public void unsuccessfulCreateAdviceWithInvalidData() {
        CategoryDTO categoryDTO = ApiUtility.createTestCategory("http://localhost:8085", ApiUtility.genNick());

        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");

        Allure.step("Нажимаем опубликовать без ввода данных и ожидаем ошибку категории", () -> {
            mainPage.createAdviceButton.scrollIntoCenter().click();
            mainPage.adviceCreateSubmitButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            Assert.assertTrue(mainPage.hasAdviceCategorySelectorError());
        });

        Allure.step("Нажимаем опубликовать после ввода категории ожидаем ошибку имени", () -> {
            mainPage.selectCategoryForAdviceSelect.shouldBe(Condition.visible).selectOptionContainingText(categoryDTO.name());
            mainPage.adviceCreateSubmitButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            Assert.assertTrue(mainPage.hasAdviceNameError());
        });

        Allure.step("Нажимаем опубликовать после ввода названия ожидаем ошибку цены", () -> {
            mainPage.adviceNameInput.shouldBe(Condition.visible).setValue(ApiUtility.genNick());
            mainPage.adviceCreateSubmitButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            Assert.assertTrue(mainPage.hasAdvicePriceError());
        });

        Allure.step("Нажимаем опубликовать после ввода нулевой цены ожидаем ошибку цены", () -> {
            mainPage.advicePriceInput.shouldBe(Condition.visible).setValue("0");
            mainPage.adviceCreateSubmitButton.shouldBe(Condition.visible).scrollIntoCenter().click();
            Assert.assertTrue(mainPage.hasAdvicePriceError());
        });
    }
}
