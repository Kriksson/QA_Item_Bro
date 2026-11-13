package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;

import static com.codeborne.selenide.Selenide.$x;

public class ProfileSettingsPage {

    public SelenideElement ageInput = $x("//input[@name='age']");
    public SelenideElement cityInput = $x("//input[@name='city']");
    public SelenideElement aboutInput = $x("//textarea[@name='about']");

    public void editProfile(String age, String city, String about) {
        Allure.step("Изменяем сведения профиля и сохраняем", () -> {
            ageInput.shouldBe(Condition.visible).setValue(age);
            cityInput.setValue(city);
            aboutInput.setValue(about);
            $x("//button[text()='Сохранить профиль']").scrollIntoCenter().click();
            $x("//p[@class='settings-saved']").shouldHave(Condition.exactText("Профиль сохранён."));
        });
    }
}
