package com.empire.bot.config;

import com.empire.bot.config.command.Command;
import com.empire.bot.config.command.CommandDeclaration;
import com.empire.bot.config.command.CommandListener;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DiscordClientConfig {

    private static final String COMMAND_NAME_NOT_FOUND_ERROR_MESSAGE = "Command implementations registered as beans should be annotated with com.empire.bot.config.command.CommandDeclaration";

    @Bean
    public GatewayDiscordClient gatewayDiscordClient(@Value("${discord.bot.token}") final String token) {
        return DiscordClient
            .create(token)
            .login()
            .block();

    }

    @Bean
    public CommandLineRunner initializer(@Value("${discord.bot.prefix}") final String prefix,
                                         GatewayDiscordClient client,
                                         List<Command> commands) {
        return args -> {
            CommandListener commandListener = CommandListener.createWithPrefix(prefix);
            commands.forEach(c -> {
                commandListener.on(resolveCommandName(c), c);
            });
            client.on(commandListener).blockLast();
        };
    }

    private static String resolveCommandName(Command command) {
        CommandDeclaration commandDeclaration = Arrays.stream(command.getClass().getAnnotations())
            .filter(a -> a instanceof CommandDeclaration)
            .map(a -> (CommandDeclaration) a)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(COMMAND_NAME_NOT_FOUND_ERROR_MESSAGE));

        return commandDeclaration.value();
    }
}
