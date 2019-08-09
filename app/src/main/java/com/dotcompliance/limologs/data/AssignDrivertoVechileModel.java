package com.dotcompliance.limologs.data;

/**
 * Created by ADMIN on 26-10-2017.
 */

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AssignDrivertoVechileModel {

    @SerializedName("error")
    @Expose
    private Integer error;
    @SerializedName("messages")
    @Expose
    private String messages;
    @SerializedName("message")
    @Expose
    private String message;

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}