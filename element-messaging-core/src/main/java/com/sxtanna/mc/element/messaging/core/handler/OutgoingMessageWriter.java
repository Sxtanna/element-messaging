package com.sxtanna.mc.element.messaging.core.handler;

import org.jetbrains.annotations.NotNull;

import com.sxtanna.mc.element.messaging.core.message.Message;

@FunctionalInterface
public interface OutgoingMessageWriter
{

    void outgoing(@NotNull final Message message);

}
