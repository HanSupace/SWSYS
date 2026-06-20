package com.daily.lastsys.features.map;

import com.daily.lastsys.features.login.LoginUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapControllerTest {
    private final MapController controller = new MapController();

    @Test
    void redirectsAnonymousUsersToLogin() {
        assertEquals("redirect:/login", controller.showMap(null));
    }

    @Test
    void rendersMapTemplateFromHomeDirectoryForLoggedInUsers() {
        LoginUser loginUser = new LoginUser(1L, "tester", "테스터");

        assertEquals("home/map", controller.showMap(loginUser));
    }
}
