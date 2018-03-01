package de.danieldeusing.stellar.tokengen.interfaces;

import de.danieldeusing.stellar.tokengen.types.AccountType;
import org.stellar.sdk.KeyPair;

public interface IAccount {
    String getName();
    AccountType getAccountType();
    KeyPair getKeyPair();
}
