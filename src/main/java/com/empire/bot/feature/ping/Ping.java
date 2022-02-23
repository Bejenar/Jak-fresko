package com.empire.bot.feature.ping;

import com.empire.bot.config.command.Command;
import com.empire.bot.config.command.CommandDeclaration;
import com.empire.bot.config.command.CommandRequest;
import com.empire.bot.config.command.CommandResponse;
import discord4j.core.object.entity.Message;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@CommandDeclaration("ping")
public class Ping implements Command {

    @Override
    @SneakyThrows
    public Publisher<Void> apply(CommandRequest request, CommandResponse response) {
        log.info("Recieved command: {}", request.getMessage());
        return request.getMessage().getChannel()
            .flatMap(c -> c.createMessage("Pong!"))
            .delayElement(Duration.ofSeconds(2))
            .flatMap(Message::delete)
            .then();
        //return response.sendMessage(spec -> spec.setContent("Pong!")).then();
    }
}


//
//            .doOnNext(m -> Schedulers.boundedElastic()
//                .schedule(() -> m.delete().block(), 2, TimeUnit.SECONDS))
