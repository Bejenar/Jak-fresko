package com.empire.bot.feature.tictactoe;

import com.empire.bot.config.command.Command;
import com.empire.bot.config.command.CommandDeclaration;
import com.empire.bot.config.command.CommandRequest;
import com.empire.bot.config.command.CommandResponse;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandDeclaration("tictactoe")
public class TicTacToe implements Command {

    private static final ReactionEmoji SQUARE = ReactionEmoji.unicode("\uD83D\uDFE9");
    private static final ReactionEmoji O = ReactionEmoji.unicode("\uD83D\uDFE2");
    private static final ReactionEmoji X = ReactionEmoji.unicode("❎");

    private final GatewayDiscordClient client;

    private final LayoutComponent[] board = {
        ActionRow.of(
            Button.primary("1", SQUARE),
            Button.primary("2", SQUARE),
            Button.primary("3", SQUARE)
        ),
        ActionRow.of(
            Button.primary("4", SQUARE),
            Button.primary("5", SQUARE),
            Button.primary("6", SQUARE)
        ),
        ActionRow.of(
            Button.primary("7", SQUARE),
            Button.primary("8", SQUARE),
            Button.primary("9", SQUARE)
        ),
    };

    @Override
    public Publisher<Void> apply(CommandRequest request, CommandResponse response) {
        client.on(ButtonInteractEvent.class).subscribe(e -> {
            board[0].getChildren().set(0, Button.primary("1", X));
            Mono.justOrEmpty(e.getInteraction().getMessage())
                .map(Message::getId)
                .filter(e.getInteraction().getId()::equals)
                .then(e.edit(spec -> {
                    spec.setComponents(board);
                }))
                .block();
        });


        return response.sendMessage(msg -> {
            msg.setContent("Tic Tac Toe")
                .setComponents(board);

        }).then();
    }
}
