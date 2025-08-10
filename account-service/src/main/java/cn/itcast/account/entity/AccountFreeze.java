package cn.itcast.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author 虎哥
 */
@Data
@TableName("account_freeze_tbl")
public class AccountFreeze {
    @TableId(type = IdType.INPUT)
    private String xid;

    private String userId;

    /**
     * 冻结金额
     */
    private Integer freezeMoney;

    private Integer state;

    /**
     * 事务状态
     * TRY：尝试阶段
     * CONFIRM：确认阶段
     * CANCEL：回滚阶段
     */
    public static abstract class State {
        public final static int TRY = 0;
        public final static int CONFIRM = 1;
        public final static int CANCEL = 2;
    }
}
