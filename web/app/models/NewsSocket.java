package models;

import akka.actor.UntypedActor;
import com.avaje.ebean.Ebean;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.List;

public class NewsSocket extends UntypedActor {
    WebSocket.In<String> in;
    WebSocket.Out<String> out;

    public NewsSocket(WebSocket.In<String> in, WebSocket.Out<String> out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void onReceive(Object message) {
        if (message.equals("News")) {
            List<News> news = News.find.setMaxRows(5).orderBy("date desc").findList();
            out.write(Json.toJson(news).toString());
        } else {
            unhandled(message);
        }
    }
}