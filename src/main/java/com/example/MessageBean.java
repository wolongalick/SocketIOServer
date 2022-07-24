package com.example;

import java.io.Serializable;

/**
 * @author 崔兴旺
 * @description
 * @date 2022/7/4 2:05
 */
public class MessageBean implements Serializable {
    public String name;
    public String message;

    public MessageBean() {
    }

    public MessageBean(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public String toString() {
        return "MessageBean{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}