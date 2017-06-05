// Hand-written CI2 Compiler-interpreter
import java.io.*;
import java.util.*;
//======================================================
class CI2
{
  public static void main(String[] args) throws
                                             IOException
  {
    System.out.println("CI2 interpreter written by Marc Gagliardo");

    if (args.length != 1)
    {
       System.err.println("Wrong number cmd line args");
       System.exit(1);
    }

    // construct input file name and object
    String inFileName = args[0] + ".s";
    Scanner inFile = new Scanner(new File(inFileName));

    // construct objects for compiler/interpreter
    CI2SymTab st = new CI2SymTab();
    CI2TokenMgr tm = new CI2TokenMgr(inFile);
    CI2CodeGen cg = new CI2CodeGen();
    CI2Parser parser = new CI2Parser(st, tm, cg);

    // parse, translate, and interpret
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
}                                    // end of CI2 class
//======================================================
interface CI2Constants
{
  // integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int PRINT = 2;
  int UNSIGNED = 3;
  int ID = 4;
  int ASSIGN = 5;
  int SEMICOLON = 6;
  int LEFTPAREN = 7;
  int RIGHTPAREN = 8;
  int PLUS = 9;
  int MINUS = 10;
  int TIMES = 11;
  int DIVIDE = 12;
  int LEFTCURLYBRACE = 13;
  int RIGHTCURLYBRACE = 14;
  int ERROR = 15;

  // opcodes
  int PUSH = 16;
  int PUSHCONSTANT = 17;
  int HALT = 18;

  // tokenImage provides string for each token kind
  String[] tokenImage =
  {
    "<EOF>",
    "\"println\"",
    "\"print\"",
    "<UNSIGNED>",
    "<ID>",
    "\"=\"",
    "\";\"",
    "\"(\"",
    "\")\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"}\"",
    "\"{\"",
    "<ERROR>"
  };
}                       // end of CI2Constants interface
//======================================================
class CI2SymTab
{
  private ArrayList<String> symbol;
  //-----------------------------------------
  public CI2SymTab()
  {
     symbol = new ArrayList<String>();
  }
  //-----------------------------------------
  public int enter(String s)
  {
    // if s is in symbol then return its index
    int index = symbol.indexOf(s);
    if (index >= 0)
      return index;

    // add s to symbol, return its index
    index = symbol.size();
    symbol.add(s);
    return index;
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
}                                    // end of CI2SymTab
//======================================================
class CI2TokenMgr implements CI2Constants
{
  private Scanner inFile;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  //-----------------------------------------
  public CI2TokenMgr(Scanner inFile)
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
      else  // not a keyword so kind is ID
        token.kind = ID;
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
    if (currentChar == EOF)         // do nothing if EOF
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
}                                  // end of CI2TokenMgr
//======================================================
class CI2Parser implements CI2Constants
{
  private CI2SymTab st;
  private CI2TokenMgr tm;
  private Token currentToken;
  private Token previousToken;
  private CI2CodeGen cg;
  //-----------------------------------------
  public CI2Parser(CI2SymTab st, CI2TokenMgr tm,
                   CI2CodeGen cg)
  {
    this.st = st;
    this.tm = tm;
    currentToken = tm.getNextToken();   // prime
    previousToken = null;
    this.cg = cg;
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
    cg.makevtab(st.getSize());
    cg.interpret();
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    if (currentToken.kind != EOF)
      throw genEx("Expecting <EOF>");
    cg.emit(HALT);
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

    t = currentToken;
    consume(ID);
    consume(ASSIGN);
    expr();
    cg.emit(ASSIGN);
    cg.emit(st.enter(t.image));
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printlnStatement()
  {
    consume(PRINTLN);
    consume(LEFTPAREN);
    expr();
    cg.emit(PRINTLN);
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printStatement()
  {
      consume(PRINT);
      consume(LEFTPAREN);
      expr();
      cg.emit(PRINT);
      consume(RIGHTPAREN);
      consume(SEMICOLON);
  }
  //-------------------------------------------
  private void nullStatement()
  {
      consume(SEMICOLON);
  }
  //--------------------------------------------
  private void compoundStatement()
  {
      consume(LEFTCURLYBRACE);
      statementList();
      consume(RIGHTCURLYBRACE);
  }
  //--------------------------------------------
  private void expr()
  {
    term();
    termList();
  }
  //-----------------------------------------
  private void termList()
  {
    int left, right;

    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        term();
        cg.emit(PLUS);
        termList();
        break;
      case MINUS:
        consume(MINUS);
        term();
        cg.emit(MINUS);
        termList();
        break;
      case RIGHTPAREN:
      case SEMICOLON:
        ;
        break;
      default:
        throw genEx("Expecting \"+\", \"-\", \")\", or \";\"");
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
    int left, right;

    switch(currentToken.kind)
    {
       case TIMES:
         consume(TIMES);
         factor();
         cg.emit(TIMES);
         factorList();
         break;
       case DIVIDE:
         consume(DIVIDE);
         factor();
         cg.emit(DIVIDE);
         factorList();
         break;
       case PLUS:
       case MINUS:
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
        cg.emit(PUSHCONSTANT);
        cg.emit(Integer.parseInt(t.image));
        break;
      case PLUS:
        consume(PLUS);
        t = currentToken;
        consume(UNSIGNED);
        cg.emit(PUSHCONSTANT);
        cg.emit(Integer.parseInt(t.image));
        break;
      case MINUS:
        consume(MINUS);
        t = currentToken;
        consume(UNSIGNED);
        cg.emit(PUSHCONSTANT);
        cg.emit(Integer.parseInt("-" + t.image));
        break;
      case ID:
        t = currentToken;
        consume(ID);
        index = st.enter(t.image);
        cg.emit(PUSH);
        cg.emit(index);
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
}                              // end of CI2Parser class
//======================================================
class CI2CodeGen implements CI2Constants
{
  private ArrayList<Integer> scode;      // holds s-code
  private Stack<Integer> s;   // stack used by s-machine
  int[] vtab;                     // table for variables
  private int pc;    // program ctr (index of next inst)
  private int opcode;   // opcode of current instruction

  public CI2CodeGen()
  {
    scode = new ArrayList<Integer>();
    s = new Stack<Integer>();
    pc = 0; // start executing scode at index 0 in scode
  }
  //-----------------------------------------
  public void emit(int inst)
  {
    scode.add(inst);        // emit instruction to scode
  }
  //-----------------------------------------
  public void makevtab(int size)
  {
    vtab = new int[size];  // create table for variables
  }
  //-----------------------------------------
  public void interpret() // interprets s-code in scode
  {
    boolean doAgain = true;
    int right;

    do
    {
      // fetch the opcode of the next instruction
      opcode = scode.get(pc++);

      // decode opcode and execute instruction
      switch(opcode)
      {
        case PRINTLN:
          System.out.println(s.pop());
          break;
        case PRINT:
          System.out.print(s.pop());
          break;
        case ASSIGN:
          vtab[scode.get(pc++)] = s.pop();
          break;
        case PLUS:
          right = s.pop();
          s.push(s.pop() + right);
          break;
        case MINUS:
          right = s.pop();
          s.push(s.pop() - right);
          break;
        case TIMES:
          right = s.pop();
          s.push(s.pop() * right);
          break;
        case DIVIDE:
          right = s.pop();
          s.push(s.pop() / right);
          break;
        case PUSHCONSTANT:
          s.push(scode.get(pc++));
          break;
        case PUSH:
          s.push(vtab[scode.get(pc++)]);
          break;
        case HALT:
          doAgain = false;
          break;
        default:
          doAgain = false;
          break;
      }
    } while (doAgain);
  }
}                             // end of CI2CodeGen class
