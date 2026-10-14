package jdk.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.util.Collections;
import java.util.Set;

/**
 * Fallback for the JDK class of the same name, see {@link jdk.net package documentation}. Supports no extended options.
 */
public final class Sockets {

    private Sockets() {
        // static methods only
    }

    /** No extended socket options are supported by this fallback. */
    public static Set<SocketOption<?>> supportedOptions(Class<?> socketType) {
        return Collections.emptySet();
    }

    public static <T> void setOption(Socket socket, SocketOption<T> name, T value) {
        throw new UnsupportedOperationException("Socket option " + name + " is not supported");
    }

    public static <T> void setOption(ServerSocket socket, SocketOption<T> name, T value) {
        throw new UnsupportedOperationException("Socket option " + name + " is not supported");
    }

}
