package com.empire.bot.config.command;

import discord4j.common.annotations.Experimental;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@Experimental
public class CommandListener extends ReactiveEventAdapter {

    private final Function<MessageCreateEvent, Publisher<String>> prefixFunction;
    private final Function<? super CommandRequest, Mono<Boolean>> filter;
    private final CopyOnWriteArrayList<CommandHandler> handlers;
    private final CopyOnWriteArrayList<BiFunction<? super CommandRequest, Throwable, Publisher<?>>> errorHandlers;

    CommandListener(Function<MessageCreateEvent, Publisher<String>> prefixFunction,
                    Function<? super CommandRequest, Mono<Boolean>> filter) {
        this.prefixFunction = prefixFunction;
        this.filter = filter;
        this.handlers = new CopyOnWriteArrayList<>();
        this.errorHandlers = new CopyOnWriteArrayList<>();
    }

    public static CommandListener create() {
        return createWithPrefix(__ -> Mono.just(""));
    }

    public static CommandListener createWithPrefix(String prefix) {
        return createWithPrefix(__ -> Mono.justOrEmpty(prefix));
    }

    public static CommandListener createWithPrefix(Function<MessageCreateEvent, Publisher<String>> prefixFunction) {
        return new CommandListener(Objects.requireNonNull(prefixFunction, "prefixFunction"), __ -> Mono.just(true));
    }

    public CommandListener filter(Function<? super CommandRequest, Mono<Boolean>> condition) {
        return new CommandListener(this.prefixFunction,
            req -> this.filter.apply(req).flatMap(result -> condition.apply(req)));
    }

    public CommandListener on(String command, Command handler) {
        return handle(request -> request.command().equals(command), handler);
    }

    public CommandListener handle(Predicate<? super CommandRequest> condition, Command handler) {
        handlers.add(new CommandHandler(condition, handler));
        return this;
    }

    public CommandListener handle(Command handler) {
        handlers.add(new CommandHandler(__ -> true, handler));
        return this;
    }

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event) {
        if (event.getMessage().getAuthor().map(User::isBot).orElse(true)) {
            return Mono.empty();
        } else {
            String content = event.getMessage().getContent();
            return Flux.defer(() -> prefixFunction.apply(event))
                .filter(content::startsWith)
                .map(prefix -> {
                    String trimmed = content.substring(prefix.length()).trim();
                    int endIndex = (trimmed.indexOf(' ') < 0) ? trimmed.length() : trimmed.indexOf(' ');
                    String commandName = trimmed.substring(0, endIndex);
                    String remaining = trimmed.substring(commandName.length()).trim();
                    return new CommandOperations(event, commandName, remaining);
                })
                .filterWhen(filter)
                .flatMap(ops -> applyCommand(ops, ops));
        }
    }

    private Mono<Void> applyCommand(CommandRequest req, CommandResponse res) {
        return Mono.from(handlers.stream()
            .filter(handler -> handler.test(req))
            .findFirst()
            .orElse(CommandHandler.NOOP_HANDLER)
            .apply(req, res));
    }
}