package cn.itcast.account.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

@LocalTCC
public interface AccountTCCService {

    /**
     * tcc模式下的try阶段，也就是扣减可用余额
     *
     * @param userId
     * @param money
     */
    @TwoPhaseBusinessAction(name = "deduct", commitMethod = "confirm", rollbackMethod = "cancel")
    void deduct(@BusinessActionContextParameter(paramName = "userId") String userId,
                @BusinessActionContextParameter(paramName = "money") int money);

    /**
     * tcc模式下的confirm阶段，也就是删除冻结记录
     *
     * @param ctx tcc上下文对象
     * @return
     */
    boolean confirm(BusinessActionContext ctx);

    /**
     * tcc模式下的cancel阶段，也就是恢复可用余额，并删除冻结记录
     *
     * @param ctx tcc上下文对象
     * @return
     */
    boolean cancel(BusinessActionContext ctx);
}
