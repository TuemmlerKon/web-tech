package models;

import akka.actor.UntypedActor;
import controllers.Account;
import controllers.Filesystem;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.List;

public class UserSocket extends UntypedActor {
    WebSocket.In<String> in;
    WebSocket.Out<String> out;
    Long userid;

    public UserSocket(WebSocket.In<String> in, WebSocket.Out<String> out, Long userid) {
        this.in = in;
        this.out = out;
        this.userid = userid;
    }

    public void onReceive(Object message) {
        if (message.equals("User")) {
            User user = Account.getById(userid);
            out.write(Json.toJson(user).toString());
        } else {
            unhandled(message);
        }
    }
}