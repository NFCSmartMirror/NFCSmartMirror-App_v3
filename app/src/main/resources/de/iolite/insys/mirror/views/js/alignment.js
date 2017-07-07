
function getParameterByName(name) {
  name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
    results = regex.exec(location.search);
  return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}


var row = getParameterByName('mirror_rows');
var col = getParameterByName('mirror_columns');
var pos = getParameterByName('mirror_pos');



pos_col = pos % col;
pos_row = parseInt (pos / row);
console.log(pos_col + " " + pos_row);

if (pos){
  var pos_class = "center";
  if (pos_col == 0 && pos_row == 0){
    // upper-left
    pos_class = "upper-left";
    console.log(pos_class);
  } else if (pos_col == col - 1 && pos_row == 0) {
    // upper-right
    pos_class = "upper-right";
    console.log(pos_class);
  } else if (pos_col == 0 && pos_row == row - 1) {
    // bottom-left
    pos_class = "bottom-left";
    console.log(pos_class);
  } else if (pos_col == col - 1 && pos_row == row - 1) {
    // bottom-right
    pos_class = "bottom-right";
    console.log(pos_class);
  }
  $('#content').addClass(pos_class);
}
