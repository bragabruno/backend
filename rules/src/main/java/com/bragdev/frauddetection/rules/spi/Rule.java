package com.bragdev.frauddetection.rules.spi;

import com.bragdev.frauddetection.common.model.Transaction;

public interface Rule {

    String name();

    RuleOutcome evaluate(Transaction transaction, RuleContext context);
}
