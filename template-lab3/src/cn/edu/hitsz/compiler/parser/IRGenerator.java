package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private final Stack<IRValue> operandStack = new Stack<>();
    private final List<Instruction> instructionList = new ArrayList<>();
    private SymbolTable symbolTable;

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if (currentToken.getKind().getIdentifier().equals("id")) {
            // 使用 IRVariable 的 named 方法来创建实例
            operandStack.push(IRVariable.named(currentToken.getText()));
        } else if (currentToken.getKind().getIdentifier().equals("IntConst")) {
            // 使用 IRImmediate.of() 或类似方法（如果存在），否则自行实现一个静态方法
            operandStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        }
    }



    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.toString()) {
            case "S -> id = E":
                // 生成 MOV 指令
                IRValue exprValue = operandStack.pop();
                IRVariable id = (IRVariable) operandStack.pop();
                instructionList.add(Instruction.createMov(id, exprValue));
                break;

            case "S -> return E":
                // 生成 RET 指令
                IRValue returnValue = operandStack.pop();
                instructionList.add(Instruction.createRet(returnValue));
                break;

            case "E -> E + A":
                // 生成 ADD 指令
                IRValue operandA = operandStack.pop();
                IRValue operandE = operandStack.pop();
                IRVariable tempAddVar = IRVariable.temp();
                instructionList.add(Instruction.createAdd(tempAddVar, operandE, operandA));
                operandStack.push(tempAddVar); // 将结果压入栈中
                break;

            case "E -> E - A":
                // 生成 SUB 指令
                IRValue operandA2 = operandStack.pop();
                IRValue operandE2 = operandStack.pop();
                IRVariable tempSubVar = IRVariable.temp();
                instructionList.add(Instruction.createSub(tempSubVar, operandE2, operandA2));
                operandStack.push(tempSubVar);
                break;

            case "A -> A * B":
                // 生成 MUL 指令
                IRValue operandB = operandStack.pop();
                IRValue operandA3 = operandStack.pop();
                IRVariable tempMulVar = IRVariable.temp();
                instructionList.add(Instruction.createMul(tempMulVar, operandA3, operandB));
                operandStack.push(tempMulVar);
                break;

            case "B -> id":
            case "B -> IntConst":
                // 在 shift 时已处理，无需额外指令
                break;

            default:
                // 处理其他可能的产生式
                break;
        }

    }

    @Override
    public void whenAccept(Status currentStatus) {
        // 打印确认信息，表明 IR 生成已经成功完成
        System.out.println("IR 生成完成，已成功接受。");

        // 如果需要，可以在此执行其他收尾操作，例如输出 IR 到控制台或文件
        for (Instruction instruction : instructionList) {
            System.out.println(instruction);
        }

    }


    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return instructionList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

