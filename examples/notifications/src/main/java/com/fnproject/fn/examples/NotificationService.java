package com.fnproject.fn.examples;

import com.fnproject.events.input.NotificationMessage;

public class NotificationService {

    public void readNotification(NotificationMessage<Employee> notification) {
        System.out.println(notification);
    }
}
