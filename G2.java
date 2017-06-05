public class G2 {

    public static void main(String[] args) {

        // check here if number of arguments is correct
        boolean debug = false;
        if (args.length != 1) {
            System.err.println("Wrong number of command line arguments");
            System.exit(1);
        }
        G2TokenMgr tm = new G2TokenMgr(args[0], debug);
        G2CodeGen cg = new G2CodeGen();
        G2Parser parser = new G2Parser(tm, cg);

        NFAState s = null;
        try {
            // parse regular expression
            s = parser.parse();
        }catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        if (s != null)
            NFAState.displayNFA(s);

    }
}

interface G2Constants {

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
class G2TokenMgr implements G2Constants {
    // contains constructor and getNexttoken() method
    private char currentChar;
    private int index;
    private int currentColumnNumber;
    private String regex;
    private Token token;
    private boolean debug;
    private StringBuffer buffer;

    public G2TokenMgr(String regex, boolean debug) {
        this.regex = regex;
        this.debug = debug;
        currentChar = '\n';
        index = 0;
        currentColumnNumber = 0;
        buffer = new StringBuffer();
    }
    public Token getNextToken() {

        /*if (regex.charAt(0) == '"') {
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
        */
        getNextChar();

        token = new Token();
        token.next = null;

        token.beginColumn = currentColumnNumber;

        if (currentChar == EORE) {
            token.image = "<EORE>";
            token.kind = EORE;
            token.beginColumn++;
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
                    if (index < regex.length())
                        currentChar = regex.charAt(index++);
                    else {
                        System.err.println("Trailing backslash");
                        System.exit(1);
                    }
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

        if (index == regex.length()) {
            currentChar = EORE;
            return;
        }

        currentChar = regex.charAt(index++);
        currentColumnNumber++;
    }
}
class G2Parser implements G2Constants {
    // contains constructor, parse(), advance(), and consume() methods
    // also contains the methods for the recursive descent parser based
    // on the grammar in figure 18.3
    private G2TokenMgr tm;
    private G2CodeGen cg;
    private Token currentToken;
    private Token previousToken;

    public G2Parser(G2TokenMgr tm, G2CodeGen cg) {
        this.tm = tm;
        this.cg = cg;
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
    public NFAState parse() {
        NFAState p;

        p = expr();              // expr is start symbol for grammar
        return p;
    }
    private NFAState expr() {
        NFAState p;

        p = term();
        p = termList(p);
        return p;
    }
    private NFAState term() {
        NFAState p;

        p = factor();
        p = factorList(p);
        return p;
    }
    private NFAState factor() {
        NFAState p;
        Token t;

        switch(currentToken.kind) {
            case CHAR:
                t = currentToken;
                consume(CHAR);
                p = cg.make(CHAR, t);
                p = factorTail(p);
                break;
            case PERIOD:
                t = currentToken;
                consume(PERIOD);
                p = cg.make(PERIOD, t);
                p = factorTail(p);
                break;
            case LEFTPAREN:
                consume(LEFTPAREN);
                p = expr();
                consume(RIGHTPAREN);
                p = factorTail(p);
                break;
            default:
                throw genEx("Expecting <CHAR>, <PERIOD>, or \"(\"");
        }
        return p;
    }
    private NFAState factorTail(NFAState p) {

        switch(currentToken.kind) {
            case STAR:
                consume(STAR);
                p = cg.make(STAR, p);
                p = factorTail(p);
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
        return p;
    }
    private NFAState factorList(NFAState p) {
        NFAState q;

        switch(currentToken.kind) {
            case CHAR:
            case PERIOD:
            case LEFTPAREN:
                q = factor();
                p = cg.make(CONCAT, p, q);
                p = factorList(p);          // pass new NFA to factorList()
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
        return p;
    }
    private NFAState termList(NFAState p) {
        NFAState q;

        switch(currentToken.kind) {
            case OR:
                consume(OR);
                q = term();
                p = cg.make(OR, p, q);
                p = termList(p);            // pass new NFA to termList()
                break;
            case RIGHTPAREN:
            case EORE:
                ;
                break;
            default:
                throw genEx("Expecting \"|\", \")\", or <EORE>");
        }
        return p;
    }
}
class G2CodeGen implements G2Constants {

    public NFAState make(int op, NFAState p, NFAState q) {
        // s  is new start state; a is new accept state
        NFAState s, a;

        switch(op) {
            case OR:
                s = new NFAState();
                a = new NFAState();
                s.arrow1 = p;        // make s point to p and q
                s.arrow2 = q;
                // make accept states of p and q NFAs point to a
                p.acceptState.arrow1 = a;
                q.acceptState.arrow1 = a;
                s.acceptState = a;   // make a the accept state
                return s;
            case CONCAT:
                p.acceptState.arrow2 = q;
                p.acceptState = q.acceptState;
                return p;
            default:
                throw new RuntimeException("Bad call of make");
        }
    }
    //----------------------------------------
    public NFAState make(int op, Token t) {
        // s is new start state; a is new acccept state
        NFAState s, a;

        switch(op) {
            case CHAR:
                s = new NFAState();
                a = new NFAState();
                s.arrow1 = a;        // make s point to a
                s.label1 = t.image.charAt(0);
                s.acceptState = a;   // make a the accept state
                return s;
            case PERIOD:
                s = new NFAState();
                a = new NFAState();
                s.arrow1 = a;
                s.label1 = t.image.charAt(0);
                s.acceptState = a;
                return s;
            default:
                throw new RuntimeException("Bad call of maker");
        }
    }
    //----------------------------------------
    public NFAState make(int op, NFAState p) {
        // s is new start state; a is new accept state
        NFAState s, a;

        switch(op) {
            case STAR:
                s = new NFAState();
                a = new NFAState();
                s.arrow2 = a;
                p.acceptState.arrow2 = s;
                s.arrow1 = p;
                s.acceptState = a;
                return s;
            default:
                throw new RuntimeException("Bad call of make");
        }
    }
}
