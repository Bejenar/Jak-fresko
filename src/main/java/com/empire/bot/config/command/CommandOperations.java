package com.empire.bot.config.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;
import java.util.function.Supplier;

class CommandOperations implements CommandRequest, CommandResponse {

    private final MessageCreateEvent event;
    private final String command;
    private final String parameters;
    private final Supplier<Mono<MessageChannel>> replyChannel;
    private final Scheduler replyScheduler;

    CommandOperations(MessageCreateEvent event, String command, String parameters) {
        this.event = event;
        this.command = command;
        this.parameters = parameters;
        this.replyChannel = this::getReplyChannel;
        this.replyScheduler = Schedulers.immediate();
    }

    private CommandOperations(MessageCreateEvent event, String command, String parameters,
                              Supplier<Mono<MessageChannel>> replyChannel, Scheduler replyScheduler) {
        this.event = event;
        this.command = command;
        this.parameters = parameters;
        this.replyChannel = replyChannel;
        this.replyScheduler = replyScheduler;
    }

    @Override
    public MessageCreateEvent event() {
        return event;
    }

    @Override
    public String command() {
        return command;
    }

    @Override
    public String parameters() {
        return parameters;
    }

    @Override
    public Mono<MessageChannel> getReplyChannel() {
        return event.getMessage().getChannel();
    }

    @Override
    public Mono<PrivateChannel> getPrivateChannel() {
        return Mono.justOrEmpty(event.getMessage().getAuthor()).flatMap(User::getPrivateChannel);
    }

    @Override
    public CommandResponse withDirectMessage() {
        return new CommandOperations(event, command, parameters, () -> getPrivateChannel().cast(MessageChannel.class),
            replyScheduler);
    }

    @Override
    public CommandResponse withReplyChannel(Mono<MessageChannel> channelSource) {
        return new CommandOperations(event, command, parameters, () -> channelSource, replyScheduler);
    }

    @Override
    public CommandResponse withScheduler(Scheduler scheduler) {
        return new CommandOperations(event, command, parameters, replyChannel, scheduler);
    }

    @Override
    public Mono<Void> sendMessage(Consumer<? super MessageCreateSpec> spec) {
        return replyChannel.get()
            .publishOn(replyScheduler)
            .flatMap(channel -> channel.createMessage(spec))
            .then();
    }

    @Override
    public Mono<Void> sendEmbed(Consumer<? super EmbedCreateSpec> spec) {
        return replyChannel.get()
            .publishOn(replyScheduler)
            .flatMap(channel -> channel.createEmbed(spec))
            .then();
    }
}