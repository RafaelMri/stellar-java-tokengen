package de.danieldeusing.stellar.tokengen.impl;

public class TokenDescImpl {
    private String code;
    private String name;
    private String description;
    private String conditions;
    private String imgURL;
    private String issuerPub;
    private String decimals;

    public TokenDescImpl(String code, String name, String description, String conditions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditions = conditions;
    }

    public TokenDescImpl(String code, String name, String description, String conditions, String imgURI, String issuerPub, String decimals) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditions = conditions;
        this.imgURL = imgURI;
        this.issuerPub = issuerPub;
        this.decimals = decimals;
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

    public String getImgURL() {
        return imgURL;
    }

    public void setImgURL(String imgURL) {
        this.imgURL = imgURL;
    }

    public String getIssuerPub() {
        return issuerPub;
    }

    public void setIssuerPub(String issuerPub) {
        this.issuerPub = issuerPub;
    }

    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(String decimals) {
        this.decimals = decimals;
    }
}
