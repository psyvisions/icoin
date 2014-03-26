package com.icoin.trading.api.fee.command.received;

import com.icoin.trading.api.fee.command.ConfirmFeeCommand;
import com.icoin.trading.api.fee.domain.fee.FeeId;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: liougehooa
 * Date: 14-3-19
 * Time: AM12:28
 * To change this template use File | Settings | File Templates.
 */
public class ConfirmReceivedFeeCommand extends ConfirmFeeCommand<ConfirmReceivedFeeCommand> {
    public ConfirmReceivedFeeCommand(FeeId feeId, Date confirmedDate) {
        super(feeId, confirmedDate);
    }
}