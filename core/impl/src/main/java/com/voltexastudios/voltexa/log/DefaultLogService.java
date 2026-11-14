package com.voltexastudios.voltexa.log;

import com.voltexastudios.voltexa.api.LogService;
import java.util.logging.Logger;

/**
 * The studio's one consistent logger. Writes through the Voltexa core's
 * plugin logger, so every studio message carries the same {@code [Voltexa]}
 * prefix in the server console.
 */
public final class DefaultLogService implements LogService {

    private final Logger delegate;

    public DefaultLogService(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void info(String format, Object... args) {
        delegate.info(fmt(format, args));
    }

    @Override
    public void warn(String format, Object... args) {
        delegate.warning(fmt(format, args));
    }

    @Override
    public void error(String format, Object... args) {
        delegate.severe(fmt(format, args));
    }

    @Override
    public void debug(String format, Object... args) {
        delegate.fine(fmt(format, args));
    }

    private static String fmt(String format, Object... args) {
        return args == null || args.length == 0 ? format : String.format(format, args);
    }
}
