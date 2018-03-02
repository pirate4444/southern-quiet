package test.app;

import com.ai.southernquiet.logging.MongoDbLoggingAutoConfiguration;
import com.ai.southernquiet.util.BCrypt;
import com.ai.southernquiet.web.AbstractWebApp;
import com.ai.southernquiet.web.CommonWebAutoConfiguration;
import com.ai.southernquiet.web.auth.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;

@SuppressWarnings("unused")
@RestController
@SpringBootApplication(scanBasePackages = {"com.ai.southernquiet"})
@EnableScheduling
@EnableSpringHttpSession
public class App extends AbstractWebApp {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class);
    }

    @Configuration
    public static class Config {
        @SuppressWarnings("Duplicates")
        @Bean
        public AuthService authService(CommonWebAutoConfiguration.WebProperties webProperties) {
            return new AuthService() {
                private Logger logger = LoggerFactory.getLogger(App.class);

                private User<Account> user = new User<>(
                    () -> "superman",
                    "2636d11c-7e52-4d12-80b5-893116c20cce",
                    webProperties.getAuthenticationTTL()
                );

                @Override
                public User<Account> authenticate(String username, String password, boolean remember) throws AuthException {
                    user.setAuthenticationTime(Instant.now());

                    if (!user.getAccount().getName().equals(username)) throw new UserNotFoundException(username);
                    String hashed = BCrypt.hashpw("givemefive", BCrypt.gensalt());
                    logger.debug(hashed);
                    if (!BCrypt.checkpw(password, hashed)) {
                        throw new IncorrectPasswordException("");
                    }

                    return user;
                }

                @Override
                public User<Account> getUserByRememberToken(String token) {
                    if (!token.equals(user.getRememberToken())) return null;

                    return user;
                }

                @Override
                public boolean checkAuthorization(String username, Set<String> authNames) {
                    return true;
                }
            };
        }
    }

    @RequestMapping("/")
    String home() {
        logger.debug("你好，Spring Boot！");
        return "Hello World!";
    }

    @PostMapping("/login")
    String login(Request request, String username, String password) {
        try {
            request.login(username, password, true);
        }
        catch (UserNotFoundException e) {
            return "UserNotFound " + e.getMessage();
        }
        catch (IncorrectPasswordException e) {
            return "IncorrectPassword " + e.getMessage();
        }
        catch (AuthException e) {
            return e.getMessage();
        }

        return "DONE";
    }

    @Auth
    @RequestMapping("/user")
    String user(HttpServletRequest request) {
        return request.getRemoteUser();
    }
}
