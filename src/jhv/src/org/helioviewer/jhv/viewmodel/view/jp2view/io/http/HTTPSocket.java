package org.helioviewer.jhv.viewmodel.view.jp2view.io.http;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;

/**
 *
 * The class <code>HTTPSocket</code> is a simple implementation for read/write
 * HTTP messages. In this version are only supported to send requests and to
 * receive responses.
 *
 * @author Juan Pablo Garcia Ortiz
 * @see java.net.Socket
 * @see HTTPResponse
 * @see HTTPRequest
 * @version 0.1
 *
 */
public class HTTPSocket extends Socket {

    /** The last used port */
    private int lastUsedPort = 0;

    /** The last used host */
    private String lastUsedHost = null;

    /** The default port for the HTTP socket */
    public static final int PORT = 80;

    /**
     * Connects to the specified host via the supplied URI.
     *
     * @param _uri
     * @throws IOException
     */
    public Object connect(URI _uri) throws IOException {
        lastUsedPort = _uri.getPort() <= 0 ? PORT : _uri.getPort();
        lastUsedHost = _uri.getHost();
        super.setSoTimeout(10000);
        super.setKeepAlive(true);
        super.setTcpNoDelay(true);
        super.connect(new InetSocketAddress(lastUsedHost, lastUsedPort), 10000);

        return null;
    }

    /**
     * Reconnects to the last used host, and using the last used port.
     *
     * @throws java.io.IOException
     */
    public void reconnect() throws IOException {
        super.connect(new InetSocketAddress(lastUsedHost, lastUsedPort), 10000);
    }

    /**
     * Sends a HTTP message. Only HTTP requests supported
     *
     * @param _msg
     *            A <code>HTTPMessage</code> object with the message.
     * @throws java.io.IOException
     */
    public void send(HTTPMessage _msg) throws IOException {
        if (!isConnected())
            reconnect();

        StringBuilder str = new StringBuilder();

        if (_msg.isRequest()) {
            HTTPRequest req = (HTTPRequest) _msg;
            String msgBody = req.getMessageBody();

            // Adds the URI line
            str.append(req.getMethod()).append(' ').append(req.getURI()).append(' ').append(HTTPConstants.versionText).append(HTTPConstants.CRLF);

            // Sets the content length header if it's a POST
            if (req.getMethod() == HTTPRequest.Method.POST)
                req.setHeader(HTTPHeaderKey.CONTENT_LENGTH.toString(), Integer.toString(msgBody.getBytes("UTF-8").length));

            // Adds the headers
            for (String key : req.getHeaders()) {
                str.append(key).append(": ").append(req.getHeader(key)).append(HTTPConstants.CRLF);
            }
            str.append(HTTPConstants.CRLF);

            // Adds the message body if it's a POST
            if (req.getMethod() == HTTPRequest.Method.POST)
                str.append(msgBody);

            // Writes the result to the output stream
            getOutputStream().write(str.toString().getBytes("UTF-8"));
        } else {
            throw new ProtocolException("Responses sending not yet supported!");
        }
    }

    /**
     * Receives a HTTP message from the socket. Currently it is only supported
     * to receive HTTP responses.
     *
     * @return A new <code>HTTPMessage</code> object with the message read or
     *         <code>null</code> if the end of stream was reached.
     * @throws java.io.IOException
     */
    public HTTPMessage receive() throws IOException {
        InputStream lineInput = getInputStream();

        String line = readLine(lineInput);
        if (line == null)
            return null;

        String parts[] = line.split(" ", 3);
        if (parts.length != 3) {
            throw new ProtocolException("Invalid HTTP message: " + line);
        }

        if (parts[0].startsWith("HTTP/")) {
            // Parses HTTP version
            double ver;
            try {
                ver = Double.parseDouble(parts[0].substring(5));
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid HTTP version format");
            }
            if (ver < 1 || ver > HTTPConstants.version)
                throw new ProtocolException("HTTP version not supported");

            // Parses status code
            int code;
            try {
                code = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                throw new ProtocolException("Invalid HTTP status code format");
            }

            // Instantiates new HTTPResponse
            HTTPResponse res = new HTTPResponse(code, parts[2]);

            // Parses HTTP headers
            for (;;) {
                line = readLine(lineInput);
                if (line == null)
                    throw new EOFException("End of stream reached before end of HTTP message");
                else if (line.length() <= 0)
                    break;

                parts = line.split(": ", 2);
                if (parts.length != 2)
                    throw new ProtocolException("Invalid HTTP header format");

                res.setHeader(parts[0], parts[1]);
            }
            return res;
        } else {
            throw new ProtocolException("Requests receiving not yet supported!");
        }
    }

    private static byte[] readRawLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            if (ch == '\n') {
                break;
            }
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }

    private static String readLine(InputStream inputStream) throws IOException {
        byte[] rawdata = readRawLine(inputStream);
        if (rawdata == null) {
            return null;
        }
        int len = rawdata.length;
        int offset = 0;
        if (len > 0 && rawdata[len - 1] == '\n') {
            offset++;
            if (len > 1 && rawdata[len - 2] == '\r') {
                offset++;
            }
        }
        return new String(rawdata, 0, len - offset, "US-ASCII");
    }

    /** Returns the lastUsedPort */
    @Override
    public int getPort() {
        return lastUsedPort;
    }

    /** Returns the lastUsedHost */
    public String getHost() {
        return lastUsedHost;
    }

}
