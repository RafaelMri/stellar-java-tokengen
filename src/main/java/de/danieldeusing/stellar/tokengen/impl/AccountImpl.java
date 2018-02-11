package de.danieldeusing.stellar.tokengen.impl;

import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import org.stellar.sdk.KeyPair;

public class AccountImpl implements IAccount {
    private AccountType type;
    private KeyPair keyPair;

    public AccountImpl(AccountType type, KeyPair keyPair) {
        this.type = type;
        this.keyPair = keyPair;
    }

    @Override
    public AccountType getAccountType() {
        return type;
    }

    @Override
    public KeyPair getKeyPair() {
        return keyPair;
    }
}
