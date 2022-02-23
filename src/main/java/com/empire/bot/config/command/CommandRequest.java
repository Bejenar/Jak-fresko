package com.empire.bot.config.command;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface CommandRequest {

    MessageCreateEvent event();

    String command();

    String parameters();

    Mono<MessageChannel> getReplyChannel();

    Mono<PrivateChannel> getPrivateChannel();

    default GatewayDiscordClient getClient() {
        return event().getClient();
    }

    default Message getMessage() {
        return event().getMessage();
    }

    default Optional<User> getAuthor() {
        return event().getMessage().getAuthor();
    }

    default Mono<Boolean> hasPermission(PermissionSet requiredPermissions) {
        return Mono.justOrEmpty(getAuthor().map(User::getId))
            .flatMap(authorId -> getMessage().getChannel().ofType(GuildChannel.class)
                .flatMap(channel -> channel.getEffectivePermissions(authorId))
                .map(set -> set.containsAll(requiredPermissions)));
    }
}