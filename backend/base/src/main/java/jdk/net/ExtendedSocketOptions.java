package jdk.net;

import java.net.SocketOption;

/**
 * Fallback for the JDK class of the same name, see {@link jdk.net package documentation}. Only declares the constants
 * Apache HttpComponents refers to; they are never used since {@link Sockets#supportedOptions(Class)} is empty.
 */
public final class ExtendedSocketOptions {

    public static final SocketOption<Integer> TCP_KEEPIDLE = new StubOption("TCP_KEEPIDLE");
    public static final SocketOption<Integer> TCP_KEEPINTERVAL = new StubOption("TCP_KEEPINTERVAL");
    public static final SocketOption<Integer> TCP_KEEPCOUNT = new StubOption("TCP_KEEPCOUNT");

    private ExtendedSocketOptions() {
        // static constants only
    }

    private static final class StubOption implements SocketOption<Integer> {
        private final String name;

        StubOption(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<Integer> type() {
            return Integer.class;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
