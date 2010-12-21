/*
 * ImapBase.java
 * (c) staktrace systems, 2010.
 * This code is BSD-licensed.
 */

import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public abstract class ImapBase {
    private final String _host;
    private final int _port;

    private Socket _socket;
    private InputStream _socketIn;
    private BufferedReader _imapServer;
    private OutputStream _socketOut;
    private PrintWriter _server;
    private int _commandCounter;

    public ImapBase( String host, int port ) {
        _host = host;
        _port = port;
    }

    protected void connect() throws IOException {
        SocketFactory sf = SSLSocketFactory.getDefault();
        _socket = sf.createSocket( _host, _port );
        _socketIn = _socket.getInputStream();
        _imapServer = new BufferedReader( new InputStreamReader( _socketIn ) );
        _socketOut = _socket.getOutputStream();
        _server = new PrintWriter( _socketOut );
        _commandCounter = 1;
    }

    protected void disconnect() throws IOException {
        _socketIn.close();
        _socketOut.close();
        _socket.close();

        _server = null;
        _socketOut = null;
        _imapServer = null;
        _socketIn = null;
        _socket = null;
    }

    protected void logIn( String username, String password ) throws IOException {
        imapDump( "LOGIN " + username + " " + password );
    }

    protected void logOut() throws IOException {
        imapDump( "LOGOUT" );
    }

    protected void imapDump( String command ) throws IOException {
        String ident = "A" + (_commandCounter++);
        _server.print( ident + " " + command + "\r\n" );
        _server.flush();

        String s;
        for (s = _imapServer.readLine(); ! s.startsWith( ident ); s = _imapServer.readLine()) {
            System.out.println( s );
        }
        System.out.println( '*' + s.substring( ident.length() ) );
    }
}
