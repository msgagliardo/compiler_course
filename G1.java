public class G1 {

    public static void main(String[] args) {

        // check here if number of arguments is correct
        boolean debug = true;
        if (args.length != 1) {
            System.err.println("Wrong number of command line arguments");
            System.exit(1);
        }

        G1TokenMgr tm = new G1TokenMgr(args[0], debug);
        G1Parser parser = new G1Parser(tm);

        try {
            // parse regular expression
            parser.parse();
        }catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

interface G1Constants {

    int EORE = 0;       // end of regular expression
    int CHAR = 1;
    int PERIOD = 2;     // period matches any character
    int LEFTPAREN = 3;
    int RIGHTPAREN = 4;
    int OR = 5;
    int STAR = 6;
    int ERROR = 7;

    int CONCAT = 8;     // no corresponding token

    String[] tokenImage = {
        "<EORE>",
        "<CHAR>",
        "\".\"",
        "\"(\"",
        "\")\"",
        "\"|\"",
        "\"*\"",
        "<ERROR>"
    };
}
class G1TokenMgr implements G1Constants {
    // contains constructor and getNexttoken() method
    private char currentChar;
    private int currentColumnNumber;
    private String regex;
    private Token token;
    private boolean debug;
    private StringBuffer buffer;

    public G1TokenMgr(String regex, boolean debug) {
        this.regex = regex;
        this.debug = debug;
        currentChar = '\n';
        currentColumnNumber = 0;
        buffer = new StringBuffer();
    }
    public Token getNextToken() {

        if (regex.charAt(0) == '"') {
            if (regex.charAt(regex.length() - 1) == '"') {
                do {
                    currentColumnNumber++;
                    buffer.append(regex.charAt(currentColumnNumber));
                }while (currentColumnNumber < (regex.length() - 1));
                regex = buffer.toString();
                buffer.setLength(0);
                currentColumnNumber = 0;
            }else {
                throw new RuntimeException("Regex missing ending quote");
            }
        }
        getNextChar();

        token = new Token();
        token.next = null;

        token.beginColumn = currentColumnNumber;

        if (currentChar == EORE) {
            token.image = "<EORE>";
            token.kind = EORE;
        }else {

            switch(currentChar) {
                case '.':
                    token.kind = PERIOD;
                    break;
                case '(':
                    token.kind = LEFTPAREN;
                    break;
                case ')':
                    token.kind = RIGHTPAREN;
                    break;
                case '|':
                    token.kind = OR;
                    break;
                case '*':
                    token.kind = STAR;
                    break;
                case '\\':
                    getNextChar();
                    token.kind = CHAR;
                    break;
                default:
                    token.kind = CHAR;
                    break;
            }
            token.image = Character.toString(currentChar);

            //getNextChar();
        }
        if (debug)
            System.out.printf(
                "; kd=%3d bC=%3d im=%s%n",
                token.kind, token.beginColumn, token.image);

        return token;
    }
    private void getNextChar() {

        if (currentChar == EORE)
            return;

        if (currentColumnNumber == regex.length()) {
            currentChar = EORE;
            return;
        }

        currentChar = regex.charAt(currentColumnNumber++);
    }
}
class G1Parser implements G1Constants {
    // contains constructor, parse(), advance(), and consume() methods
    // also contains the methods for the recursive descent parser based
    // on the grammar in figure 18.3
    private G1TokenMgr tm;
    private Token currentToken;
    private Token previousToken;

    public G1Parser(G1TokenMgr tm) {
        this.tm = tm;
        // prime currentToken with first token
        currentToken = tm.getNextToken();
        previousToken = null;
    }
    // construct and return an exception that contains a message
    // consisting of the image of the current token, its location
    // and the expected tokens.
    private RuntimeException genEx(String errorMessage) {
        return new RuntimeException("Encountered \"" +
            currentToken.image + "\" in column " +
            currentToken.beginColumn + "." +
            System.getProperty("line.separator") +
            errorMessage);
    }
    // advance currentToken to next token
    public void advance() {
        previousToken = currentToken;

        if (currentToken.next != null)
            currentToken = currentToken.next;
        else
            currentToken = currentToken.next = tm.getNextToken();
    }
    private void consume(int expected) {
        if (currentToken.kind == expected)
            advance();
        else
            throw genEx("Expecting " + tokenImage[expected]);
    }
    public void parse() {
        expr();              // expr is start symbol for grammar
    }
    private void expr() {
        term();
        termList();
    }
    private void term() {
        factor();
        factorList();
    }
    private void factor() {

        switch(currentToken.kind) {
            case CHAR:
                consume(CHAR);
                factorTail();
                break;
            case PERIOD:
                consume(PERIOD);
                factorTail();
                break;
            case LEFTPAREN:
                consume(LEFTPAREN);
                expr();
                consume(RIGHTPAREN);
                factorTail();
                break;
            default:
                throw genEx("Expecting <CHAR>, <PERIOD>, or \"(\"");
        }
    }
    private void factorTail() {

        switch(currentToken.kind) {
            case STAR:
                consume(STAR);
                factorTail();
                break;
            case CHAR:
            case PERIOD:
            case LEFTPAREN:
            case OR:
            case RIGHTPAREN:
            case EORE:
                ;
                break;
            default:
                throw genEx("Expecting \"*\", <CHAR>, \".\", \"(\", \"|\", "
                    + "\")\", or <EORE>");
        }
    }
    private void factorList() {

        switch(currentToken.kind) {
            case CHAR:
            case PERIOD:
            case LEFTPAREN:
                factor();
                factorList();
                break;
            case OR:
            case RIGHTPAREN:
            case EORE:
                ;
                break;
            default:
                throw genEx("Expecting <CHAR>, \".\", \"(\", \"|\", "
                    + "\")\", or <EORE>");
        }
    }
    private void termList() {

        switch(currentToken.kind) {
            case OR:
                consume(OR);
                term();
                termList();
                break;
            case RIGHTPAREN:
            case EORE:
                ;
                break;
            default:
                throw genEx("Expecting \"|\", \")\", or <EORE>");
        }
    }
}
