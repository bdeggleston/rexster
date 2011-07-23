package com.tinkerpop.rexster.protocol;

import com.tinkerpop.pipes.SingleIterator;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.message.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;
import jline.ConsoleReader;
import jline.History;
import org.apache.commons.cli.*;

import javax.script.Bindings;
import java.io.*;
import java.nio.ByteBuffer;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class RexsterConsole {

    private RemoteRexsterSession session = null;
    private String host;
    private String language;
    private int port;
    private int timeout;

    private final PrintStream output = System.out;

    private static final String REXSTER_HISTORY = ".rexster_history";

    public RexsterConsole(String host, int port, String language, int timeout) throws Exception {

        this.output.println("        (l_(l");
        this.output.println("(_______( 0 0");
        this.output.println("(        (-Y-) <woof>");
        this.output.println("l l-----l l");
        this.output.println("l l,,   l l,,");

        this.host = host;
        this.port = port;
        this.language = language;
        this.timeout = timeout;

        this.output.print("opening session with Rexster [" + this.host + ":" + this.port + "] requesting [" + this.language + "]");
        this.session = new RemoteRexsterSession(this.host, this.port, this.timeout);
        this.session.open();
        this.output.println("--> ready");

        this.output.println("?h for help");

        this.primaryLoop();
    }

    public void primaryLoop() throws Exception {

        final ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setUseHistory(true);

        try {
            History history = new History();
            history.setHistoryFile(new File(REXSTER_HISTORY));
            reader.setHistory(history);
        } catch (IOException e) {
            System.err.println("Could not find history file");
        }

        String line = "";
        this.output.println();

        while (line != null) {

            try {
                line = "";
                boolean submit = false;
                boolean newline = false;
                while (!submit) {
                    if (newline)
                        line = line + "\n" + reader.readLine(RexsterConsole.makeSpace(this.getPrompt().length()));
                    else
                        line = line + "\n" + reader.readLine(this.getPrompt());
                    if (line.endsWith(" .")) {
                        newline = true;
                        line = line.substring(0, line.length() - 2);
                    } else {
                        line = line.trim();
                        submit = true;
                    }
                }

                if (line.isEmpty())
                    continue;
                if (line.equals(Tokens.REXSTER_CONSOLE_QUIT)) {
                    this.output.print("closing session with Rexster [" + this.host + ":" + this.port + "]");
                    if (this.session != null) {
                        this.session.close();
                        this.session = null;
                    }
                    this.output.println("--> done");
                    return;
                } else if (line.equals(Tokens.REXSTER_CONSOLE_HELP)) {
                    this.printHelp();
                } else if (line.equals(Tokens.REXSTER_CONSOLE_RESET)) {
                    this.output.print("resetting session with Rexster [" + this.host + ":" + this.port + "]");
                    if (this.session != null) {
                        this.session.reset();
                    } else {
                        this.session = new RemoteRexsterSession(this.host, this.port, this.timeout);
                    }
                    this.output.println("--> done");
                } else if (line.equals(Tokens.REXSTER_CONSOLE_LANGUAGES)) {
                    this.printAvailableLanguages();
                } else if (line.startsWith(Tokens.REXSTER_CONSOLE_LANGUAGE)) {
                    String langToChangeTo = line.substring(1);
                    if (langToChangeTo == null || langToChangeTo.isEmpty()) {
                        this.output.println("specify a language on Rexster ?<language-name>");
                        this.printAvailableLanguages();
                    } else if (this.session.isAvailableLanguage(langToChangeTo)) {
                        this.language = langToChangeTo;
                    } else {
                        this.output.println("not a valid language on Rexster: [" + langToChangeTo + "].");
                        this.printAvailableLanguages();
                    }
                } else {
                    Object result = eval(line, this.language, this.session);
                    Iterator itty;
                    if (result instanceof Iterator) {
                        itty = (Iterator) result;
                    } else if (result instanceof Iterable) {
                        itty = ((Iterable) result).iterator();
                    } else if (result instanceof Map) {
                        itty = ((Map) result).entrySet().iterator();
                    } else {
                        itty = new SingleIterator<Object>(result);
                    }

                    while (itty.hasNext()) {
                        this.output.println("==>" + itty.next());
                    }
                }

            } catch (Exception e) {
                this.output.println("Evaluation error: " + e.getMessage());
            }
        }
    }

    private void printAvailableLanguages() {
        this.output.println("-= Available Languages =-");

        Iterator<String> languages = this.session.getAvailableLanguages();
        while(languages.hasNext()) {
            this.output.println("?" + languages.next());
        }
    }

    public void printHelp() {
        this.output.println("-= Console Specific =-");
        this.output.println("?<language-name>: jump to engine");
        this.output.println(Tokens.REXSTER_CONSOLE_LANGUAGES + ": list of available languages on Rexster");
        this.output.println(Tokens.REXSTER_CONSOLE_RESET + ": reset the rexster session");
        this.output.println(Tokens.REXSTER_CONSOLE_QUIT + ": quit");
        this.output.println(Tokens.REXSTER_CONSOLE_HELP + ": displays this message");

        this.output.println("");
        this.output.println("-= Rexster Context =-");
        this.output.println("rexster.getGraph(graphName) - gets a Graph instance");
        this.output.println("   :graphName - [String] - the name of a graph configured within Rexster");
        this.output.println("rexster.getGraphNames() - gets the set of graph names configured within Rexster");
        this.output.println("rexster.getVersion() - gets the version of Rexster server");
        this.output.println("");
    }

    public void printBindings(final Bindings bindings) {
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            this.output.println(entry);
        }
    }

    public String getPrompt() {
        return "rexster[" + this.language +  "]> ";
    }

    public static String makeSpace(int number) {
        String space = new String();
        for (int i = 0; i < number; i++) {
            space = space + " ";
        }
        return space;
    }

    private static Object eval(String script, String scriptEngineName, RemoteRexsterSession session) {

        Object returnValue = null;

        try {
            session.open();

            // pass in some dummy rexster bindings...not really fully working quite right for scriptengine usage
            final RexProMessage scriptMessage = new ScriptRequestMessage(
                    session.getSessionKey(), scriptEngineName, new RexsterBindings(), script);

            final RexProMessage resultMessage = RexPro.sendMessage(
                    session.getRexProHost(), session.getRexProPort(), scriptMessage);

            ArrayList<String> lines = new ArrayList<String>();
            try {
                ConsoleScriptResponseMessage responseMessage = new ConsoleScriptResponseMessage(resultMessage);
                ByteBuffer bb = ByteBuffer.wrap(responseMessage.getBody());

                // navigate to the start of the results...bindings are attached if there is no error present
                int lengthOfBindings = bb.getInt();
                bb.position(lengthOfBindings + 4);

                while (bb.hasRemaining()) {
                    int segmentLength = bb.getInt();
                    byte[] resultObjectBytes = new byte[segmentLength];
                    bb.get(resultObjectBytes);

                    lines.add(new String(resultObjectBytes));
                }

            } catch (IllegalArgumentException iae) {
                ErrorResponseMessage errorMessage = new ErrorResponseMessage(resultMessage);
                lines.add(errorMessage.getErrorMessage());
            }

            returnValue = lines.iterator();

            if (lines.size() == 1) {
                returnValue = lines.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

        return returnValue;
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option hostName = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("the rexster server to connect to")
                .withLongOpt("rexsterhost")
                .create("rh");

        Option port = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("the port of the rexster server that is serving rexpro")
                .withLongOpt("rexsterport")
                .create("rp");

        Option language = OptionBuilder.withArgName("language")
                .hasArg()
                .withDescription("the script engine language to use by default")
                .withLongOpt("language")
                .create("l");

        Option timeout = OptionBuilder.withArgName("seconds")
                .hasArg()
                .withDescription("time allowed when waiting for results from server (default 100 seconds)")
                .withLongOpt("timeout")
                .create("t");

        Options options = new Options();
        options.addOption(help);
        options.addOption(hostName);
        options.addOption(port);
        options.addOption(language);
        options.addOption(timeout);

        return options;
    }

    private static CommandLine getCliInput(final String[] args) throws Exception {
        Options options = getCliOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            throw new Exception("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster console", options);
            System.exit(0);
        }

        return line;
    }

    public static void main(String[] args) throws Exception {

        CommandLine line = getCliInput(args);

        String host = "localhost";
        int port = 8185;
        String language = "gremlin";
        int timeout = RexPro.DEFAULT_TIMEOUT_SECONDS;

        if (line.hasOption("rexsterhost")) {
            host = line.getOptionValue("rexsterhost");
        }

        if (line.hasOption("rexsterport")) {
            String portString = line.getOptionValue("rexsterport");
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                System.out.println("the rexsterport parameter must be an integer value. Defaulting to: [" + port + "]");
            }
        }

        if (line.hasOption("language")) {
            language = line.getOptionValue("language");
        }

        if (line.hasOption("timeout")) {
            String timeoutString = line.getOptionValue("timeout");
            try {
                port = Integer.parseInt(timeoutString);
            } catch (NumberFormatException nfe) {
                System.out.println("the timeout parameter must be an integer value. Defaulting to: " + timeout);
            }
        }

        new RexsterConsole(host, port, language, timeout);
    }
}
