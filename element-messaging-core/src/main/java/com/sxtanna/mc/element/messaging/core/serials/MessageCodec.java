package com.sxtanna.mc.element.messaging.core.serials;

import org.jetbrains.annotations.NotNull;

import com.sxtanna.mc.element.messaging.core.message.Message;

public interface MessageCodec<T> extends MessageEncoder<T>, MessageDecoder<T>
{

    @Override
    @NotNull T encode(@NotNull final Message message);

    @Override
    @NotNull Message decode(@NotNull final T message);

}
