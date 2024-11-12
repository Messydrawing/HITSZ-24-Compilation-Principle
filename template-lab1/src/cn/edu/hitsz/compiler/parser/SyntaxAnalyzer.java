package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Action;
import cn.edu.hitsz.compiler.parser.table.LRTable;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private final List<Token> tokens = new ArrayList<>();
    private LRTable lrTable;
    private int currentIndex = 0;

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // 加载词法单元
        tokens.forEach(this.tokens::add);
    }

    public void loadLRTable(LRTable table) {
        // 加载 LR 分析表
        this.lrTable = table;
    }

    public void run() {
        Stack<Status> statusStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        statusStack.push(lrTable.getInit()); // 初始状态加入状态栈

        while (currentIndex < tokens.size()) {
            Status currentStatus = statusStack.peek();
            Token currentToken = tokens.get(currentIndex);

            System.out.println("Current Status Stack: " + statusStack);
            System.out.println("Current Symbol Stack: " + symbolStack);
            System.out.println("Current Token: " + currentToken);

            // 查询 ACTION 表
            Action action = lrTable.getAction(currentStatus, currentToken);
            System.out.println("Action found: " + action.getKind());

            switch (action.getKind()) {
                case Shift:
                    // 执行 shift 操作
                    Status nextState = action.getStatus();
                    statusStack.push(nextState);
                    symbolStack.push(currentToken.getText());
                    callWhenInShift(currentStatus, currentToken);
                    currentIndex++; // 移动到下一个 token
                    System.out.println("Shift action performed. Moved to state: " + nextState);
                    break;

                case Reduce:
                    // 执行 reduce 操作
                    Production production = action.getProduction();
                    for (int i = 0; i < production.body().size(); i++) {
                        statusStack.pop();
                        symbolStack.pop();
                    }
                    symbolStack.push(production.head().toString());

                    Status gotoState = lrTable.getGoto(statusStack.peek(), production.head());
                    statusStack.push(gotoState);

                    callWhenInReduce(currentStatus, production);
                    System.out.println("Reduce action performed with production: " + production);
                    break;

                case Accept:
                    // 执行 accept 操作
                    callWhenInAccept(currentStatus);
                    System.out.println("Parsing accepted successfully.");
                    return;

                case Error:
                    System.err.println("Syntax error at token: " + currentToken + " in state: " + currentStatus);
                    throw new RuntimeException("Syntax error at token: " + currentToken);
            }
        }
    }

}