package com.example.edgesum.page.main;

public enum ActionButton {
    ADD("Add"),
    REMOVE("Remove"),
    UPLOAD("Upload");

    private final String text;

    ActionButton(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
