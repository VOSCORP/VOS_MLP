package com.voscorp.vosmlp.messages;

public class LetterView {
    private final String senderName;
    private final String body;
    private final String timeText;
    private final boolean mine;

    public LetterView(String senderName, String body, String timeText, boolean mine) {
        this.senderName = senderName;
        this.body = body;
        this.timeText = timeText;
        this.mine = mine;
    }

    public String getSenderName() { return senderName; }
    public String getBody() { return body; }
    public String getTimeText() { return timeText; }
    public boolean isMine() { return mine; }
}