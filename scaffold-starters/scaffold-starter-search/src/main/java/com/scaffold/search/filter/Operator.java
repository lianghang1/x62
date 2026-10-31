package com.scaffold.search.filter;

import cn.zhxu.bs.FieldOp;
import cn.zhxu.bs.operator.Contain;
import cn.zhxu.bs.operator.Empty;
import cn.zhxu.bs.operator.EndWith;
import cn.zhxu.bs.operator.Equal;
import cn.zhxu.bs.operator.GreaterEqual;
import cn.zhxu.bs.operator.GreaterThan;
import cn.zhxu.bs.operator.InList;
import cn.zhxu.bs.operator.IsNull;
import cn.zhxu.bs.operator.NotNull;
import cn.zhxu.bs.operator.LessEqual;
import cn.zhxu.bs.operator.LessThan;
import cn.zhxu.bs.operator.NotBetween;
import cn.zhxu.bs.operator.NotEmpty;
import cn.zhxu.bs.operator.NotEqual;
import cn.zhxu.bs.operator.NotIn;
import cn.zhxu.bs.operator.NotLike;
import cn.zhxu.bs.operator.Between;
import cn.zhxu.bs.operator.StartWith;

/**
 * 检索运算符，底层映射到 Bean Searcher FieldOps。
 *
 * @author lianghang
 */
public enum Operator {

    /** 等于 = */
    EQ(Equal.class),
    /** 不等于 != */
    NE(NotEqual.class),
    /** 大于 > */
    GT(GreaterThan.class),
    /** 大于等于 >= */
    GE(GreaterEqual.class),
    /** 小于 < */
    LT(LessThan.class),
    /** 小于等于 <= */
    LE(LessEqual.class),
    /** IN 列表 */
    IN(InList.class),
    /** NOT IN 列表 */
    NI(NotIn.class),
    /** 包含 like %x% */
    CN(Contain.class),
    /** 不包含 not like %x% */
    NC(NotLike.class),
    /** 前缀 like x% */
    LL(StartWith.class),
    /** 后缀 like %x */
    RL(EndWith.class),
    /** IS NULL */
    NU(IsNull.class),
    /** IS NOT NULL */
    NN(NotNull.class),
    /** BETWEEN（值传 [min, max] 两元素集合或数组） */
    BT(Between.class),
    /** NOT BETWEEN（值传 [min, max] 两元素集合或数组） */
    NBT(NotBetween.class),
    /** 空字符串 ='' */
    BLANK(Empty.class),
    /** 非空字符串 !='' */
    NBLANK(NotEmpty.class);

    private final Class<? extends FieldOp> fieldOp;

    Operator(Class<? extends FieldOp> fieldOp) {
        this.fieldOp = fieldOp;
    }

    public Class<? extends FieldOp> fieldOp() {
        return fieldOp;
    }

    /** 是否无需传值（IS NULL / IS NOT NULL / 空串判断） */
    public boolean isValueless() {
        return this == NU || this == NN || this == BLANK || this == NBLANK;
    }
}
