import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;
import java.util.stream.Collectors.*;


public class SimpleMailer
{
    public static class SimpleMailerError extends Exception
    {
        public SimpleMailerError(String msg_)
        {
            super(msg_);
        }
    }


    private static final String EMAIL_SERVER_NAME_DEFAULT = "localhost";
    private static final int EMAIL_SERVER_PORT_DEFAULT = 25;

    private static final Charset CHARSET_DEFAULT = Charset.forName("UTF-8");

    private static final Map<String, String> SERVER_RESPONSES = Stream.of(new String[][] { { "Start", "220 " },
                                                                                           { "OK", "250 " },
                                                                                           { "Data", "354 " },
                                                                                           { "End", "221 " } }
                                                                         ).collect(Collectors.toMap(u_ -> u_[0], u_ -> u_[1]));
    private static final Map<String, String> CLIENT_COMMANDS = Stream.of(new String[][] { { "Start", "HELO {0}" },
                                                                                          { "Sender", "MAIL FROM:<{0}>" },
                                                                                          { "Recipient", "RCPT TO:<{0}>" },
                                                                                          { "Data", "DATA" },
                                                                                          { "Body", "{0}" },
                                                                                          { "Quit", "QUIT"} }
                                                                        ).collect(Collectors.toMap(u_ -> u_[0], u_ -> u_[1]));
    private static final Map<String, String> EMAIL_COMPONENTS = Stream.of(new String[][] { { "From", "From: {From}" },
                                                                                           { "To", "To: {To}" },
                                                                                           { "Date", "Date: {Date}" },
                                                                                           { "Subject", "Subject: {Subject}" },
                                                                                           { "Separator", "" },
                                                                                           { "Body", "{Body}" },
                                                                                           { "EOM", "\r\n.\r\n" } }
                                                                         ).collect(Collectors.toMap(u_ -> u_[0], u_ -> u_[1]));


    public static void Send(String[] recipients_,
                            String subject_,
                            String body_) throws IOException,
                                                 UnknownHostException,
                                                 SimpleMailerError
    {
        Send(EMAIL_SERVER_NAME_DEFAULT,
             EMAIL_SERVER_PORT_DEFAULT,
             System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName(),
             recipients_,
             subject_,
             body_);
    }

    public static void Send(String sender_,
                            String[] recipients_,
                            String subject_,
                            String body_) throws IOException,
                                                 UnknownHostException,
                                                 SimpleMailerError
    {
        Send(EMAIL_SERVER_NAME_DEFAULT,
             EMAIL_SERVER_PORT_DEFAULT,
             sender_,
             recipients_,
             subject_,
             body_);
    }

    public static void Send(String server_name_,
                            int server_port_,
                            String sender_,
                            String[] recipients_,
                            String subject_,
                            String body_) throws IOException,
                                                 UnknownHostException,
                                                 SimpleMailerError
    {
        try (final Socket SOCKET = new Socket(server_name_, server_port_);
             final InputStreamReader INSTREAM = new InputStreamReader(SOCKET.getInputStream());
             final BufferedReader READER = new BufferedReader(INSTREAM);
             final OutputStreamWriter WRITER = new OutputStreamWriter(SOCKET.getOutputStream(),
                                                                      CHARSET_DEFAULT);
             final PrintWriter PRINTER = new PrintWriter(WRITER, true))
        {
            Communicate(PRINTER,
                        READER,
                        null,
                        "Start");

            Communicate(PRINTER,
                        READER,
                        CLIENT_COMMANDS.get("Start")
                                       .replace("{0}", server_name_),
                        "OK");

            Communicate(PRINTER,
                        READER,
                        CLIENT_COMMANDS.get("Sender")
                                       .replace("{0}", sender_),
                        "OK");

            for (String RECIPIENT : recipients_)
            {
                Communicate(PRINTER,
                            READER,
                            CLIENT_COMMANDS.get("Recipient")
                                           .replace("{0}", RECIPIENT),
                            "OK");
            }

            Communicate(PRINTER,
                        READER,
                        CLIENT_COMMANDS.get("Data"),
                        "Data");

            Communicate(PRINTER,
                        READER,
                        CLIENT_COMMANDS.get("Body")
                                       .replace("{0}", Message(sender_,
                                                               recipients_,
                                                               subject_,
                                                               body_)),
                        "OK");

            Communicate(PRINTER,
                        READER,
                        CLIENT_COMMANDS.get("Quit"),
                        "End");
        }
    }


    private static void Communicate(PrintWriter writer_,
                                    BufferedReader reader_,
                                    String call_,
                                    String response_) throws IOException,
                                                             SimpleMailerError
    {
        if (call_ == null)
        {
            // Empty
        }
        else
        {
            writer_.println(call_);
        }

        final String RESPONSE = reader_.readLine();

        if (!RESPONSE.startsWith(SERVER_RESPONSES.get(response_)))
        {
            throw new SimpleMailerError(RESPONSE);
        }
    }


    private static String Message(String sender_,
                                  String[] recipients_,
                                  String subject_,
                                  String body_)
    {
        final StringBuilder RESULT = new StringBuilder();

        final String SEPARATOR = System.lineSeparator();

        RESULT.append(EMAIL_COMPONENTS.get("From")
                                      .replace("{From}", sender_))
              .append(SEPARATOR);
              
        for (String RECIPIENT : recipients_)
        {
            RESULT.append(EMAIL_COMPONENTS.get("To")
                                          .replace("{To}", RECIPIENT))
                  .append(SEPARATOR);
        }

        RESULT.append(EMAIL_COMPONENTS.get("Date")
                                      .replace("{Date}",
                                               ZonedDateTime.now()
                                                            .format(DateTimeFormatter.RFC_1123_DATE_TIME)))
              .append(SEPARATOR);

        RESULT.append(EMAIL_COMPONENTS.get("Subject")
                                      .replace("{Subject}", subject_))
              .append(SEPARATOR);

        RESULT.append(EMAIL_COMPONENTS.get("Body")
                                      .replace("{Body}", body_))
              .append(SEPARATOR);

        RESULT.append(EMAIL_COMPONENTS.get("EOM"));

        return RESULT.toString();
    }
}