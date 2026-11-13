package UI.Pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Allure;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$$x;
import static com.codeborne.selenide.Selenide.$x;

public class AdminControlPage {

    ElementsCollection categoriesNameList = $$x("//div[@class='managed-games']//span//strong");
    ElementsCollection categoriesList = $$x("//div[@class='managed-games']");
    SelenideElement createCategoryButton = $x("//button[text()='Создать категорию']");
    SelenideElement createCategorySubmitButton = $x("//button[text()='Создать категорию ']");
    SelenideElement categoryNameFormInput = $x("//input[@placeholder='Например, Path of Exile']");
    SelenideElement confirmArchiveButton = $x("//button[text()='Архивировать категорию']");

    public AdminControlPage() {}

    public void createCategory(String categoryName) {
        Allure.step("Создаём категорию "+categoryName, () -> {
            createCategoryButton.click();
            categoryNameFormInput.shouldBe(Condition.visible);
            categoryNameFormInput.setValue(categoryName);
            createCategorySubmitButton.click();
        });
    }

    public boolean categoryContain(String categoryName) {
        return Allure.step("Проверяем наличие категории "+categoryName, () -> categoriesNameList
                .findBy(Condition.exactText(categoryName))
                .is(Condition.visible, Duration.ofSeconds(5)));
    }

    public void archiveCategoryByName(String categoryName) {
        Allure.step("Администратор архивирует категорию " + categoryName, () -> {
            for (SelenideElement category : categoriesList) {
                category.shouldBe(Condition.visible);
                if (category.$x(".//span//strong").getText().equals(categoryName)) {
                    SelenideElement archiveButton = category.$x(".//div//button[contains(@aria-label, 'Архивировать')]");
                    archiveButton.click();
                    confirmArchiveButton.shouldBe(Condition.visible).scrollIntoCenter().click();
                    break;
                }
            }
        });
    }
}
