package models;

import akka.actor.UntypedActor;
import controllers.Filesystem;
import play.libs.Json;
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

    public void onReceive(Object message) {
        if (message.equals("Files")) {
            if(user != null) {
                List<File> files = File.find.where().eq("filetype", Filesystem.FILETYPE_FILE).eq("owner",user.getId()).setMaxRows(8).orderBy("create_date desc").findList();
                out.write(Json.toJson(files).toString());
            }
        } else {
            unhandled(message);
        }
    }
}