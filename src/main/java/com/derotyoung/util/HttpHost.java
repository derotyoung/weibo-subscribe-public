package com.derotyoung.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

public class HttpHost {

    /**
     * The default scheme is "http".
     */
    public static final String DEFAULT_SCHEME_NAME = "http";

    /**
     * The host to use.
     */
    protected String hostname;

    /**
     * The lowercase host, for {@link #equals} and {@link #hashCode}.
     */
    protected String lcHostname;

    /**
     * The port to use, defaults to -1 if not set.
     */
    protected int port;

    /**
     * The scheme (lowercased)
     */
    protected String schemeName;

    /**
     * Creates {@code HttpHost} instance with the given scheme, hostname and port.
     *
     * @param hostname the hostname (IP or DNS name)
     * @param port     the port number.
     *                 {@code -1} indicates the scheme default port.
     * @param scheme   the name of the scheme.
     *                 {@code null} indicates the
     *                 {@link #DEFAULT_SCHEME_NAME default scheme}
     */
    public HttpHost(final String hostname, final int port, final String scheme) {
        super();
        this.hostname = hostname;
        this.lcHostname = hostname.toLowerCase(Locale.ROOT);
        if (scheme != null) {
            this.schemeName = scheme.toLowerCase(Locale.ROOT);
        } else {
            this.schemeName = DEFAULT_SCHEME_NAME;
        }
        this.port = port;
    }

    private HttpHost() {
    }

    /**
     * Creates {@code HttpHost} instance from string. Text may not contain any blanks.
     *
     * @since 2022-11-06 15:28:58
     */
    public static HttpHost create(final String s) {
        String name = "HTTP Host";
        if (s == null) {
            throw new IllegalArgumentException(name + " may not be null");
        }
        if (s.length() == 0) {
            throw new IllegalArgumentException(name + " may not be empty");
        }
        if (StringUtils.containsWhitespace(s)) {
            throw new IllegalArgumentException(name + " may not contain blanks");
        }
        String text = s;
        String scheme = null;
        final int schemeIdx = text.indexOf("://");
        if (schemeIdx > 0) {
            scheme = text.substring(0, schemeIdx);
            text = text.substring(schemeIdx + 3);
        }
        int port = -1;
        final int portIdx = text.lastIndexOf(":");
        if (portIdx > 0) {
            try {
                port = Integer.parseInt(text.substring(portIdx + 1));
            } catch (final NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid HTTP host: " + text);
            }
            text = text.substring(0, portIdx);
        }
        return new HttpHost(text, port, scheme);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

}
