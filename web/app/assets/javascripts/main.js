$(function() {

   function pad(n) {return (n<10 ? '0'+n : n);}

   function bytesToSize(bytes) {
      var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
      if (bytes == 0) return 'n/a';
      var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
      return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
   };


   $('input[type=checkbox].multi-checkbox.master').on('click', function() {
      var isChecked = $(this).prop('checked');
      $('input[type=checkbox].multi-checkbox:not(.master)').each(function() {
         $(this).prop('checked', isChecked);
      });
   });

   $('.modal-action').on('click', function() {
      $.ajax({
         type : 'GET',
         url : $(this).attr('data-requesturl'),
         success : function(data) {
            $(".modal-content").html(data['body']);
         },
         error : function(data) {
            alert("failed");
         }
      });
      $('#myModal').modal();
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
      $('.panel-body.news').html("");
      for(i=0;i<news.length;i++) {
         var date = new Date(news[i]['date']);
         $('.panel-body.news').append('<p><span class="text-muted">'+pad(date.getDate())+"."+pad(date.getMonth())+"."+date.getFullYear()+" "+pad(date.getHours())+":"+pad(date.getMinutes())+' Uhr</span>: <strong>'+news[i]['name']+'</strong> '+news[i]['text']+'</p>');
      }
   };
   newsSocket.onmessage = nreceiveEvent

   var filesocket = new WS2("ws://"+window.location.host+"/files/updates");
   var freceiveEvent = function(event) {
      var files = JSON.parse(event.data);
      $('.panel-body.files tbody').html("");
      for(i=0;i<files.length;i++) {
         var date = new Date(files[i]['createDate'])
         var val = "<tr>"+
                   "<td>"+pad(date.getDate())+"."+pad(date.getMonth())+"."+date.getFullYear()+" "+pad(date.getHours())+":"+pad(date.getMinutes())+" Uhr</td>" +
                   "<td>"+files[i]['filename']+"</td>" +
                   "<td>"+bytesToSize(files[i]['size'])+"</td>" +
                   "<td>"+files[i]['service']+"</td>" +
                   "</tr>";
         $('.panel-body.files tbody').append(val);
      }
   };
   filesocket.onmessage = freceiveEvent



});