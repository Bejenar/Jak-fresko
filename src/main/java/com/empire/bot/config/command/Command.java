package com.empire.bot.config.command;

import org.reactivestreams.Publisher;

import java.util.function.BiFunction;

public interface Command extends BiFunction<CommandRequest, CommandResponse, Publisher<Void>> {
}