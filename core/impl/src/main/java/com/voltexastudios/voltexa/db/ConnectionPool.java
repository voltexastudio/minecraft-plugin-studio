package com.voltexastudios.voltexa.db;

import com.voltexastudios.voltexa.log.DefaultLogService;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small fixed-size JDBC connection pool. The core is the only owner of
 * connections; feature plugins never touch the driver directly. Borrowing
 * blocks until a connection is free or the timeout elapses.
 */
public final class ConnectionPool {

    private final DatabaseProvider provider;
    private final int maxSize;
    private final DefaultLogService log;
    private final BlockingQueue<Connection> idle;
    private final AtomicInteger created = new AtomicInteger();
    private final AtomicInteger inUse = new AtomicInteger();
    private volatile boolean closed;

    public ConnectionPool(DatabaseProvider provider, int maxSize, DefaultLogService log) {
        this.provider = provider;
        this.maxSize = Math.max(1, maxSize);
        this.log = log;
        this.idle = new ArrayBlockingQueue<>(this.maxSize);
    }

    /**
     * Borrows a connection, creating one if under the pool cap, otherwise
     * waiting for a release.
     *
     * @throws SQLException if the pool is closed or the wait times out
     */
    public Connection borrow(long timeout, TimeUnit unit) throws SQLException, InterruptedException {
        if (closed) {
            throw new SQLException("connection pool is closed");
        }
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true) {
            Connection connection = idle.poll();
            if (connection != null) {
                if (isValid(connection)) {
                    inUse.incrementAndGet();
                    return connection;
                }
                discard(connection);
                continue;
            }
            if (created.get() < maxSize && created.incrementAndGet() <= maxSize) {
                Connection fresh = provider.createConnection();
                inUse.incrementAndGet();
                return fresh;
            }
            if (created.get() > maxSize) {
                created.decrementAndGet(); // lost the creation race; wait below
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new SQLException(
                        "timed out after " + timeout + " " + unit + " waiting for a database connection");
            }
            idle.poll(Math.min(remaining, unit.toNanos(50)), TimeUnit.NANOSECONDS);
        }
    }

    /** Returns a borrowed connection to the pool, or discards it if unusable. */
    public void release(Connection connection) {
        if (connection == null) {
            return;
        }
        inUse.decrementAndGet();
        if (closed || !isValid(connection)) {
            discard(connection);
            return;
        }
        if (!idle.offer(connection)) {
            discard(connection);
        }
    }

    /** Closes the pool and all idle connections. */
    public void close() {
        closed = true;
        Connection connection;
        while ((connection = idle.poll()) != null) {
            discard(connection);
        }
        provider.close();
    }

    /** Connections currently checked out. */
    public int inUse() {
        return inUse.get();
    }

    /** Pool cap. */
    public int maxSize() {
        return maxSize;
    }

    /** Product name of the underlying database. */
    public String product() {
        return provider.product();
    }

    private void discard(Connection connection) {
        created.decrementAndGet();
        try {
            connection.close();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    private boolean isValid(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
