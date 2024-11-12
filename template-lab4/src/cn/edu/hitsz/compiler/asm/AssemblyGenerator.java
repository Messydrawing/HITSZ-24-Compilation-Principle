package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;

/**
 * 汇编生成器实现
 */
public class AssemblyGenerator {

    // 存储IR指令列表
    private List<Instruction> instructions;

    // 存储生成的汇编代码
    private final List<String> asmCode = new ArrayList<>();

    // 可用的寄存器列表，只使用 t0-t6
    private final List<String> registers = Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5", "t6");

    // 变量到寄存器的映射
    private final Map<String, String> variableRegMap = new HashMap<>();

    // 变量的引用计数，用于管理寄存器的分配和释放
    private final Map<String, Integer> variableUsageCount = new HashMap<>();

    /**
     * 加载前端提供的中间代码
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        this.instructions = originInstructions;
        analyzeVariableUsage();
    }

    /**
     * 分析变量的使用次数
     */
    private void analyzeVariableUsage() {
        // 遍历所有指令，统计每个变量的使用次数
        for (Instruction instruction : instructions) {
            switch (instruction.getKind()) {
                case MOV:
                    increaseUsageCount(instruction.getResult());
                    increaseUsageCount(instruction.getFrom());
                    break;
                case ADD:
                case SUB:
                case MUL:
                    increaseUsageCount(instruction.getResult());
                    increaseUsageCount(instruction.getLHS());
                    increaseUsageCount(instruction.getRHS());
                    break;
                case RET:
                    increaseUsageCount(instruction.getReturnValue());
                    break;
                default:
                    // 其他指令类型暂不处理
                    break;
            }
        }
    }

    /**
     * 增加变量的引用计数
     *
     * @param value IRValue
     */
    private void increaseUsageCount(IRValue value) {
        if (value instanceof IRVariable) {
            String varName = ((IRVariable) value).getName();
            variableUsageCount.put(varName, variableUsageCount.getOrDefault(varName, 0) + 1);
        }
    }

    /**
     * 执行代码生成
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        for (Instruction instruction : instructions) {
            String resultReg, operand1Reg, operand2Reg;

            switch (instruction.getKind()) {
                case MOV:
                    operand1Reg = getRegisterForOperand(instruction.getFrom());
                    resultReg = allocateRegister((IRVariable) instruction.getResult());
                    asmCode.add(String.format("mv %s, %s", resultReg, operand1Reg));
                    decreaseUsageAndRelease(instruction.getFrom(), operand1Reg);
                    decreaseUsageAndRelease(instruction.getResult(), resultReg);
                    break;

                case ADD:
                    operand1Reg = getRegisterForOperand(instruction.getLHS());
                    operand2Reg = getRegisterForOperand(instruction.getRHS());
                    resultReg = allocateRegister((IRVariable) instruction.getResult());
                    asmCode.add(String.format("add %s, %s, %s", resultReg, operand1Reg, operand2Reg));
                    decreaseUsageAndRelease(instruction.getLHS(), operand1Reg);
                    decreaseUsageAndRelease(instruction.getRHS(), operand2Reg);
                    decreaseUsageAndRelease(instruction.getResult(), resultReg);
                    break;

                case SUB:
                    operand1Reg = getRegisterForOperand(instruction.getLHS());
                    operand2Reg = getRegisterForOperand(instruction.getRHS());
                    resultReg = allocateRegister((IRVariable) instruction.getResult());
                    asmCode.add(String.format("sub %s, %s, %s", resultReg, operand1Reg, operand2Reg));
                    decreaseUsageAndRelease(instruction.getLHS(), operand1Reg);
                    decreaseUsageAndRelease(instruction.getRHS(), operand2Reg);
                    decreaseUsageAndRelease(instruction.getResult(), resultReg);
                    break;

                case MUL:
                    operand1Reg = getRegisterForOperand(instruction.getLHS());
                    operand2Reg = getRegisterForOperand(instruction.getRHS());
                    resultReg = allocateRegister((IRVariable) instruction.getResult());
                    asmCode.add(String.format("mul %s, %s, %s", resultReg, operand1Reg, operand2Reg));
                    decreaseUsageAndRelease(instruction.getLHS(), operand1Reg);
                    decreaseUsageAndRelease(instruction.getRHS(), operand2Reg);
                    decreaseUsageAndRelease(instruction.getResult(), resultReg);
                    break;

                case RET:
                    operand1Reg = getRegisterForOperand(instruction.getReturnValue());
                    if (!operand1Reg.equals("a0")) {
                        asmCode.add(String.format("mv a0, %s", operand1Reg));
                    }
                    decreaseUsageAndRelease(instruction.getReturnValue(), operand1Reg);
                    break;

                default:
                    // 其他指令类型暂不处理
                    break;
            }
        }
    }

    /**
     * 获取操作数对应的寄存器
     *
     * @param operand 操作数
     * @return 寄存器名称
     */
    private String getRegisterForOperand(IRValue operand) {
        if (operand instanceof IRImmediate) {
            // 对于立即数，加载到临时寄存器
            String tempReg = getFreeRegister();
            if (tempReg == null) {
                throw new RuntimeException("没有可用的寄存器");
            }
            asmCode.add(String.format("li %s, %s", tempReg, ((IRImmediate) operand).getValue()));
            return tempReg;
        } else {
            // 对于变量，获取其对应的寄存器
            IRVariable variable = (IRVariable) operand;
            return allocateRegister(variable);
        }
    }

    /**
     * 为变量分配寄存器
     *
     * @param variable 变量
     * @return 寄存器名称
     */
    private String allocateRegister(IRVariable variable) {
        String varName = variable.getName();
        if (variableRegMap.containsKey(varName)) {
            return variableRegMap.get(varName);
        } else {
            String reg = getFreeRegister();
            if (reg == null) {
                throw new RuntimeException("没有可用的寄存器");
            }
            variableRegMap.put(varName, reg);
            return reg;
        }
    }

    /**
     * 减少变量的引用计数，并在计数为0时释放寄存器
     *
     * @param value IRValue
     * @param reg   寄存器名称
     */
    private void decreaseUsageAndRelease(IRValue value, String reg) {
        if (value instanceof IRVariable) {
            String varName = ((IRVariable) value).getName();
            int count = variableUsageCount.getOrDefault(varName, 0) - 1;
            variableUsageCount.put(varName, count);
            if (count <= 0) {
                variableRegMap.remove(varName);
            }
        }
        // 对于立即数的寄存器，在使用后无需处理，因为立即数的寄存器是临时的
    }

    /**
     * 获取一个可用的寄存器
     *
     * @return 寄存器名称
     */
    private String getFreeRegister() {
        Set<String> allocatedRegs = new HashSet<>(variableRegMap.values());
        for (String reg : registers) {
            if (!allocatedRegs.contains(reg)) {
                return reg;
            }
        }
        return null;
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        // 使用 FileUtils 工具类将 asmCode 写入文件
        FileUtils.writeLines(path, asmCode);
    }
}
