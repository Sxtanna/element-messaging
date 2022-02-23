package com.sxtanna.mc.element.messaging.core.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxtanna.mc.element.messaging.core.message.Message;
import com.sxtanna.mc.element.messaging.core.serials.MessageCodec;
import com.sxtanna.mc.element.messaging.core.serials.MessageDecoder;
import com.sxtanna.mc.element.messaging.core.serials.MessageEncoder;
import com.sxtanna.mc.element.messaging.core.serials.builtins.MessageCodecGson;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MessageCodecGsonTests {

    public static final class TestMessage implements Message {

        @NotNull
        private final UUID uuid;

        public TestMessage(@NotNull UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public @NotNull UUID uuid() {
            return this.uuid;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestMessage)) return false;
            TestMessage that = (TestMessage) o;
            return uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

    }


    public static final MessageDecoder<JsonElement> DECODER = (json) -> {
        return new TestMessage(UUID.fromString(json.getAsJsonObject().get("uuid").getAsString()));
    };

    public static final MessageEncoder<JsonElement> ENCODER = (data) -> {
        final JsonObject json = new JsonObject();

        json.addProperty("uuid", data.uuid().toString());

        return json;
    };


    @Test
    void testJsonElementRoundTrip() {
        final Message message = new TestMessage(UUID.randomUUID());

        final JsonElement encoded = assertDoesNotThrow(() -> ENCODER.encode(message));

        final Message decoded = assertDoesNotThrow(() -> DECODER.decode(encoded));

        assertEquals(message, decoded);
    }

    @Test
    void testJsonElementGsonCodec() {
        final MessageCodec<String> codec = new MessageCodecGson(DECODER, ENCODER);

        final Message message = new TestMessage(UUID.randomUUID());

        final String encoded = assertDoesNotThrow(() -> codec.encode(message));
        assertEquals("{\"uuid\":\"" + message.uuid() + "\"}", encoded);

        final Message decoded = assertDoesNotThrow(() -> codec.decode(encoded));
        assertEquals(message, decoded);
    }

}
