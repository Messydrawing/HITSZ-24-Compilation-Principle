package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;


import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    private Token currentToken; // 添加此行，声明为类成员变量

    private final Stack<String> typeStack = new Stack<>(); // 用于存储类型信息
    private final Stack<String> valueStack = new Stack<>(); // 用于存储表达式的值
    private int tempVarCounter = 0; // 临时变量计数器
    @Override
    public void whenAccept(Status currentStatus) {
        //System.out.println("语义分析器已成功接收");
        // TODO: 该过程在遇到 Accept 时要采取的代码动作

    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch (production.toString()) {
            case "D -> int":
                // 将 SourceCodeType.Int 类型压入栈
                typeStack.push("int");
                break;
            case "S -> D id":
                String id = valueStack.pop();
                String type = typeStack.pop();
                //System.out.println("正在处理声明: " + id);
                if (!symbolTable.has(id)) {
                    // 添加符号到符号表，并设置类型
                    SymbolTableEntry entry = symbolTable.add(id);
                    entry.setType(SourceCodeType.Int);
                    //System.out.println("符号表更新: 添加 " + id);
                } else {
                    System.out.println("符号表中已存在: " + id);
                    throw new RuntimeException("重复声明错误: " + id);
                }
                break;

            case "S -> id = E":
                String exprValue = valueStack.pop(); // 右侧表达式
                String idToAssign = valueStack.pop(); // 左侧变量

                // 调试输出
                //System.out.println("赋值语句左侧变量: " + idToAssign);
                //System.out.println("赋值语句右侧表达式: " + exprValue);

                // 检查左侧变量是否已声明
                if (!symbolTable.has(idToAssign)) {
                    //throw new RuntimeException("未声明的标识符: " + idToAssign);
                }

                // 检查右侧表达式的符号表，排除对临时变量的检查
                if (!isNumeric(exprValue) && !exprValue.startsWith("$") && !symbolTable.has(exprValue)) {
                    //throw new RuntimeException("未声明的标识符: " + exprValue);
                }
                break;
            case "S -> return E":
                String returnValue = valueStack.pop();
                generateCode("RET " + returnValue);
                break;
            case "E -> E + A":
            case "E -> E - A":
            case "E -> E & A":
            case "E -> E / A":
                String rightOperand = valueStack.pop();
                String leftOperand = valueStack.pop();
                String operator = production.body().get(1).toString(); // 获取操作符
                String tempVar = getNextTempVar();
                generateCode(tempVar + " = " + leftOperand + " " + operator + " " + rightOperand);
                valueStack.push(tempVar);
                break;
            case "A -> A * B":
                String b = valueStack.pop();
                String a = valueStack.pop();
                String tempMulVar = getNextTempVar();
                generateCode(tempMulVar + " = " + a + " * " + b);
                valueStack.push(tempMulVar);
                break;
            case "B -> id":
            case "B -> IntConst":
                // 改为从 valueStack 中获取当前处理的值，不使用 currentToken
                String value = valueStack.peek(); // 或者根据语法规则使用 pop()
                //System.out.println("处理表达式中的值: " + value);
                break;

            default:
                break;
        }

    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if (currentToken.getKind().getIdentifier().equals("id")) {
            valueStack.push(currentToken.getText()); // 标识符压入栈
            //System.out.println("标识符压入栈: " + currentToken.getText());
        } else if (currentToken.getKind().getIdentifier().equals("IntConst")) {
            valueStack.push(currentToken.getText()); // 常量压入栈
            //System.out.println("常量压入栈: " + currentToken.getText());
        }
    }


    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        this.symbolTable = table;
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储

    }

    // 辅助方法用于生成中间代码
    private void generateCode(String code) {
        //System.out.println("生成中间代码: " + code);
    }

    // 获取下一个临时变量名
    private String getNextTempVar() {
        return "$t" + (tempVarCounter++);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}

