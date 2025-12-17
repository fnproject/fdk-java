package com.fnproject.fn.examples;

import com.fnproject.events.NotificationFunction;
import com.fnproject.events.input.NotificationMessage;

public class Function extends NotificationFunction<Employee> {

    public NotificationService notificationService;

    public Function() {
        this.notificationService = new NotificationService();
    }

    @Override
    public void handler(NotificationMessage<Employee> content) {
        notificationService.readNotification(content);
    }
}