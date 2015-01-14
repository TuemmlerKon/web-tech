package controllers;


import play.mvc.Controller;
import play.mvc.Result;

public class jsMessage extends Controller {

    final static jsmessages.JsMessages messages = jsmessages.JsMessages.create(play.Play.application());

    public static Result get() {
        return ok(messages.generate("window.Messages"));
    }

}