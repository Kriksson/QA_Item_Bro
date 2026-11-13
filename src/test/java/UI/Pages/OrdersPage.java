package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import io.qameta.allure.Allure;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$$x;

public class OrdersPage {

    ElementsCollection ordersList = $$x("//div[@class='order-list']/button");

    public boolean orderContain(String adviceName) {
        return Allure.step("Проверяем наличие заказа по объявлению " + adviceName, () -> ordersList
                .findBy(Condition.text(adviceName)).is(Condition.visible, Duration.ofSeconds(5)));
    }

    public OrderPage openOrder(String adviceName) {
        return Allure.step("Открываем заказ по объявлению " + adviceName, () -> {
            ordersList.findBy(Condition.text(adviceName)).shouldBe(Condition.visible).scrollIntoCenter().click();
            return new OrderPage();
        });
    }
}
