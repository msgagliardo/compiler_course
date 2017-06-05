/*Marc Gagliardo 											12/9/16
  CPS565.01 Compiler Design 						  Final Project

  *****************************************************************
*/  


import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class G3 {

    public static void main(String[] args) {

        // check here if number of arguments is correct
        boolean debug = false;
        if (args.length != 2) {
            System.err.println("Wrong number of command line arguments");
            System.exit(1);
        }
        G3TokenMgr tm = new G3TokenMgr(args[0], debug);
        G3CodeGen cg = new G3CodeGen();
        G3Parser parser = new G3Parser(tm, cg);

        NFAState start = null;
        try {
            // parse regular expression
            Scanner inFile = new Scanner(new File(args[1]));
            start = parser.parse();
            G3Matcher m = new G3Matcher(inFile, start);
            m.match();
            inFile.close();
        }catch (RuntimeException | IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        if (start != null)
            NFAState.displayNFA(start);

    }
}

interface G3Constants {

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
class G3TokenMgr implements G3Constants {
    // contains constructor and getNexttoken() method
    private char currentChar;
    private int index;
    private int currentColumnNumber;
    private String regex;
    private Token token;
    private boolean debug;
    private StringBuffer buffer;

    public G3TokenMgr(String regex, boolean debug) {
        this.regex = regex;
        this.debug = debug;
        currentChar = '\n';
        index = 0;
        currentColumnNumber = 0;
        buffer = new StringBuffer();
    }
    public Token getNextToken() {

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
class G3Parser implements G3Constants {
    // contains constructor, parse(), advance(), and consume() methods
    // also contains the methods for the recursive descent parser based
    // on the grammar in figure 18.3
    private G3TokenMgr tm;
    private G3CodeGen cg;
    private Token currentToken;
    private Token previousToken;

    public G3Parser(G3TokenMgr tm, G3CodeGen cg) {
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
class G3CodeGen implements G3Constants {

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
class G3Matcher implements G3Constants {

    private ArrayList<NFAState> currentStates;
    private NFAState startState;
    private Scanner inFile;

    public G3Matcher(Scanner inFile, NFAState startState) {
        currentStates = new ArrayList<NFAState>();
        this.startState = startState;
        this.inFile = inFile;
    }

    private boolean lambdaClosure() {
        boolean gotAccept = false;

        for (int i = 0; i < currentStates.size(); i++) {
            if (currentStates.get(i) == startState.acceptState) {
                gotAccept = true;
            }
            if (currentStates.get(i).arrow1 != null &&
                currentStates.get(i).label1 == 0 &&
                !currentStates.contains(currentStates.get(i).arrow1)) {

                    currentStates.add(currentStates.get(i).arrow1);
            }
            if (currentStates.get(i).arrow2 != null &&
                !currentStates.contains(currentStates.get(i).arrow2)) {

                    currentStates.add(currentStates.get(i).arrow2);
            }
        }
        return gotAccept;
    }
    private void applyChar(char c) {
        ArrayList<NFAState> nextStates = new ArrayList<NFAState>();

        for (int i = 0; i < currentStates.size(); i++) {
            if (currentStates.get(i).arrow1 != null &&
                !nextStates.contains(currentStates.get(i).arrow1) &&
                (currentStates.get(i).label1 == '.' ||
                currentStates.get(i).label1 == c)) {
                        nextStates.add(currentStates.get(i).arrow1);
            }
        }
        currentStates = nextStates;
    }
    public void match() {
        boolean gotAccept;
        String buf;
        int bufIndex;
        // process input line in buf
        while (inFile.hasNextLine()) {
            buf = inFile.nextLine();
            for (int startIndex = 0; startIndex < buf.length(); startIndex++) {
                currentStates.clear();
                currentStates.add(startState);
                bufIndex = startIndex;

                // apply substring starting at bufIndex to
                // NFA.  Exit on an accept, end of substring,
                // or trap state
                while (true) {
                    gotAccept = lambdaClosure();
                    if (gotAccept          // accept state entered
                        || bufIndex >= buf.length() // end substring
                        || currentStates.size() == 0)  // trap state
                        break;
                    applyChar(buf.charAt(bufIndex++));
                }

                // display line if match occurred somewhere
                if (gotAccept) {
                    System.out.println(buf);
                    break;                      // go to next line
                }
            }   // end of for loop
        }
    }
}
