package de.danieldeusing.stellar.tokengen.impl;

public class TokenDescImpl {
    private String code;
    private String name;
    private String description;
    private String conditions;

    public TokenDescImpl(String code, String name, String description, String conditions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditions = conditions;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }
}
