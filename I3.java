// Hand-written I2 Interpreter
import java.io.*;
import java.util.*;
//======================================================
class I2
{
  public static void main(String[] args) throws
                                             IOException
  {
    System.out.println("I2 interpreter written by ...");

    if (args.length != 1)
    {
      System.err.println("Wrong number cmd line args");
      System.exit(1);
    }

    // construct input file name and object
    String inFileName = args[0] + ".s";
    Scanner inFile = new Scanner(new File(inFileName));

    // construct objects that make up the interpreter
    I2SymTab st = new I2SymTab();
    I2TokenMgr tm = new I2TokenMgr(inFile);
    I2Parser parser = new I2Parser(st, tm);

    // parse and interpret
    try
    {
      parser.parse();
    }
    catch (RuntimeException e)
    {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}                                     // end of I2 class
//======================================================
interface I2Constants
{
  // integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int UNSIGNED = 2;
  int ID = 3;
  int ASSIGN = 4;
  int SEMICOLON = 5;
  int LEFTPAREN = 6;
  int RIGHTPAREN = 7;
  int PLUS = 8;
  int MINUS = 9;
  int TIMES = 10;
  int ERROR = 11;
  int READINT = 12;
  int DIVIDE = 13;
  int LEFTCURLYBRACE = 14;
  int RIGHTCURLYBRACE = 15;
  int PRINT = 16;
  int STRING = 17;

  // tokenImage provides string for each token kind
  String[] tokenImage =
  {
     "<EOF>",
     "\"println\"",
     "<UNSIGNED>",
     "<ID>",
     "\"=\"",
     "\";\"",
     "\"(\"",
     "\")\"",
     "\"+\"",
     "\"-\"",
     "\"*\"",
     "<ERROR>",
     "\"readint\"",
     "\"/\"",
     "\"{\"",
     "\"}\"",
     "\"print\"",
     "<STRING>"
  };
}                        // end of I2Constants interface
//======================================================
class I2SymTab
{
  private ArrayList<String> symbol;
  private ArrayList<Integer> value;
  //-----------------------------------------
  public I2SymTab()
  {
    symbol = new ArrayList<String>();
    value = new ArrayList<Integer>();
  }
  //-----------------------------------------
  public int enter(String s)
  {
    int index = symbol.indexOf(s);

    // if s is in symbol then return its index
    if (index >= 0)
      return index;

    // add s to symbol, return its index
    index = symbol.size();
    symbol.add(s);          // add s to symbol
    value.add(0);           // make init value 0
    return index;
  }
  //-----------------------------------------
  public Integer getValue(int index)
  {
    return value.get(index);
  }
  //-----------------------------------------
  public void setValue(int index, Integer v)
  {
    value.set(index, v);
  }
}                               // end of I2SymTab class
//======================================================
class I2TokenMgr implements I2Constants
{
  private Scanner inFile;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  //-----------------------------------------
  public I2TokenMgr(Scanner inFile)
  {
    this.inFile = inFile;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar))
      getNextChar();

    // construct token to be returned to parser
    token = new Token();
    token.next = null;

    // record start-of-token position
    token.beginLine = currentLineNumber;
    token.beginColumn = currentColumnNumber;

    // check for EOF
    if (currentChar == EOF)
    {
      token.image = "<EOF>";
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      token.kind = EOF;
    }

    else  // check for unsigned int
    if (Character.isDigit(currentChar))
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();
      token.kind = UNSIGNED;
    }

    else  // check for identifier
    if (Character.isLetter(currentChar))
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isLetterOrDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();

      // check if keyword
      if (token.image.equals("println"))
        token.kind = PRINTLN;
      else if (token.image.equals("print"))
        token.kind = PRINT;
      else if (token.image.equals("readint"))
        token.kind = READINT;
      else  // not a keyword so kind is ID
        token.kind = ID;
    }
    else  // check for String
    if (currentChar == '"')
    {
      buffer.setLength(0);  // clear the buffer
      do  // build token image in buffer
      {
          buffer.append(currentChar);
          token.endLine = currentLineNumber;
          token.endColoumn = currentColumnNumber;
          getNextChar();
      } while (currentChar != '"' && currentChar != '\n');
      // append ending quote
      if (currentChar == '"') {
          buffer.append(currentChar);
          token.endLine = currentLineNumber;
          token.endColumn = currentColumnNumber;
          // save buffer as String in token image
          token.image = buffer.toString();
          token.kind = STRING;
      }
      else
        token.kind = ERROR;
    }
    else  // process single-character token
    {
      switch(currentChar)
      {
        case '=':
          token.kind = ASSIGN;
          break;
        case ';':
          token.kind = SEMICOLON;
          break;
        case '(':
          token.kind = LEFTPAREN;
          break;
        case ')':
          token.kind = RIGHTPAREN;
          break;
        case '+':
          token.kind = PLUS;
          break;
        case '-':
          token.kind = MINUS;
          break;
        case '*':
          token.kind = TIMES;
          break;
        case '/':
          token.kind = DIVIDE;
          break;
        case '{':
          token.kind = LEFTCURLYBRACE;
          break;
        case '}':
          token.kind = RIGHTCURLYBRACE;
          break;
        default:
          token.kind = ERROR;
          break;
      }

      // save currentChar as String in token.image
      token.image = Character.toString(currentChar);

      // save end location
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;

      getNextChar();  // read beyond end
    }

    return token;
  }
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')        // need line?
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        inputLine = inputLine + "\n";   // mark line end
        currentColumnNumber = 0;
        currentLineNumber++;
      }
      else  // at end of file
      {
         currentChar = EOF;
         return;
      }
    }

    // get next character from inputLine
    currentChar =
                inputLine.charAt(currentColumnNumber++);

    // test for single-line comment
    if (currentChar == '/' && inputLine.charAt(currentColumnNumber)
                    == '/')
        currentChar = '\n';
  }
}                                   // end of I2TokenMgr
//======================================================
class I2Parser implements I2Constants
{
  private I2SymTab st;
  private I2TokenMgr tm;
  private Token currentToken;
  private Token previousToken;
  private Stack<Integer> s;
  //-----------------------------------------
  public I2Parser(I2SymTab st, I2TokenMgr tm)
  {
    this.st = st;
    this.tm = tm;
    currentToken = tm.getNextToken();   // prime
    previousToken = null;
    s = new Stack<Integer>();
  }
  //-----------------------------------------
  // Construct and return an exception that contains
  // a message consisting of the image of the current
  // token, its location, and the expected tokens.
  //
  private RuntimeException genEx(String errorMessage)
  {
    return new RuntimeException("Encountered \"" +
      currentToken.image + "\" on line " +
      currentToken.beginLine + " column " +
      currentToken.beginColumn +
      System.getProperty("line.separator") +
      errorMessage);
  }
  //-----------------------------------------
  // Advance currentToken to next token.
  //
  private void advance()
  {
    previousToken = currentToken;

    // If next token is on token list, advance to it.
    if (currentToken.next!=null)
      currentToken = currentToken.next;

    // Otherwise, get next token from token mgr and
    // put it on the list.
    else
      currentToken =
                  currentToken.next = tm.getNextToken();
  }
  //-----------------------------------------
  // getToken(i) returns ith token without advancing
  // in token stream.  getToken(0) returns
  // previousToken.  getToken(1) returns currentToken.
  // getToken(2) returns next token, and so on.
  //
  private Token getToken(int i)
  {
    if (i <= 0)
      return previousToken;

    Token t = currentToken;
    for (int j = 1; j < i; j++)  // loop to ith token
    {
      // if next token is on token list, move t to it
      if (t.next != null)
        t = t.next;

      // Otherwise, get next token from token mgr and
      // put it on the list.
      else
        t = t.next = tm.getNextToken();
    }
    return t;
  }
  //-----------------------------------------
  // If the kind of the current token matches the
  // expected kind, then consume advances to the next
  // token. Otherwise, it throws an exception.
  //
  private void consume(int expected)
  {
    if (currentToken.kind == expected)
      advance();
    else
      throw genEx("Expecting " + tokenImage[expected]);
  }
  //-----------------------------------------
  public void parse()
  {
    program();
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    if (currentToken.kind != EOF) // garbage at end?
      throw genEx("Expecting <EOF>");
  }
  //-----------------------------------------
  private void statementList()
  {
    switch(currentToken.kind)
    {
      case ID:
      case PRINTLN:
      case PRINT:
      case SEMICOLON:
      case LEFTCURLYBRACE:
        statement();
        statementList();
        break;
      case EOF:
      case RIGHTCURLYBRACE:
        ;
        break;
      default:
        throw genEx("Expecting statement or <EOF>");
    }
  }
  //-----------------------------------------
  private void statement()
  {
    switch(currentToken.kind)
    {
      case ID:
        assignmentStatement();
        break;
      case PRINTLN:
        printlnStatement();
        break;
      case PRINT:
        printStatement();
        break;
      case SEMICOLON:
        nullStatement();
        break;
      case LEFTCURLYBRACE:
        compoundStatement();
        break;
      default:
        throw genEx("Expecting statement");
    }
  }
  //-----------------------------------------
  private void assignmentStatement()
  {
    Token t;
    int left, expVal;

    t = currentToken;
    consume(ID);
    left  = st.enter(t.image);
    consume(ASSIGN);
    expr();
    st.setValue(left, s.pop());
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printlnStatement()
  {
        consume(PRINTLN);
        consume(LEFTPAREN);
        printArg();
        System.out.println(s.pop());
        consume(RIGHTPAREN);
        consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printStatement()
  {
      consume(PRINT);
      consume(LEFTPAREN);
      printArg();
      consume(RIGHTPAREN);
      consume(SEMICOLON);
  }
  //------------------------------------------
  private void printArg()
  {

  }
  private void expr()
  {
    term();
    termList();
  }
  //-----------------------------------------
  private void termList()
  {
    int right;

    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        term();
        right = s.pop();
        s.push(s.pop() + right);
        termList();
        break;
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        break;
      default:
        throw genEx("Expecting \"+\", \")\", or \";\"");
    }
 }
  //-----------------------------------------
  private void term()
  {
    factor();
    factorList();
  }
  //-----------------------------------------
  private void factorList()
  {
    int right;

    switch(currentToken.kind)
    {
      case TIMES:
        consume(TIMES);
        factor();
        right = s.pop();
        s.push(s.pop() * right);
        factorList();
        break;
      case PLUS:
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        break;
      default:
        throw genEx("Expecting op, \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private void factor()
  {
    Token t;
    int index;

    switch(currentToken.kind)
    {
      case UNSIGNED:
        t = currentToken;
        consume(UNSIGNED);
        s.push(Integer.parseInt(t.image));
        break;
      case PLUS:
        consume(PLUS);
        t = currentToken;
        consume(UNSIGNED);
        s.push(Integer.parseInt(t.image));
        break;
      case MINUS:
        consume(MINUS);
        t = currentToken;
        consume(UNSIGNED);
        s.push(Integer.parseInt("-" + t.image));
        break;
      case ID:
        t = currentToken;
        consume(ID);
        index = st.enter(t.image);
        s.push(st.getValue(index));
        break;
      case LEFTPAREN:
        consume(LEFTPAREN);
        expr();
        consume(RIGHTPAREN);
        break;
      default:
        throw genEx("Expecting factor");
    }
  }
}                               // end of I2Parser class
