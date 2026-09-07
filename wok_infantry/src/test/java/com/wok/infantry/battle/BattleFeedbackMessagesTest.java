package com.wok.infantry.battle;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleFeedbackMessagesTest {
    @Test
    void markerSuccessTravelsAsACompactClientLocalizedToken() {
        String wireMessage = BattleFeedbackMessages.markerCreated(TacticalMarkerType.IFV);
        Component resolved = BattleFeedbackMessages.resolve(wireMessage);

        assertTrue(wireMessage.length() < 64);
        TranslatableContents contents = assertInstanceOf(
                TranslatableContents.class, resolved.getContents());
        assertEquals("message.wok_infantry.marker_created", contents.getKey());
    }

    @Test
    void legacyAndUnknownFeedbackRemainLiteral() {
        assertEquals("plain server message",
                BattleFeedbackMessages.resolve("plain server message").getString());
        assertEquals("@wok_infantry:marker_created:unknown",
                BattleFeedbackMessages.resolve(
                        "@wok_infantry:marker_created:unknown").getString());
    }
}
