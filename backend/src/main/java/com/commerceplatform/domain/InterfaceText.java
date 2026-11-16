package com.commerceplatform.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "interface_text")
public class InterfaceText {
    @Id @Column(length = 180) private String id;
    @Column(nullable = false, length = 2000) private String wording;
    protected InterfaceText() {}
    public InterfaceText(String id, String wording) { this.id = id; this.wording = wording; }
    public String getId() { return id; }
    public String getWording() { return wording; }
    public void setWording(String value) { wording = value; }
}
