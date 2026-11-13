package UI.Pages;

import UI.Data.Advice;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

public class AdvicesPage {

    ElementsCollection advicesNameList = $$x("//div[@class='listing-grid']//div[@class='listing-info']//h3");
    ElementsCollection advicesList = $$x("//div[@class='listing-grid']//article");
    SelenideElement searchInput = $x("//input[@aria-label='Поиск предмета на странице']");

    SelenideElement confirmArchive = $x("//button[text()='Архивировать']");
    public SelenideElement createOrderButton = $x("//dialog//button[contains(., 'Создать заказ')]");
    public SelenideElement loginForBuyButton = $x("//dialog//button[contains(., 'Войти для покупки')]");
    public SelenideElement ownAdviceButton = $x("//dialog//button[contains(., 'Твоё объявление')]");


    public AdvicesPage() {}

    public void openAdvice(String adviceName) {
        Allure.step("Открываем объявление " + adviceName, () -> {
            searchInput.scrollIntoCenter().setValue(adviceName);
            advicesList.findBy(Condition.text(adviceName)).shouldBe(Condition.visible)
                    .$x(".//button[@class='listing-open-title']").scrollIntoCenter().click();
        });
    }

    public OrderPage buyAdvice() {
        return Allure.step("Покупаем объявление", () -> {
            createOrderButton.shouldBe(Condition.visible).shouldBe(Condition.enabled).scrollIntoCenter().click();
            $x("//section[@class='order-workspace']").shouldBe(Condition.visible);
            return new OrderPage();
        });
    }

    public boolean adviceContain(String adviceName) {
        return Allure.step("Ищем объявление в поиске", () -> {
            searchInput.setValue(adviceName);
            return advicesNameList.findBy(Condition.text(adviceName)).is(Condition.visible, Duration.ofSeconds(5));
        });
    }

    public Advice getAdviceInfoByName(String adviceName) {
        return Allure.step("Ищем объявление в поиске", () -> {
            searchInput.scrollIntoCenter().setValue(adviceName);
            SelenideElement advice = advicesList.first();
            String sellerName = advice
                    .$x(".//span[@class='seller-identity']/button").scrollIntoCenter().getText();
            String price = advice.$x(".//div[@class='listing-price']").scrollIntoCenter()
                    .getText().replace(" ", "").replace(",", ".").replace("₽", "");
            String categoryName =
                    advice.$x(".//span[@class='item-category']")
                    .scrollIntoCenter().getText();
            return new Advice(categoryName, adviceName, sellerName, price);
        });
    }

    public void archiveAdvice(String adviceName) {
        Allure.step("Архивируем объявление", () -> {
            searchInput.scrollIntoCenter().setValue(adviceName);
            SelenideElement advice = advicesList.first();
            SelenideElement archiveButton = advice.$x(".//button[text()='В архив']");
            archiveButton.scrollIntoCenter().click();
            confirmArchive.shouldBe(Condition.visible).scrollIntoCenter().click();
        });
    }
}
