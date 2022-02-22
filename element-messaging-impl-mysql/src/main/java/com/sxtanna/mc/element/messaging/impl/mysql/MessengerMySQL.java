package com.sxtanna.mc.element.messaging.impl.mysql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import org.bukkit.plugin.Plugin;

import com.sxtanna.mc.element.messaging.core.Messenger;
import com.sxtanna.mc.element.messaging.core.handler.IncomingMessageReader;
import com.sxtanna.mc.element.messaging.core.message.Message;
import com.sxtanna.mc.element.messaging.core.serials.MessageCodec;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class MessengerMySQL implements Messenger
{

    private static final long NONE = -1L;

    private static final long POLL_DELAY_TIME = 0L;
    private static final long POLL_TIMER_TIME = 20L;


    @NotNull
    private static final String MARIADB_FQN = "org.mariadb.jdbc.Driver";
    @NotNull
    private static final String MARIADB_URL = "jdbc:mariadb://%s:%d/%s?useSSL=false";


    private static final String CREATE = "CREATE TABLE IF NOT EXISTS `%s` (`uuid` INT AUTO_INCREMENT PRIMARY KEY NOT NULL, `time` TIMESTAMP NOT NULL, `data` TEXT NOT NULL)";

    private static final String LATEST = "SELECT MAX(`uuid`) as `latest` FROM `%s`";

    private static final String INSERT = "INSERT INTO `%s` (`time`, `data`) VALUES(NOW(), ?)";

    private static final String SELECT = "SELECT `uuid`, `data` FROM `%s` WHERE `uuid` > ? AND (NOW() - `time` < 30)";


    @NotNull
    private final String               table;
    @NotNull
    private final MessageCodec<String> codec;

    @NotNull
    private final Plugin       plugin;
    @NotNull
    private final HikariConfig config;

    @NotNull
    private final AtomicBoolean                     loaded = new AtomicBoolean();
    @NotNull
    private final AtomicReference<HikariDataSource> source = new AtomicReference<>();


    @NotNull
    private final AtomicLong lastId = new AtomicLong(NONE);
    @NotNull
    private final AtomicLong taskId = new AtomicLong(NONE);


    @NotNull
    private final Map<UUID, IncomingMessageReader> readers = new LinkedHashMap<>();


    public MessengerMySQL(@NotNull final Plugin plugin,

                          @NotNull final MessageCodec<String> codec,

                          @NotNull final String host, final int port,

                          @NotNull final String username,
                          @NotNull final String password,
                          @NotNull final String database,

                          @NotNull final String table)
    {
        this.table = table;
        this.codec = codec;

        this.plugin = plugin;
        this.config = createConfig(host, port, username, password, database);
    }


    @Override
    public void start()
    {
        if (this.loaded.get())
        {
            return;
        }

        final HikariDataSource prev = this.source.getAndSet(new HikariDataSource(this.config));
        if (prev != null)
        {
            prev.close();
        }

        this.loaded.set(true);


        try (final Connection connection = openConnection())
        {
            try (final PreparedStatement statement = connection.prepareStatement(String.format(CREATE, this.table)))
            {
                statement.execute();
            }

            try (final PreparedStatement statement = connection.prepareStatement(String.format(LATEST, this.table)))
            {
                try (final ResultSet resultSet = statement.executeQuery())
                {
                    if (resultSet.next())
                    {
                        this.lastId.set(resultSet.getLong("latest"));
                    }
                }
            }
        }
        catch (final SQLException ex)
        {
            throw new RuntimeException(ex);
        }


        final long taskId = this.taskId.getAndSet(this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, this::poll, POLL_DELAY_TIME, POLL_TIMER_TIME).getTaskId());
        if (taskId != NONE)
        {
            this.plugin.getServer().getScheduler().cancelTask(((int) taskId));
        }
    }

    @Override
    public void close()
    {
        if (!this.loaded.get())
        {
            return;
        }

        final long lastId = this.lastId.getAndSet(NONE);
        final long taskId = this.taskId.getAndSet(NONE);
        if (taskId != NONE)
        {
            this.plugin.getServer().getScheduler().cancelTask(((int) taskId));
        }

        final HikariDataSource prev = this.source.getAndSet(null);
        if (prev != null)
        {
            prev.close();
        }

        this.loaded.set(false);
    }


    @Override
    public void outgoing(@NotNull final Message message)
    {
        try (final Connection connection = openConnection())
        {
            try (final PreparedStatement statement = connection.prepareStatement(String.format(INSERT, this.table)))
            {
                statement.setString(1, this.codec.encode(message));
                statement.execute();
            }
        }
        catch (final SQLException ex)
        {
            ex.printStackTrace();
        }
    }


    @Override
    public @NotNull @UnmodifiableView Collection<IncomingMessageReader> readers()
    {
        return Collections.unmodifiableCollection(this.readers.values());
    }

    @Override
    public @NotNull UUID register(@NotNull final IncomingMessageReader reader)
    {
        final UUID uuid = UUID.randomUUID();

        this.readers.put(uuid, reader);

        return uuid;
    }

    @Override
    public boolean unregister(@NotNull final UUID uuid)
    {
        return this.readers.remove(uuid) != null;
    }


    private void poll()
    {
        try (final Connection connection = openConnection())
        {
            try (final PreparedStatement statement = connection.prepareStatement(String.format(SELECT, this.table)))
            {
                statement.setLong(1, this.lastId.get());

                try (final ResultSet resultSet = statement.executeQuery())
                {
                    while (resultSet.next())
                    {
                        final long uuid = resultSet.getLong("uuid");
                        this.lastId.set(Math.max(this.lastId.get(), uuid));

                        final String data = resultSet.getString("data");
                        if (data != null)
                        {
                            push(this.codec.decode(data));
                        }
                    }
                }
            }
        }
        catch (final SQLException ex)
        {
            ex.printStackTrace();
        }
    }

    private void push(@NotNull final Message message)
    {
        for (final IncomingMessageReader reader : this.readers.values())
        {
            try
            {
                reader.incoming(message);
            }
            catch (final Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }


    private @NotNull Connection openConnection() throws SQLException
    {
        return Objects.requireNonNull(this.source.get(), "messenger not loaded").getConnection();
    }


    private static @NotNull HikariConfig createConfig(@NotNull final String host, final int port, @NotNull final String username, @NotNull final String password, @NotNull final String database)
    {
        final HikariConfig config = new HikariConfig();

        config.setUsername(username);
        config.setPassword(password);

        config.setDriverClassName(MARIADB_FQN);
        config.setJdbcUrl(String.format(MARIADB_URL, host, port, database));

        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cacheCallableStmts", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("alwaysSendSetIsolation", false);

        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        return config;
    }

}
