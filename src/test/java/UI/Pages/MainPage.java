package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$x;

public class MainPage {

    SelenideElement logInButton = $x("//button[text()='Войти']");
    SelenideElement selectRegisterButton = $x("//button[text()='Зарегистрироваться']");

    SelenideElement userNameRegisterInput = $x("//input[@placeholder='Твоё имя']");
    SelenideElement passwordRegisterInput = $x("//input[@placeholder='От 8 символов']");
    SelenideElement acceptRegisterButton = $x("//button[text()='Создать аккаунт']");

    SelenideElement userNameAuthInput = $x("//input[@placeholder='Твоё имя']");
    SelenideElement passwordAuthInput = $x("//input[@placeholder='Твой пароль']");
    SelenideElement acceptAuthButton = $x("//button[text()='Войти'][@class='button button-full']");

    SelenideElement authUsernameText = $x("//span[contains(@class, 'username')]");
    SelenideElement logOutButton = $x("//button[text()='Выйти']");
    SelenideElement formErrorText = $x("//p[@class='form-error']");

    SelenideElement adminSectionButton = $x("//button[text()='Управление']");

    public SelenideElement createAdviceButton = $x("//button[text()='Разместить объявление']");
    public SelenideElement selectCategoryForAdviceSelect = $x("//select[@name='game']");
    public SelenideElement adviceNameInput = $x("//input[@placeholder='Что ты продаёшь?']");
    public SelenideElement advicePriceInput = $x("//input[@placeholder='0']");
    public SelenideElement adviceCreateSubmitButton = $x("//button[text()='Опубликовать объявление ']");

    public MainPage(String URL) {
        Allure.step("Переходим на основную страницу", () -> Selenide.open(URL));
    }

    public void registerUser(String username, String password) {
        Allure.step("Регистрируем нового пользователя", () -> {
            logInButton.click();
            selectRegisterButton.shouldBe(Condition.visible);
            selectRegisterButton.click();
            userNameRegisterInput.setValue(username);
            passwordRegisterInput.setValue(password);
            acceptRegisterButton.click();
        });
    }

    public void authUser(String username, String password) {
        Allure.step("Авторизовываемся в аккаунт", () -> {
            logInButton.click();
            userNameAuthInput.shouldBe(Condition.visible);
            userNameAuthInput.setValue(username);
            passwordAuthInput.setValue(password);
            acceptAuthButton.click();
        });
    }

    public void logOut() {
        Allure.step("Выходим из аккаунта", () -> {
            authUsernameText.scrollIntoCenter().click();
            logOutButton.scrollIntoCenter().click();
        });
    }

    public String getUsername() {
        authUsernameText.shouldBe(Condition.visible);
        return authUsernameText.getText();
    }

    public String getFormError() {
        formErrorText.shouldBe(Condition.visible);
        return formErrorText.getText();
    }

    public boolean isAdminFunctionVisible() {
        return adminSectionButton.is(Condition.visible, Duration.ofSeconds(4));
    }

    public AdminControlPage openAdminControlPage() {
        return Allure.step("Открываем панель управления", () -> {
            adminSectionButton.click();
            return new AdminControlPage();
        });
    }

    public AdvicesPage createAdvice(String category, String name, String price) {
        return Allure.step("Создаём объявление в " +category + ", " + name + ", с ценой " + price, () -> {
            createAdviceButton.click();
            selectCategoryForAdviceSelect.shouldBe(Condition.visible);
            selectCategoryForAdviceSelect.selectOptionContainingText(category);
            adviceNameInput.setValue(name);
            advicePriceInput.setValue(price);
            adviceCreateSubmitButton.click();
            return new AdvicesPage();
        });
    }

    public boolean hasAdviceCategorySelectorError() {
        return selectCategoryForAdviceSelect.is(
                Condition.not(
                        Condition.domProperty("validationMessage", "")
                )
        );
    }

    public boolean hasAdviceNameError() {
        return adviceNameInput.is(Condition.not(
                Condition.domProperty("validationMessage", "")
        ));
    }

    public boolean hasAdvicePriceError() {
        return advicePriceInput.is(Condition.not(
                Condition.domProperty("validationMessage", "")
        ));
    }
}
