package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;


public class Modal extends Controller {

    public static Logger.ALogger logger = Logger.of("application.controller.account");

    public static Result newFolder() {
        ObjectNode result = Json.newObject();
        result.put("body", views.html.modal.newfolder.render().toString());
        return ok(result);
    }

    public static Result newFile() {
        ObjectNode result = Json.newObject();
        result.put("body", views.html.modal.newfile.render().toString());
        return ok(result);
    }

    public static Result rmAccount() {
        ObjectNode result = Json.newObject();
        result.put("body", views.html.modal.rmaccount.render().toString());
        return ok(result);
    }

}