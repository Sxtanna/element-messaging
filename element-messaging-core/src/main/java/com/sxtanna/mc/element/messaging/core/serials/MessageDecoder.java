package com.sxtanna.mc.element.messaging.core.serials;

import org.jetbrains.annotations.NotNull;

import com.sxtanna.mc.element.messaging.core.message.Message;

public interface MessageDecoder<T>
{

    @NotNull Message decode(@NotNull final T message);

}
