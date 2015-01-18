$(function() {

   $("#default_encryption_set").change(function() {
      $(this).parent().parent().parent().submit();
   });

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

   $("#button_upload").on("click", function() {
      alert($("#upload_file").files[0].size);
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

   //Initiales laden der Tabellen
   refresh(".table.filesystem tbody", jsRoutes.controllers.Filesystem.jsonFilesList().url);
   refreshNews(".table.news tbody", jsRoutes.controllers.News.jsonNewsList().url);


});

