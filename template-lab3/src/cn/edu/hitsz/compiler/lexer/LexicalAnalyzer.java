package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;
import cn.edu.hitsz.compiler.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;


/**
 * 实验一: 实现词法分析
 * 你可能需要参考的框架代码如下:
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String sourceCode;
    private final List<Token> tokens = new ArrayList<>(); // 存储Token列表

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // 读取整个文件内容并存入sourceCode字符串中
        this.sourceCode = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表
     * 需要维护符号表条目
     */
// 定义关键字列表，可以从 coding_map.csv 中读取或直接硬编码
    private final Set<String> keywords = Set.of("int", "return");

    public void run() {
        int state = 0;
        StringBuilder currentToken = new StringBuilder();
        char[] chars = sourceCode.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];

            switch (state) {
                case 0:
                    if (Character.isWhitespace(ch)) {
                        continue;  // 忽略空白字符
                    } else if (Character.isLetter(ch)) {
                        currentToken.append(ch);
                        state = 1;  // 进入标识符状态
                    } else if (Character.isDigit(ch)) {
                        currentToken.append(ch);
                        state = 2;  // 进入数字状态
                    } else if (ch == '=') {
                        // 处理 `=`
                        tokens.add(Token.simple(TokenKind.fromString("=")));
                    } else if (ch == '+') {
                        tokens.add(Token.simple(TokenKind.fromString("+")));
                    } else if (ch == '-') {
                        tokens.add(Token.simple(TokenKind.fromString("-")));
                    } else if (ch == '*') {
                        tokens.add(Token.simple(TokenKind.fromString("*")));
                    } else if (ch == '/') {
                        tokens.add(Token.simple(TokenKind.fromString("/")));
                    } else if (ch == ';') {
                        tokens.add(Token.simple(TokenKind.fromString("Semicolon")));
                    } else if (ch == '(') {
                        tokens.add(Token.simple(TokenKind.fromString("(")));
                    } else if (ch == ')') {
                        tokens.add(Token.simple(TokenKind.fromString(")")));
                    } else {
                        System.err.println("Unknown character: " + ch);
                    }
                    break;

                case 1:  // 处理标识符或关键字
                    if (Character.isLetterOrDigit(ch)) {
                        currentToken.append(ch);
                    } else {
                        String tokenStr = currentToken.toString();

                        // 检查是否是关键字
                        if (keywords.contains(tokenStr)) {
                            tokens.add(Token.simple(TokenKind.fromString(tokenStr)));
                        } else {
                            tokens.add(Token.normal(TokenKind.fromString("id"), tokenStr));
                            // 不再在词法分析阶段添加到符号表
                        }
                        currentToken.setLength(0);  // 清空Token缓存
                        state = 0;  // 返回初始状态
                        i--;  // 回退一个字符
                    }
                    break;


                case 2:  // 处理数字
                    if (Character.isDigit(ch)) {
                        currentToken.append(ch);
                    } else {
                        tokens.add(Token.normal(TokenKind.fromString("IntConst"), currentToken.toString()));
                        currentToken.setLength(0);
                        state = 0;
                        i--;  // 回退一个字符
                    }
                    break;

                default:
                    throw new RuntimeException("Unexpected state: " + state);
            }
        }

        // 处理剩余的Token
        if (currentToken.length() > 0) {
            if (state == 1) {
                String tokenStr = currentToken.toString();
                if (keywords.contains(tokenStr)) {
                    tokens.add(Token.simple(TokenKind.fromString(tokenStr)));
                } else {
                    tokens.add(Token.normal(TokenKind.fromString("id"), tokenStr));
                    if (!symbolTable.has(tokenStr)) {
                        symbolTable.add(tokenStr);
                    }
                }
            } else if (state == 2) {
                tokens.add(Token.normal(TokenKind.fromString("IntConst"), currentToken.toString()));
            }
        }

        // 添加EOF符号
        tokens.add(Token.eof());
    }



    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                tokens.stream().map(Token::toString).toList()
        );
    }
}
