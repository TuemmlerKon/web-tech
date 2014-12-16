package models;

import akka.actor.UntypedActor;
import com.avaje.ebean.Ebean;
import controllers.Filesystem;
import play.libs.Json;
import play.mvc.WebSocket;

import java.util.List;

public class FileSocket extends UntypedActor {
    WebSocket.In<String> in;
    WebSocket.Out<String> out;

    public FileSocket(WebSocket.In<String> in, WebSocket.Out<String> out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void onReceive(Object message) {
        if (message.equals("Files")) {
            List<File> files = File.find.where().eq("filetype", Filesystem.FILETYPE_FILE).setMaxRows(8).orderBy("create_date desc").findList();
            out.write(Json.toJson(files).toString());
        } else {
            unhandled(message);
        }
    }
}