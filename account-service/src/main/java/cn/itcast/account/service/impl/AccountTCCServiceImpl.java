package cn.itcast.account.service.impl;

import cn.itcast.account.entity.AccountFreeze;
import cn.itcast.account.mapper.AccountFreezeMapper;
import cn.itcast.account.mapper.AccountMapper;
import cn.itcast.account.service.AccountTCCService;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountTCCServiceImpl implements AccountTCCService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountFreezeMapper freezeMapper;

    /**
     * TCC模式下的try阶段，也就是扣减可用余额
     *
     * @param userId 用户ID
     * @param money  扣减金额
     */
    @Override
    @Transactional
    public void deduct(String userId, int money) {
        // 0.获取全局事务id
        String xid = RootContext.getXID();
        // 1.判断freeze表中是否有冻结记录，并且状态是cancel状态的，如果有则一定是cancel执行过了，需要拒绝业务
        AccountFreeze oldFreeze = freezeMapper.selectById(xid);
        if (oldFreeze != null) {
            log.warn("业务悬挂了，拒绝执行后续逻辑");
            return;
        }
        // 2.扣减可用余额
        accountMapper.deduct(userId, money);
        // 3.记录冻结金额，事务状态
        AccountFreeze freeze = new AccountFreeze();
        freeze.setUserId(userId);
        freeze.setFreezeMoney(money);
        freeze.setState(AccountFreeze.State.TRY);
        freeze.setXid(xid);
        freezeMapper.insert(freeze);
    }

    /**
     * TCC模式下的confirm阶段，也就是删除冻结记录
     *
     * @param ctx TCC上下文对象
     * @return 是否成功
     */
    @Override
    public boolean confirm(BusinessActionContext ctx) {
        // 1.获取事务id
        String xid = ctx.getXid();
        // 2.根据id删除冻结记录
        int count = freezeMapper.deleteById(xid);
        return count == 1;
    }

    /**
     * TCC模式下的cancel阶段，也就是恢复可用余额，并删除冻结记录
     *
     * @param ctx TCC上下文对象
     * @return 是否成功
     */
    @Override
    public boolean cancel(BusinessActionContext ctx) {
        // 0.查询冻结记录
        String xid = ctx.getXid();
        String userId = ctx.getActionContext("userId").toString();

        AccountFreeze freeze = freezeMapper.selectById(xid);

        // 1.空回滚判断：判断freeze是否为null，如果为null，说明没有冻结记录，需要做空回滚
        if (freeze == null) {
            // 证明try阶段没有执行，需要做空回滚记录
            freeze = new AccountFreeze();
            freeze.setUserId(userId);
            freeze.setFreezeMoney(0);
            freeze.setState(AccountFreeze.State.CANCEL);
            freeze.setXid(xid);
            freezeMapper.insert(freeze);
            return true;
        }

        // 2.幂等判断
        if (freeze.getState() == AccountFreeze.State.CANCEL) {
            // 证明之前已经执行过cancel方法了，直接返回true
            return true;
        }

        // 3.恢复可用余额
        accountMapper.refund(freeze.getUserId(), freeze.getFreezeMoney());
        // 4.将冻结金额清零，状态改为CANCEL
        freeze.setFreezeMoney(0);
        freeze.setState(AccountFreeze.State.CANCEL);
        int count = freezeMapper.updateById(freeze);
        return count == 1;
    }
}
