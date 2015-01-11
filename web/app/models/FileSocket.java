package models;

import akka.actor.UntypedActor;
import com.avaje.ebean.Ebean;
import controllers.Account;
import controllers.Application;
import controllers.Filesystem;
import play.Logger;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.WebSocket;

import java.util.List;

public class FileSocket extends UntypedActor {
    WebSocket.In<String> in;
    WebSocket.Out<String> out;
    User user;

    public FileSocket(WebSocket.In<String> in, WebSocket.Out<String> out, User user) {
        this.in = in;
        this.out = out;
        this.user = user;
    }

    @Override
    public void onReceive(Object message) {
        if (message.equals("Files")) {
            List<File> files = File.find.where().eq("filetype", Filesystem.FILETYPE_FILE).eq("owner",user.getId()).setMaxRows(8).orderBy("create_date desc").findList();
            out.write(Json.toJson(files).toString());
        } else {
            unhandled(message);
        }
    }
}