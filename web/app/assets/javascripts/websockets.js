$(function() {

   var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
   //Socketanfrage für die Newsliste auf der Startseite
   var newsSocket = new WS(jsRoutes.controllers.News.newsWS().webSocketURL());
   var nreceiveEvent = function(event) {
      var news = JSON.parse(event.data);
      var body = $('.panel-body.news');
      body.html("");
      for(i=0;i<news.length;i++) {
         var date = new Date(news[i]['date']);
         var date_str = pad(date.getDate())+"."+pad(date.getMonth()+1)+"."+date.getFullYear();
         var time_str = pad(date.getHours())+":"+pad(date.getMinutes());
         body.append('<p>'+Messages("application.dashboard.newstext", date_str, time_str, news[i]['owner'], news[i]['name'], news[i]['text'])+'</p>');
      }
      //wenn keine Daten vorhanden sind
      if(news.length == 0) {
         body.html('<div class="loader text-muted">'+Messages("application.dashboard.nonewsavailable")+'</div>')
      }
   };
   newsSocket.onmessage = nreceiveEvent;


   //Socketanfrage für die Dateiliste auf der Startseite
   var WS2 = window['MozWebSocket'] ? MozWebSocket : WebSocket;
   var filesocket = new WS2(jsRoutes.controllers.Filesystem.filesWS().webSocketURL());
   var freceiveEvent = function(event) {
      var files = JSON.parse(event.data);
      var body = $('.panel-body.files tbody');
      body.html("");
      for(i=0;i<files.length;i++) {
         var date = new Date(files[i]['createDate']);
         var date_str = pad(date.getDate())+"."+pad(date.getMonth()+1)+"."+date.getFullYear()+" "+pad(date.getHours())+":"+pad(date.getMinutes());
         var val = "<tr>"+
                   "<td>"+date_str+" "+Messages("user.files.clock")+"</td>" +
                   "<td><i class=\"fa fa-file\"></i> "+files[i]['filename']+"</td>" +
                   "<td>"+bytesToSize(files[i]['size'])+"</td>" +
                   "<td>"+files[i]['service']+"</td>" +
                   "</tr>";
         body.append(val);
      }
      //wenn keine Daten vorhanden sind
      if(files.length == 0) {
         body.html('<tr><td colspan="4" class="loader text-muted">'+Messages("application.dashboard.nofilesavailable")+'</td></tr>')
      }
   };
   filesocket.onmessage = freceiveEvent;

   //*//Socketanfrage für die Userdaten
   var WS3 = window['MozWebSocket'] ? MozWebSocket : WebSocket;
   var usersocket = new WS3(jsRoutes.controllers.Account.userWS().webSocketURL());
   var ureceiveEvent = function(event) {
      var user = JSON.parse(event.data);
      if(user) {
         var user = JSON.parse(event.data);
         var bar = $("#storage-bar");
         var percent = (user['used']/(user['storage']/100)).toFixed(2);

         var bar_class = '';
         if (percent <= 80) bar_class = "progress-bar-success";
         if (percent > 80) bar_class = "progress-bar-warning";
         if (percent > 95) bar_class = "progress-bar-danger";

         //Inhalt löschen
         bar.html("");
         var content = '<div class="progress">' +
                       '<div class="progress-bar '+bar_class+' progress-bar-striped" role="progressbar" aria-valuenow="'+percent+'" aria-valuemin="0" aria-valuemax="100" style="width: '+Math.floor(percent)+'%;">'+percent+'%</div>' +
                       '</div>' +
                       '<p>'+Messages("application.dashboard.usage", bytesToSize(user['used']), percent, bytesToSize(user['storage']))+'</p>';
         bar.html(content);
      }
   };
   usersocket.onmessage = ureceiveEvent;//*/

});