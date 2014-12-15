$(function() {

   $('input[type=checkbox].multi-checkbox.master').on('click', function() {
      var isChecked = $(this).prop('checked');
      $('input[type=checkbox].multi-checkbox:not(.master)').each(function() {
         $(this).prop('checked', isChecked);
      });
   });

   $('#deleteselected').on('click', function() {
      var values = new Array();
      $('input[type=checkbox].multi-checkbox:not(.master):checked').each(function() {
         values.push($(this).attr('data-value'));
      });
      $.ajax({
         type : 'POST',
         url : $(this).attr("data-url"),
         data : JSON.stringify(values),
         contentType: "application/json; charset=utf-8",
         dataType: "json",
         success : function(data) {
            //setError('Call succedded');
            //$('#test1').attr("src", data)
            alert("ok")
         },
         error : function(data) {
            //setError('Make call failed');
            alert("error")
         }
      });
      return false;
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
});