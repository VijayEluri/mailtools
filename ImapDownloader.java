/*
 * ImapDownloader.java
 * (c) staktrace systems, 2010.
 * This code is BSD-licensed.
 */

import java.io.*;
import java.util.*;

public class ImapDownloader extends ImapBase {
    public ImapDownloader( String host, int port ) {
        super( host, port );
    }

    public void download( int start, int end ) throws IOException {
        int messages = -1;
        String selectIdent = imapSend( "SELECT INBOX" );
        for (String response = readUntilDone( selectIdent ); response != null; response = readUntilDone( selectIdent )) {
            if (response.endsWith( " EXISTS" )) {
                StringTokenizer st = new StringTokenizer( response );
                st.nextToken();
                try {
                    messages = Integer.parseInt( st.nextToken() );
                } catch (NumberFormatException nfe) {
                }
            }
        }

        if (messages < 0) {
            System.err.println( "Unable to determine number of messages in mailbox. Exiting." );
            return;
        }
        System.out.println( "Found " + messages + " messages in mailbox." );

        if (start <= 0) {
            start = 1;
        } else if (start > messages) {
            System.err.println( "Error: message download start index is greater than number of messages." );
            return;
        }

        if (end <= 0) {
            end = messages;
        } else if (end > messages || end < start) {
            System.err.println( "Error: message download end index is invalid." );
            return;
        }

        System.out.println( "Downloading messages in range [" + start + ", " + end + "]" );

        for (int i = start; i <= end; i++) {
            System.out.println( "Downloading message " + i );
            downloadMessage( i );
        }
    }

    protected void downloadMessage( int message ) throws IOException {
        String fetchIdent = imapSend( "FETCH " + message + " RFC822" );
        String confirm = readUntilDone( fetchIdent );
        if (confirm.indexOf( message + " FETCH (RFC822" ) < 0) {
            System.err.println( "Error: could not locate fetch confirmation in [" + confirm + "]" );
            throw new IOException( "Protocol error" );
        }

        String filename = message + ".msg";
        while (filename.length() < 9) {
            filename = "0" + filename;
        }
        PrintWriter file = new PrintWriter( new File( filename ) );
        outer: while (true) {
            String msgLine = readUntilDone( fetchIdent );
            while (msgLine.endsWith( ")" )) {
                String nextLine = readUntilDone( fetchIdent );
                if (nextLine == null) {
                    if (msgLine.length() > 1) {
                        file.println( msgLine.substring( 0, msgLine.length() - 1 ) );
                    }
                    break outer;
                }
                file.println( msgLine );
                msgLine = nextLine;
            }
            file.println( msgLine );
        }
        file.close();
    }

    public static void usage( PrintStream out ) {
        out.println( "Usage: java ImapDownloader [-s <start>] [-e <end>] <host> [port]" );
        out.println( "       This will open a secure socket to the given host/port and prompt you for credentials." );
        out.println( "       The credentials will be used to authenticate the IMAP connection, and then all the" );
        out.println( "       messages in the specified range will be downloaded as RFC822-encoded messages and saved" );
        out.println( "       to plaintext files." );
        out.println();
    }

    public static void main( String[] args ) throws IOException {
        if (args.length < 1) {
            usage( System.out );
            return;
        }

        int startIx = -1;
        int endIx = -1;
        String host = null;
        int port = 993;

        try {
            for (int argIx = 0; argIx < args.length; argIx++) {
                if (args[ argIx ].equals( "-s" )) {
                    startIx = Integer.parseInt( args[ ++argIx ] );
                } else if (args[ argIx ].equals( "-e" )) {
                    endIx = Integer.parseInt( args[ ++argIx ] );
                } else {
                    host = args[ argIx++ ];
                    if (argIx < args.length) {
                        port = Integer.parseInt( args[ argIx ] );
                    }
                }
            }
            if (host == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            usage( System.out );
        }

        ImapDownloader downloader = new ImapDownloader( host, port );

        System.out.println( "Connecting..." );
        downloader.connect();

        if (downloader.logIn( System.console() )) {
            downloader.download( startIx, endIx );
            downloader.logOut();
        }

        downloader.disconnect();
    }
}
