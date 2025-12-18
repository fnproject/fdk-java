package com.fnproject.events.testfns.notification;

import com.fnproject.events.NotificationFunction;
import com.fnproject.events.input.NotificationMessage;
import com.fnproject.events.testfns.Animal;

public class NotificationObjectTestFunction extends NotificationFunction<Animal> {

    @Override
    public void handler(NotificationMessage<Animal> batch) {

    }
}
