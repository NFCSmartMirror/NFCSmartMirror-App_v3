var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

showDateAndTime();


window.setInterval(function() {
  // var myDate = new Date().toTimeString().replace(/.*(\d{2}:\d{2}:\d{2}).*/, "$1");
  // $('#center').html(myDate);
  showDateAndTime();
}, 1000);


function showDateAndTime(){
  var dateNow = new Date();
  var month = months[dateNow.getMonth()];
  var date = dateNow.getDate();
  var year = dateNow.getFullYear();

  var datum = date + ', ' + month + ' ' + year;
  var time = dateNow.toTimeString().replace(/.*(\d{2}:\d{2}:\d{2}).*/, "$1");
  var msg = '<div class="date">' + datum + '</div>';
  msg += '<div class="time">' + time + '</div>';
  $('#content').html(msg);
}