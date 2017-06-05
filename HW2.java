/*Marc Gagliardo                                             September 16, 2016
   CPS565.01 Compiler Design                                               Hw#2

    Top-down stack parser for

                    Selection sets
    1) S -> aSd         {a}
    2) S -> AB          {e, b, c}
    3) A -> eA          {e}
    4) A -> lambda      {b, c}
    5) B -> bB          {b}
    6) B -> c           {c}
*/
import java.util.*;  // import Stack and Scanner classes

public class HW2 {

    public static void main(String[] args) {

        HW2ArgsTokenMgr tm = new HW2ArgsTokenMgr(args);
        HW2Parser parser = new HW2Parser(tm);

        parser.parse();
    }

}
class HW2ArgsTokenMgr {
    private int index;
    String input;

    public HW2ArgsTokenMgr(String[] args) {
        if (args.length > 0)
            input = args[0];
        else
            input = "";
        index = 0;
        System.out.println("input = " + input);
    }
    public char getNextToken() {
        if (index < input.length())
            return input.charAt(index++);
        else
            return '#';
    }
}
class HW2Parser {
    private HW2ArgsTokenMgr tm;
    private Stack<Character> stk;
    private char currentToken;

    public HW2Parser(HW2ArgsTokenMgr tm) {
        this.tm = tm;
        advance();
        stk = new Stack<Character>();
        stk.push('$');
        stk.push('S');
    }
    private void advance() {
        currentToken = tm.getNextToken();
    }
    public void parse() {
        boolean done = false;

        while (!done) {
            switch(stk.peek()) {

                case 'S':
                    if (currentToken == 'a') {
                        stk.pop();
                        stk.push('d');
                        stk.push('S');
                        advance();
                    }else if (currentToken == 'b' || currentToken == 'c' ||
                                currentToken == 'e') {
                        stk.pop();
                        stk.push('B');
                        stk.push('A');
                    }else
                        done = true;
                    break;

                case 'A':
                    if (currentToken == 'b' || currentToken == 'c')
                        stk.pop();
                    else if (currentToken == 'e')
                        advance();
                    else
                        done = true;
                    break;

                case 'B':
                    if (currentToken == 'b')
                        advance();
                    else if (currentToken == 'c') {
                        stk.pop();
                        advance();
                    }else
                        done = true;
                    break;

                case 'd':
                    if (stk.peek() == currentToken) {
                        stk.pop();
                        advance();
                    }else
                        done = true;
                    break;

                case '$':
                    done = true;
                    break;
            }
        }
        if (currentToken == '#' && stk.peek() == '$')
            System.out.println("accept");
        else
            System.out.println("reject");
    }
}
