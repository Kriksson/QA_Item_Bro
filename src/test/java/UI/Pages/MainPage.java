package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

public class MainPage {

    SelenideElement logInButton = $x("//button[@class='button button-small']");
    SelenideElement authorizeButton = $x("//button[@class='button button-full']");
    SelenideElement nameInput = $x("//input[@name='name']");
    SelenideElement passwordInput = $x("//input[@name='password']");

    SelenideElement addAdviceButton = $x("//button[@class='button']");
    SelenideElement gameSelect = $x("//select[@name='game']");
    SelenideElement nameOfAdviceInput = $x("//input[@placeholder='Что ты продаёшь?']");
    SelenideElement priceInput = $x("//input[@placeholder='0']");

    SelenideElement searchFromMyAdvicesInput = $x("//input[@placeholder='Найти предмет на странице']");

    SelenideElement adviceName = $x("//button[@class='listing-open-title']");
    SelenideElement advicePrice = $x("//div[@class='listing-price']");
    SelenideElement adviceCategory = $x("//div//h1");
    SelenideElement closeFormButton = $x("//button[@aria-label='Закрыть окно']");

    ElementsCollection gamesList = $$x("//div[@class='game-card-bottom']");
    ElementsCollection advicesNameList = $$x("//div[@class=\"listing-grid\"]//article//div[@class='listing-info']//h3");
    SelenideElement myAdvicesButton = $x("//button[text()='Мои объявления']");

    SelenideElement deleteAdviceButton = $x("//button[text()='В архив']");
    SelenideElement confirmDeleteAdviceButton = $x("//button[text()='Архивировать']");

    public MainPage(String URL) {
        Selenide.open(URL);
    }

    public void authorize(String login, String password) {
        logInButton.scrollIntoCenter().click();
        nameInput.shouldBe(Condition.visible, Duration.ofSeconds(5));
        nameInput.setValue(login);
        passwordInput.setValue(password);
        authorizeButton.scrollIntoCenter().click();
    }

    public void createAdvice(String category, String nameOfAdvice, String price) {
        addAdviceButton.scrollIntoCenter().click();
        gameSelect.selectOptionContainingText(category);
        nameOfAdviceInput.setValue(nameOfAdvice);
        priceInput.setValue(price);
        authorizeButton.scrollIntoCenter().click();
    }

    public void searchFromMyAdvices(String search) {
        searchFromMyAdvicesInput.setValue(search);
    }

    public String getNameOfAdvice() {
        return adviceName.getText();
    }

    public String getPrice() {
        return advicePrice.getText().replaceAll("[^0-9]", "");
    }

    public String getCategory() {
        String category = adviceCategory.getText();

        return category.substring(0, category.length() - 1);
    }

    public void closeForm() {
        closeFormButton.scrollIntoCenter().click();
    }

    public void openCategoryByName(String category) {
        myAdvicesButton.scrollIntoCenter().click();
        for (SelenideElement selenideElement : gamesList) {
            if (selenideElement.getText().equals(category)) {
                selenideElement.scrollIntoCenter().click();
            }
        }
    }

    public void isInputInvalid() {
        Selenide.$("input[name='price']:invalid")
                .shouldBe(Condition.visible);
    }

    public boolean advicesContainsName(String name) {
        if (advicesNameList.isEmpty()) {
            return false;
        }
        for (int i = 0; i < advicesNameList.size(); i++) {
            if (advicesNameList.get(i).scrollIntoCenter().getText().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void deleteAdvice(String advice) {
        searchFromMyAdvices(advice);
        deleteAdviceButton.scrollIntoCenter().click();
        confirmDeleteAdviceButton.scrollIntoCenter().click();
    }
}
