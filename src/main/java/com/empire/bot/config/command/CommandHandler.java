package com.empire.bot.config.command;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

class CommandHandler implements BiFunction<CommandRequest, CommandResponse, Publisher<Void>>,
    Predicate<CommandRequest> {

    private final Predicate<? super CommandRequest> condition;
    private final BiFunction<? super CommandRequest, ? super CommandResponse, ? extends Publisher<Void>> handler;

    CommandHandler(
        Predicate<? super CommandRequest> condition,
        BiFunction<? super CommandRequest, ? super CommandResponse, ? extends Publisher<Void>> handler) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @Override
    public Publisher<Void> apply(CommandRequest request, CommandResponse response) {
        return handler.apply(request, response);
    }

    @Override
    public boolean test(CommandRequest request) {
        return condition.test(request);
    }

    static CommandHandler NOOP_HANDLER = new CommandHandler(req -> false, (req, res) -> Mono.empty());
}