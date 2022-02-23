package com.sxtanna.mc.element.messaging.core.serials.builtins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sxtanna.mc.element.messaging.core.message.Message;
import com.sxtanna.mc.element.messaging.core.serials.MessageCodec;
import com.sxtanna.mc.element.messaging.core.serials.MessageDecoder;
import com.sxtanna.mc.element.messaging.core.serials.MessageEncoder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class MessageCodecGson implements MessageCodec<String> {

    @NotNull
    private final MessageDecoder<JsonElement> decoder;
    @NotNull
    private final MessageEncoder<JsonElement> encoder;

    @NotNull
    private final Gson gson;


    @Contract(pure = true)
    public MessageCodecGson(@NotNull final MessageDecoder<JsonElement> decoder,
                            @NotNull final MessageEncoder<JsonElement> encoder) {
        this(decoder, encoder, $ -> {});
    }

    @Contract(pure = true)
    public MessageCodecGson(@NotNull final MessageDecoder<JsonElement> decoder,
                            @NotNull final MessageEncoder<JsonElement> encoder,

                            @NotNull final Consumer<GsonBuilder> consumer) {
        this.decoder = decoder;
        this.encoder = encoder;

        final GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls()
               .disableHtmlEscaping()
               .enableComplexMapKeySerialization()
               .serializeSpecialFloatingPointValues();

        consumer.accept(builder);

        this.gson = builder.create();
    }


    @Override
    public @NotNull String encode(@NotNull final Message message) {
        return this.gson.toJson(this.encoder.encode(message));
    }

    @Override
    public @NotNull Message decode(@NotNull final String message) {
        return this.decoder.decode(JsonParser.parseString(message));
    }

}
