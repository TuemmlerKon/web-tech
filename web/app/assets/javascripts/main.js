
function pad(n) {return (n<10 ? '0'+n : n);}

function bytesToSize(bytes) {
   var sizes = ['Bytes', 'KByte', 'MByte', 'GByte', 'TByte'];
   if (bytes == 0) return '0 Bytes';
   var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
   return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i];
}

function refresh(selector, data_source) {
   var body = $(selector);
   body.html("");
   $.ajax({
      type : 'GET',
      url : data_source,
      success : function(data) {
         for(i=0;i<data.length;i++) {
            var d = data[i];
            var value = '<tr>';

            value += '<td><input type="checkbox" class="multi-checkbox" data-value="'+d['id']+'" data-text="'+d['filename']+'"></td>';

            if(d['filetype'] == 'folder') {
               value += '<td class="folder-col"><a href="'+jsRoutes.controllers.Filesystem.cwd(d['id']).url+'">'+d['filename']+'</a></td>';
            } else {
               value += '<td class="file-col"><a href="'+jsRoutes.controllers.Filesystem.download(d['id']).url+'">'+d['filename']+'</a></td>';
            }
            var date = new Date(d['createDate']);
            value += "<td>"+pad(date.getDate())+"."+pad(date.getMonth()+1)+"."+date.getFullYear()+" "+pad(date.getHours())+":"+pad(date.getMinutes())+" "+Messages("user.files.clock")+"</td>"
            if(d['filetype'] == 'folder') {
               value += '<td><i class="fa fa-folder"></i></td>';
            } else {
               value += '<td><i class="fa fa-file"></i> '+bytesToSize(d['size'])+'</td>';
            }

            value += '<td>'+d['service']+'</td>';
            //Prüfen ob wir einen Ordner oder eine Datei vor uns haben
            if(d['filetype'] == 'folder') {
               value += '<td><a href="'+jsRoutes.controllers.Filesystem.deleteFolder(d['id']).url+'" title="löschen"><span class="fa fa-remove"></span></a></td>';
            } else {
               value += '<td><a href="'+jsRoutes.controllers.Filesystem.deleteFile(d['id']).url+'" title="löschen"><span class="fa fa-remove"></span></a></td>';
            }



            value += '</tr>';
            body.append(value);
         }

         if(data.length == 0) {
            body.append("<tr><td colspan=\"6\"><div class='loader'><span class=\"text-muted\">"+Messages("application.dashboard.nofilesavailable")+"</span></div></td></tr>");
         }

      },
      error : function(data) {
         body.html("<tr><td colspan=\"6\"><div class='loader'><span class=\"text-muted\">"+Messages("application.dashboard.fileserror")+"</span></div></td></tr>");
      }
   });
}

function refreshNews(selector, data_source) {
   var body = $(selector);
   body.html("");
   $.ajax({
      type : 'GET',
      url : data_source,
      success : function(data) {
         for(i=0;i<data.length;i++) {
            var d = data[i];
            var value = '<tr>';

            value += '<td><input type="checkbox" class="multi-checkbox" data-value="'+d['id']+'" data-text="'+d['name']+'"></td>';
            value += "<td>"+d['name']+"</td>";
            value += "<td>"+d['text']+"</td>";
            var date = new Date(d['date']);
            value += "<td>"+pad(date.getDate())+"."+pad(date.getMonth()+1)+"."+date.getFullYear()+" "+pad(date.getHours())+":"+pad(date.getMinutes())+" "+Messages("user.files.clock")+"</td>"
            value += '<td><a href="'+jsRoutes.controllers.News.rm(d['id']).url+'" title="\''+d['name']+'\' löschen"><span class="fa fa-remove"></span></a></td>';

            value += '</tr>';
            body.append(value);
         }

         if(data.length == 0) {
            body.append("<tr><td colspan=\"6\"><div class='loader'><span class=\"text-muted\">"+Messages("application.dashboard.nonewsavailable")+"</span></div></td></tr>");
         }

      },
      error : function(data) {
         body.html("<tr><td colspan=\"6\"><div class='loader'><span class=\"text-muted\">"+Messages("application.dashboard.newserror")+"</span></div></td></tr>");
      }
   });
}

$(function() {

   $('input[type=checkbox].multi-checkbox.master').on('click', function() {
      var isChecked = $(this).prop('checked');
      $('input[type=checkbox].multi-checkbox:not(.master)').each(function() {
         $(this).prop('checked', isChecked);
      });
   });

   $('#deleteselected').on('click', function() {
      var rmbutton = $(this);
      var str_files = "";
      var str_hidden = "";
      var ids = new Array();
      var buttons = $('input[type=checkbox].multi-checkbox:not(.master):checked');

      buttons.each(function() {
         str_files  += '<li>'+$(this).attr('data-text')+'</li>';
         str_hidden += '<input type="hidden" name="ids[]" value="'+$(this).attr("data-value")+'">';
      });

      $.ajax({
         type : 'GET',
         url : rmbutton.attr('data-requesturl'),
         success : function(data) {

            if(str_files == "") {
               str_files = Messages("application.multirm.noentriesselected");
            } else {
               str_files = "<ul>"+str_files+"</ul>";
            }

            var val =
            $(".modal-content").html(data.replace("%PLACEHOLDER_FOR_FILES%", str_files)
                                                 .replace("%PLACEHOLDER_FOR_HIDDEN_INPUTS%", str_hidden)
                                                 .replace("%PLACEHOLDER_RM_ACTIONURL%", rmbutton.attr('data-deleteurl')));
            $('#myModal').modal();
         }
      });

   });

   $('.modal-action').on('click', function() {
      $.ajax({
         type : 'GET',
         url : $(this).attr('data-requesturl'),
         success : function(data) {
            $(".modal-content").html(data);
            $('#myModal').modal();
         }
      });
   });

   $('.input-group.password .glyphicon-eye-open').on('click', function () {

      var child = $(this).parent().find('input.form-control');

      if(child.attr('type') == 'password') {
         child.attr('type', 'text');
         $(this).removeClass('glyphicon-eye-open');
         $(this).addClass('glyphicon-eye-close');
      } else {
         child.attr('type', 'password');
         $(this).removeClass('glyphicon-eye-close');
         $(this).addClass('glyphicon-eye-open');
      }
   });


   $('.panel-heading').on('click', function() {
      $(this).parent().find(".panel-body").fadeToggle();
   });

   var WS = WS2 = window['MozWebSocket'] ? MozWebSocket : WebSocket;

   var newsSocket = new WS("ws://"+window.location.host+"/news/updates");
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
         body.html('<div class="loader text-muted">Messages("application.dashboard.nonewsavailable")</div>')
      }
   };
   newsSocket.onmessage = nreceiveEvent;

   var filesocket = new WS2("ws://"+window.location.host+"/files/updates");
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
         body.html('<tr><td colspan="4" class="loader text-muted">Messages("application.dashboard.nofilesavailable")</td></tr>')
      }
   };
   filesocket.onmessage = freceiveEvent;

   //Initiales laden der Tabellen
   refresh(".table.filesystem tbody", jsRoutes.controllers.Filesystem.jsonFilesList().url);
   refreshNews(".table.news tbody", jsRoutes.controllers.News.jsonNewsList().url);

   var info = $('#storage-info');
   info.html(Messages("application.dashboard.usage", bytesToSize(info.attr('data-used')), info.attr('data-percent'), bytesToSize(info.attr('data-available'))));

});

