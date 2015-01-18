
function pad(n) {return (n<10 ? '0'+n : n);}

function bytesToSize(bytes) {
   var sizes = ['Bytes', 'KByte', 'MByte', 'GByte', 'TByte'];
   if (bytes == 0) return '0 Bytes';
   var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
   return (bytes / Math.pow(1024, i)).toFixed(2).replace('.',',') + ' ' + sizes[i];
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
               var filename = d['filename'];
               if(d['key'] != null) {
                  filename += " <i class=\"fa fa-lock\" title=\""+Messages("filesystem.fileencrypted")+"\"></i>"
               }

               value += '<td class="file-col"><a href="'+jsRoutes.controllers.Filesystem.download(d['id']).url+'">'+filename+'</a></td>';
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