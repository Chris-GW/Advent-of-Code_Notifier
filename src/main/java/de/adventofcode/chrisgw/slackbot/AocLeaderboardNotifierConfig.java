package de.adventofcode.chrisgw.slackbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.adventofcode.chrisgw.slackbot.service.AocLeaderboardNotifier;
import de.adventofcode.chrisgw.slackbot.service.AocLeaderboardService;
import de.adventofcode.chrisgw.slackbot.service.AocSlackMessageService;
import io.reactivex.disposables.Disposable;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.time.LocalDate;
import java.util.Scanner;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;


@Configuration
@ComponentScan(basePackageClasses = { AocLeaderboardService.class, AocSlackMessageService.class })
@PropertySource("file:advent-of-code.properties")
public class AocLeaderboardNotifierConfig {


    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(
                AocLeaderboardNotifierConfig.class);
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        AocLeaderboardNotifier aocLeaderboardNotifier = applicationContext.getBean(AocLeaderboardNotifier.class);
        if (!environment.containsProperty("leaderboardId")) {
            throw new IllegalArgumentException("leaderboardId was unset");
        }
        long leaderboardId = environment.getProperty("leaderboardId", Long.class, 1L);
        int year = environment.getProperty("year", Integer.class, LocalDate.now().getYear());

        System.out.println("Watching leaderboard with ID: " + leaderboardId);
        Disposable disposable = aocLeaderboardNotifier.subscribeLeaderboard(year, leaderboardId);
        System.out.println("Press Enter to finish");
        new Scanner(System.in).nextLine(); // block until enter
        disposable.dispose();
    }


    @Bean
    public Client client(ObjectMapper objectMapper) {
        return ClientBuilder.newClient()
                .register(new JacksonJaxbJsonProvider(objectMapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS))
                .register(objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        om.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.setVisibility(new Std(NONE, NONE, NONE, DEFAULT, ANY));
        return om;
    }

}
