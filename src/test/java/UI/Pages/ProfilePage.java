package UI.Pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$x;

public class ProfilePage {

    public SelenideElement profileDetailsText = $x("//p[@class='profile-details']");
    public SelenideElement profileAboutText = $x("//p[@class='profile-about']");
}
