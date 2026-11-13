package UI;

import API.ApiUtility;
import UI.Pages.AdminControlPage;
import UI.Pages.MainPage;
import io.qameta.allure.Owner;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AdminTest extends TestBaseUI {

    public static final String URL = "http://localhost:5173";

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор создаёт категорию")
    public void successfulCreateCategoryByAdmin() {
        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("ADMIN", "Admin123!");
        AdminControlPage adminControlPage = mainPage.openAdminControlPage();
        String categoryName = ApiUtility.genNick();
        adminControlPage.createCategory(categoryName);
        Assert.assertTrue(adminControlPage.categoryContain(categoryName));
    }

    @Test
    @Owner("kriksson")
    @DisplayName("Администратор архивирует категорию")
    public void successfulArchiveCategoryByAdmin() {
        MainPage mainPage = new MainPage(URL);
        mainPage.authUser("ADMIN", "Admin123!");
        AdminControlPage adminControlPage = mainPage.openAdminControlPage();
        String categoryName = ApiUtility.genNick();
        adminControlPage.createCategory(categoryName);
        adminControlPage.archiveCategoryByName(categoryName);
        Assert.assertFalse(adminControlPage.categoryContain(categoryName));
    }

}
