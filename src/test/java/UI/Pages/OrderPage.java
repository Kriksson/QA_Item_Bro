package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;

import java.math.BigDecimal;

import static com.codeborne.selenide.Selenide.$x;

public class OrderPage {

    public SelenideElement orderSection = $x("//section[@class='order-workspace']");
    public SelenideElement statusText = orderSection.$x(".//div[@class='order-heading-badges']/span[contains(@class, 'order-status')]");
    public SelenideElement confirmOrderButton = orderSection.$x(".//button[contains(., 'Подтвердить получение')]");
    public SelenideElement cancelOrderButton = orderSection.$x(".//button[text()='Отменить заказ']");
    public SelenideElement feedbackText = orderSection.$x(".//div[@class='review-block']/p");
    public SelenideElement sellerResponseText = orderSection.$x(".//div[@class='review-block']/blockquote/p");
    public SelenideElement ratingStars = orderSection.$x(".//div[@class='review-stars']");
    public SelenideElement messageInput = orderSection.$x(".//textarea[@aria-label='Сообщение']");

    public String getAdviceName() {
        return orderSection.$x(".//div[@class='order-summary-product']/h2").shouldBe(Condition.visible).getText();
    }

    public BigDecimal getPrice() {
        return new BigDecimal(orderSection.$x(".//div[@class='order-summary-product']/strong")
                .shouldBe(Condition.visible).getText().replaceAll("[\\s\\u00a0\\u202f₽]", "").replace(",", "."));
    }

    public String getBuyerName() {
        return orderSection.$x(".//div[@class='order-parties']/div[1]/strong").shouldBe(Condition.visible).getText();
    }

    public String getSellerName() {
        return orderSection.$x(".//div[@class='order-parties']/div[2]/strong").shouldBe(Condition.visible).getText();
    }

    public void confirmOrder() {
        Allure.step("Покупатель подтверждает получение", () -> {
            confirmOrderButton.shouldBe(Condition.visible).shouldBe(Condition.enabled).scrollIntoCenter().click();
            statusText.shouldHave(Condition.exactText("Закрыт"));
        });
    }

    public void cancelOrder() {
        Allure.step("Продавец отменяет заказ", () -> {
            cancelOrderButton.shouldBe(Condition.visible).shouldBe(Condition.enabled).scrollIntoCenter().click();
            statusText.shouldHave(Condition.exactText("Отменён"));
        });
    }

    public void leaveFeedback(String rating, String text) {
        Allure.step("Покупатель оставляет оценку и отзыв", () -> {
            orderSection.$x(".//select[@name='rating']").shouldBe(Condition.enabled).scrollIntoCenter().selectOptionByValue(rating);
            orderSection.$x(".//textarea[@placeholder='Как всё прошло?']").setValue(text);
            orderSection.$x(".//button[text()='Сохранить отзыв']").scrollIntoCenter().click();
            feedbackText.shouldHave(Condition.exactText(text));
        });
    }

    public void leaveSellerResponse(String text) {
        Allure.step("Продавец отвечает на отзыв", () -> {
            orderSection.$x(".//textarea[@placeholder='Ответь покупателю']")
                    .shouldBe(Condition.visible).shouldBe(Condition.enabled).scrollIntoCenter().setValue(text);
            orderSection.$x(".//button[text()='Сохранить ответ']").scrollIntoCenter().click();
            sellerResponseText.shouldHave(Condition.exactText(text));
        });
    }

    public void sendMessage(String text) {
        Allure.step("Отправляем сообщение в чат заказа", () -> {
            messageInput.shouldBe(Condition.enabled).scrollIntoCenter().setValue(text);
            orderSection.$x(".//button[@aria-label='Отправить сообщение']")
                    .shouldBe(Condition.enabled).scrollIntoCenter().click();
            messageInput.shouldHave(Condition.exactValue(""));
        });
    }
}
