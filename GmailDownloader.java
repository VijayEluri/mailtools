/*
 * GmailDownloader.java
 * (c) staktrace systems, 2009.
 * This code is BSD-licensed.
 */

import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class GmailDownloader {
    public static void main( String[] args ) throws Exception {
        int start = 1;
        int end = -1;
        if (args.length > 0) {
            try {
                start = Integer.parseInt( args[0] );
                if (args.length > 1) {
                    end = Integer.parseInt( args[1] );
                }
            } catch (NumberFormatException nfe) {
                System.out.println( "Usage: java GmailDownloader [start [end]]" );
                System.out.println();
                System.out.println( "   start and end are integers indicating the range of messages to" );
                System.out.println( "   be downloaded. For an initial download just leave them blank, or" );
                System.out.println( "   specify start as 1 and end as the number of messages to download." );
                System.out.println( "   To resume a previously-interrupted or partial download, specify" );
                System.out.println( "   the start value for where you left off. Optionally specify the" );
                System.out.println( "   end value to limit the number of messages downloaded." );
                System.out.println();
                return;
            }
        }

        Console console = System.console();
        String username = console.readLine( "Enter username: " );
        String password = new String( console.readPassword( "Enter password: " ) );

        SocketFactory sf = SSLSocketFactory.getDefault();
        Socket socket = sf.createSocket( "imap.gmail.com", 993 );
        InputStream in = socket.getInputStream();
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

        OutputStream out = socket.getOutputStream();
        PrintWriter pw = new PrintWriter( out );
        br.readLine();

        pw.print( "A LOGIN " + username + " " + password + "\r\n" );
        pw.flush();
        br.readLine();

        pw.print( "B SELECT \"[Gmail]/All Mail\"\r\n" );
        pw.flush();
        for (int i = 0; i < 3; i++) br.readLine();
        String numMsgs = br.readLine();
        for (int i = 0; i < 3; i++) br.readLine();
        if (end < 0) {
            StringTokenizer st = new StringTokenizer( numMsgs );
            st.nextToken();
            end = Integer.parseInt( st.nextToken() );
            System.out.println( "Found " + end + " messages" );
        }

        System.out.println( "Downloading messages from [" + start + "] to [" + end + "]" );
        for (int i = start; i <= end; i++) {
            System.out.println( "Downloading message " + i );
            pw.print( "ZZ FETCH " + i + " RFC822\r\n" );
            pw.flush();
            br.readLine();
            String fn = i + ".msg";
            while (fn.length() < 9) {
                fn = "0" + fn;
            }
            PrintWriter file = new PrintWriter( new File( fn ) );
            outer: while (true) {
                String s = br.readLine();
                while (s.endsWith( ")" )) {
                    String t = br.readLine();
                    if (t.startsWith( "ZZ OK" )) {
                        if (s.length() > 1) {
                            file.println( s.substring( 0, s.length() - 1 ) );
                        }
                        break outer;
                    }
                    file.println( s );
                    s = t;
                }
                file.println( s );
            }
            file.close();
        }

        pw.print( "C LOGOUT\r\n" );
        pw.flush();

        in.close();
        out.close();
        socket.close();
    }
}
