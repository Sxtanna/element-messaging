package com.sxtanna.mc.element.messaging.core.serials;

import org.jetbrains.annotations.NotNull;

import com.sxtanna.mc.element.messaging.core.message.Message;

@FunctionalInterface
public interface MessageEncoder<T>
{

    @NotNull T encode(@NotNull final Message message);

}
