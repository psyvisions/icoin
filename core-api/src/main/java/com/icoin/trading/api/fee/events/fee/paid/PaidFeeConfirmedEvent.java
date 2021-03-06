package com.icoin.trading.api.fee.events.fee.paid;

import com.icoin.trading.api.fee.domain.fee.FeeId;
import com.icoin.trading.api.fee.events.fee.FeeConfirmedEvent;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: liougehooa
 * Date: 14-3-18
 * Time: PM9:19
 * To change this template use File | Settings | File Templates.
 */
public class PaidFeeConfirmedEvent extends FeeConfirmedEvent<PaidFeeConfirmedEvent> {
    private String sequenceNumber;

    public PaidFeeConfirmedEvent(FeeId feeId, String sequenceNumber, Date confirmedDate) {
        super(feeId, confirmedDate);
        this.sequenceNumber = sequenceNumber;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }
}