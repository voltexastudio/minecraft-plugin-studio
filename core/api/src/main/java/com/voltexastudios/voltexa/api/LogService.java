package com.voltexastudios.voltexa.api;

/**
 * One consistent logger for everything the studio ships. Messages are written
 * through the Voltexa core's plugin logger, so all studio output carries the
 * same {@code [Voltexa]} prefix in the server console. Feature plugins log
 * through this instead of their own loggers.
 *
 * <p>Methods take a format string and optional arguments (printf-style); with
 * no arguments the string is used as-is.
 */
public interface LogService {

    /** Logs an informational message. */
    void info(String format, Object... args);

    /** Logs a warning. */
    void warn(String format, Object... args);

    /** Logs an error. */
    void error(String format, Object... args);

    /** Logs a debug message (visible only when fine logging is enabled). */
    void debug(String format, Object... args);
}
