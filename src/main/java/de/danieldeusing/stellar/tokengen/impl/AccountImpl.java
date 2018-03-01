package de.danieldeusing.stellar.tokengen.impl;

import de.danieldeusing.stellar.tokengen.interfaces.IAccount;
import de.danieldeusing.stellar.tokengen.types.AccountType;
import org.stellar.sdk.KeyPair;

public class AccountImpl implements IAccount {
    private String name;
    private AccountType type;
    private KeyPair keyPair;

    public AccountImpl(AccountType type, KeyPair keyPair) {
        this.type = type;
        this.keyPair = keyPair;
    }

    public AccountImpl(String name, AccountType type, KeyPair keyPair) {
        this.name = name;
        this.type = type;
        this.keyPair = keyPair;
    }

    @Override
    public String getName() {
        return name;
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
