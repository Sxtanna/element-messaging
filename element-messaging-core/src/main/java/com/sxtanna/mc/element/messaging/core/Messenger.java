package com.sxtanna.mc.element.messaging.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import com.sxtanna.mc.element.messaging.core.handler.IncomingMessageReader;
import com.sxtanna.mc.element.messaging.core.handler.OutgoingMessageWriter;
import com.sxtanna.mc.element.messaging.core.message.Message;

import java.util.Collection;
import java.util.UUID;

public interface Messenger extends OutgoingMessageWriter, AutoCloseable
{

    default void start()
    {

    }

    @Override
    default void close()
    {

    }


    @Override
    void outgoing(@NotNull final Message message);



    @NotNull @UnmodifiableView Collection<IncomingMessageReader> readers();


    @NotNull UUID register(@NotNull final IncomingMessageReader reader);

    boolean unregister(@NotNull final UUID uuid);

}
