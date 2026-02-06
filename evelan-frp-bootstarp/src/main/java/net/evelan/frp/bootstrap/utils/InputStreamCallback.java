package net.evelan.frp.bootstrap.utils;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamCallback<T> {
    T downInputStream(InputStream stream) throws IOException;
}
