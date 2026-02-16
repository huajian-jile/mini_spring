package liu.spring.container.aop;

import java.lang.reflect.Method;

/**
 * 切点顾问条目：切点表达式 + 切面方法 + 通知类型 + 顺序。
 */
public class PointcutAdvisorEntry {
    private final String expression;
    private final Method adviceMethod;
    private final AdviceType adviceType;
    private final int order;

    public PointcutAdvisorEntry(String expression, Method adviceMethod, AdviceType adviceType, int order) {
        this.expression = expression;
        this.adviceMethod = adviceMethod;
        this.adviceType = adviceType;
        this.order = order;
    }

    public String getExpression() { return expression; }
    public Method getAdviceMethod() { return adviceMethod; }
    public AdviceType getAdviceType() { return adviceType; }
    public int getOrder() { return order; }
}
