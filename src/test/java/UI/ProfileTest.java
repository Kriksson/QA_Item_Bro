package UI;

import UI.Pages.*;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import config.TestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.Owner;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Selenide.$x;

public class ProfileTest extends TestBaseUI {

    public static final String URL = TestConfig.UI_URL;

    @Test
    @Owner("kriksson")
    @DisplayName("Изменённые сведения сохраняются и отображаются в публичном профиле")
    public void successfulEditProfile() {
        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("MAJOR", "Major123!");
        Assert.assertEquals("MAJOR", mainPage.getUsername());
        ProfileSettingsPage settingsPage = mainPage.openProfileSettingsPage();
        String age = "21";
        String city = "Москва";
        String about = "Учусь писать автотесты";
        settingsPage.editProfile(age, city, about);

        Allure.step("Перезагружаем настройки и проверяем сохранённые сведения", () -> {
            Selenide.refresh();
            Assert.assertTrue(WebDriverRunner.url().endsWith("/profile/edit"));
            settingsPage.ageInput.shouldHave(Condition.value(age));
            settingsPage.cityInput.shouldHave(Condition.value(city));
            settingsPage.aboutInput.shouldHave(Condition.value(about));
        });

        ProfilePage profilePage = mainPage.openProfilePage();
        Allure.step("Проверяем сведения в публичном профиле", () -> {
            profilePage.profileDetailsText.shouldHave(Condition.text("21 год")).shouldHave(Condition.text(city));
            profilePage.profileAboutText.shouldHave(Condition.exactText(about));
        });
        mainPage.logOut();

        Allure.step("Проверяем что сведения публичного профиля видны гостю", () -> {
            $x("//header//button[text()='Войти']").shouldBe(Condition.visible);
            Selenide.open(URL+"/users/MAJOR");
            $x("//header//button[text()='Войти']").shouldBe(Condition.visible);
            profilePage.profileDetailsText.shouldHave(Condition.text("21 год")).shouldHave(Condition.text(city));
            profilePage.profileAboutText.shouldHave(Condition.exactText(about));
        });
    }

}
