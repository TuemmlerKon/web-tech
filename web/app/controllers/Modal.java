package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;


public class Modal extends Controller {

    public static Logger.ALogger logger = Logger.of("application.controller.account");

    public static Result newFolder() {
        return ok(views.html.modal.newfolder.render());
    }

    public static Result newFile() {
        return ok(views.html.modal.newfile.render());
    }

    public static Result rmAccount() {
        return ok(views.html.modal.rmaccount.render());
    }

    public static Result newNews() {
        return ok(views.html.modal.newnews.render());
    }

    public static Result multiRm() {
        return ok(views.html.modal.multirm.render());
    }
}