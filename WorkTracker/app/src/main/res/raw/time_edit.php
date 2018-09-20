<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8">
        <link rel="icon" href="favicon.ico" type="image/x-icon">
        <link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
        <link href="default.css" rel="stylesheet" type="text/css">
        <title>Time Tracker - Editing Time Record</title>
        <script src="js/strftime.js"></script>
        <script>
    Date.ext.locales['en'] = {
      a: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
      p: ['AM', 'PM']
    };
  </script>
        <script src="js/strptime.js"></script>
    </head>

    <body leftmargin="0" topmargin="0" marginheight="0" marginwidth="0" onLoad="fillDropdowns()">


        <table height="100%" cellspacing="0" cellpadding="0" width="100%" border="0">
            <tr>
                <td valign="top" align="center"> <!-- This is to centrally align all our content. -->

                    <!-- top image -->
                    <table cellspacing="0" cellpadding="0" width="100%" border="0">
                        <tr>
                            <td bgcolor="" align="left">
                                <table cellspacing="0" cellpadding="0" width="700" border="0">
                                    <tr>
                                        <td valign="top">
                                            <table cellspacing="0" cellpadding="0" width="100%" border="0">
                                                <tr><td height="6" colspan="2"><img width="1" height="6" src="images/1x1.gif" border="0"></td></tr>
                                                <tr valign="top">
                                                    <td height="55" align="left"><a href="https://www.anuko.com/lp/tt_1.htm" target="_blank"><img alt="Anuko Time Tracker" width=""  height="" src="images/tt_logo.png" border="0"></a></td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                    <!-- end of top image -->

                    <!-- top menu for authorized user -->
                    <table cellspacing="0" cellpadding="3" width="100%" border="0">
                        <tr>
                            <td class="systemMenu" height="17" align="center">&nbsp;
                                <a class="systemMenu" href="logout.php">Logout</a> &middot;
                                <a class="systemMenu" href="profile_edit.php">Profile</a> &middot;
                                <a class="systemMenu" href="https://www.anuko.com/forum/viewforum.php?f=4" target="_blank">Forum</a> &middot;
                                <a class="systemMenu" href="https://www.anuko.com/time_tracker/user_guide/index.htm" target="_blank">Help</a>
                            </td>
                        </tr>
                    </table>
                    <!-- end of top menu for authorized user -->

                    <!-- sub menu for authorized user -->
                    <table cellspacing="0" cellpadding="3" width="100%" border="0">
                        <tr>
                            <td align="center" bgcolor="#d9d9d9" nowrap height="17" background="images/subm_bg.gif">&nbsp;
                                <a class="mainMenu" href="time.php">Time</a>
                                &middot; <a class="mainMenu" href="reports.php">Reports</a>

                                &middot; <a class="mainMenu" href="users.php">Users</a>
                            </td>
                        </tr>
                    </table>
                    <!-- end of sub menu for authorized user -->
                    <br>

                    <!-- page title and user details -->
                    <table cellspacing="0" cellpadding="5" width="720" border="0">
                        <tr><td class="sectionHeader"><div class="pageTitle">Editing Time Record</div></td></tr>
                        <tr><td>Moshe Waisberg - User, Tikal</td></tr>
                    </table>
                    <!-- end of page title and user details -->

                    <!-- output errors -->
                    <!-- end of output errors -->

                    <!-- output messages -->
                    <!-- end of output messages -->

                    <script>
// This script is shared by time.tpl, time_edit.tpl (both regular and mobile),
// and also by WEB-INF/templates/mobile/timer.tpl.
// This creates certain restrictions, such as the form name being "timeRecordForm",
// variables such as $client_list, $project_list and others to be assigned in php files
// for all pages. Things need to be tested together for all involved files.

// We need a few arrays to populate project and task dropdowns.
// When client selection changes, the project dropdown must be re-populated with only relevant projects.
// When project selection changes, the task dropdown must be repopulated similarly.
// Format:
// project_ids[143] = "325,370,390,400";  // Comma-separated list of project ids for client.
// project_names[325] = "Time Tracker";   // Project name.
// task_ids[325] = "100,101,302,303,304"; // Comma-separated list ot task ids for project.
// task_names[100] = "Coding";            // Task name.

// Prepare an array of project ids for clients.
var project_ids = new Array();
// Prepare an array of project names.
var project_names = new Array();
  project_names[486] = "HumanEyes";
  project_names[568] = "KLA";
  project_names[537] = "Nexar";
  project_names[496] = "SideKix";
  project_names[14] = "Tikal";
// We'll use this array to populate project dropdown when client is not selected.
var idx = 0;
var projects = new Array();
  projects[idx] = new Array("486", "HumanEyes");
  idx++;
  projects[idx] = new Array("568", "KLA");
  idx++;
  projects[idx] = new Array("537", "Nexar");
  idx++;
  projects[idx] = new Array("496", "SideKix");
  idx++;
  projects[idx] = new Array("14", "Tikal");
  idx++;

// Prepare an array of task ids for projects.
var task_ids = new Array();
  task_ids[486] = "1,5";
  task_ids[568] = "1,5";
  task_ids[537] = "1,5";
  task_ids[496] = "1,5";
  task_ids[14] = "20,7,5,4,16,9,6,21,13,23,10,12,14,3,11,8";
// Prepare an array of task names.
var task_names = new Array();
  task_names[20] = "Accounting";
  task_names[7] = "Army Service";
  task_names[1] = "Consulting";
  task_names[5] = "Development";
  task_names[4] = "General";
  task_names[16] = "HR";
  task_names[9] = "Illness";
  task_names[6] = "Management";
  task_names[21] = "Marketing";
  task_names[13] = "Meeting";
  task_names[23] = "Personal Absence";
  task_names[10] = "Sales";
  task_names[12] = "Subscription";
  task_names[3] = "Training";
  task_names[11] = "Transport";
  task_names[8] = "Vacation";

// Mandatory top options for project and task dropdowns.
var empty_label_project = "--- select ---";
var empty_label_task = "--- select ---";

// The fillDropdowns function populates the "project" and "task" dropdown controls
// with relevant values.
function fillDropdowns() {
  if(document.body.contains(document.timeRecordForm.client))
    fillProjectDropdown(document.timeRecordForm.client.value);

  fillTaskDropdown(document.timeRecordForm.project.value);
}

// The fillProjectDropdown function populates the project combo box with
// projects associated with a selected client (client id is passed here as id).
function fillProjectDropdown(id) {
  var str_ids = project_ids[id];
  var dropdown = document.getElementById("project");
  // Determine previously selected item.
  var selected_item = dropdown.options[dropdown.selectedIndex].value;

  // Remove existing content.
  dropdown.length = 0;
  var project_reset = true;
  // Add mandatory top option.
  dropdown.options[0] = new Option(empty_label_project, '', true);

  // Populate project dropdown.
  if (!id) {
    // If we are here, client is not selected.
    var len = projects.length;
    for (var i = 0; i < len; i++) {
      dropdown.options[i+1] = new Option(projects[i][1], projects[i][0]);
      if (dropdown.options[i+1].value == selected_item)  {
        dropdown.options[i+1].selected = true;
        project_reset = false;
      }
    }
  } else if (str_ids) {
    var ids = new Array();
    ids = str_ids.split(",");
    var len = ids.length;

    for (var i = 0; i < len; i++) {
      var p_id = ids[i];
      dropdown.options[i+1] = new Option(project_names[p_id], p_id);
      if (dropdown.options[i+1].value == selected_item)  {
        dropdown.options[i+1].selected = true;
        project_reset = false;
      }
    }
  }

  // If project selection was reset - clear the tasks dropdown.
  if (project_reset) {
    dropdown = document.getElementById("task");
    dropdown.length = 0;
    dropdown.options[0] = new Option(empty_label_task, '', true);
  }
}

// The fillTaskDropdown function populates the task combo box with
// tasks associated with a selected project (project id is passed here as id).
function fillTaskDropdown(id) {
  var str_ids = task_ids[id];

  var dropdown = document.getElementById("task");
  if (dropdown == null) return; // Nothing to do.

  // Determine previously selected item.
  var selected_item = dropdown.options[dropdown.selectedIndex].value;

  // Remove existing content.
  dropdown.length = 0;
  // Add mandatory top option.
  dropdown.options[0] = new Option(empty_label_task, '', true);

  // Populate the dropdown from the task_names array.
  if (str_ids) {
    var ids = new Array();
    ids = str_ids.split(",");
    var len = ids.length;

    var idx = 1;
    for (var i = 0; i < len; i++) {
      var t_id = ids[i];
      if (task_names[t_id]) {
        dropdown.options[idx] = new Option(task_names[t_id], t_id);
        idx++;
      }
    }

    // If a previously selected item is still in dropdown - select it.
    if (dropdown.options.length > 0) {
      for (var i = 0; i < dropdown.options.length; i++) {
        if (dropdown.options[i].value == selected_item) {
          dropdown.options[i].selected = true;
        }
      }
    }

    // Select a task if user is required to do so and there is only one task available.
    if (1 && dropdown.options.length == 2) { // 2 because of mandatory top option.
      dropdown.options[1].selected = true;
    }
  }
}

// The formDisable function disables some fields depending on what we have in other fields.
function formDisable(formField) {
  var formFieldValue = eval("document.timeRecordForm." + formField + ".value");
  var formFieldName = eval("document.timeRecordForm." + formField + ".name");
  var x;

  if (((formFieldValue != "") && (formFieldName == "start")) || ((formFieldValue != "") && (formFieldName == "finish"))) {
    x = eval("document.timeRecordForm.duration");
    x.value = "";
    x.disabled = true;
    x.style.background = "#e9e9e9";
  }

  if (((formFieldValue == "") && (formFieldName == "start") && (document.timeRecordForm.finish.value == "")) || ((formFieldValue == "") && (formFieldName == "finish") && (document.timeRecordForm.start.value == ""))) {
    x = eval("document.timeRecordForm.duration");
    x.value = "";
    x.disabled = false;
    x.style.background = "white";
  }

  if ((formFieldValue != "") && (formFieldName == "duration")) {
    x = eval("document.timeRecordForm.start");
    x.value = "";
    x.disabled = true;
    x.style.background = "#e9e9e9";
    x = eval("document.timeRecordForm.finish");
    x.value = "";
    x.disabled = true;
    x.style.background = "#e9e9e9";
  }

  if ((formFieldValue == "") && (formFieldName == "duration")) {
    x = eval("document.timeRecordForm.start");
    x.disabled = false;
    x.style.background = "white";
    x = eval("document.timeRecordForm.finish");
    x.disabled = false;
    x.style.background = "white";
  }
}

// The setNow function fills a given field with current time.
function setNow(formField) {
  var x = eval("document.timeRecordForm.start");
  x.disabled = false;
  x.style.background = "white";
  x = eval("document.timeRecordForm.finish");
  x.disabled = false;
  x.style.background = "white";
  var today = new Date();
  var time_format = '%H:%M';
  var obj = eval("document.timeRecordForm." + formField);
  obj.value = today.strftime(time_format);
  formDisable(formField);
}

function get_date() {
  var date = new Date();
  return date.strftime("%Y-%m-%d");
}

function get_time() {
  var date = new Date();
  return date.strftime("%H:%M");
}
</script>

                    <form name="timeRecordForm" method="post">
                        <table cellspacing="4" cellpadding="7" border="0">
                            <tr>
                                <td>
                                    <table width = "100%">
                                        <tr>
                                            <td valign="top">
                                                <table border="0">
                                                    <tr>
                                                        <td align="right">Project (*):</td>
                                                        <td>
                                                            <select name="project" id="project" onchange="fillTaskDropdown(this.value);" style="width: 250px;">
                                                                <option value="">--- select ---</option>
                                                                <option value="486" selected>HumanEyes</option>
                                                                <option value="568">KLA</option>
                                                                <option value="537">Nexar</option>
                                                                <option value="496">SideKix</option>
                                                                <option value="14">Tikal</option>
                                                            </select></td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Task (*):</td>
                                                        <td>
                                                            <select name="task" id="task" style="width: 250px;">
                                                                <option value="">--- select ---</option>
                                                                <option value="20">Accounting</option>
                                                                <option value="7">Army Service</option>
                                                                <option value="1" selected>Consulting</option>
                                                                <option value="5">Development</option>
                                                                <option value="4">General</option>
                                                                <option value="16">HR</option>
                                                                <option value="9">Illness</option>
                                                                <option value="6">Management</option>
                                                                <option value="21">Marketing</option>
                                                                <option value="13">Meeting</option>
                                                                <option value="23">Personal Absence</option>
                                                                <option value="10">Sales</option>
                                                                <option value="12">Subscription</option>
                                                                <option value="3">Training</option>
                                                                <option value="11">Transport</option>
                                                                <option value="8">Vacation</option>
                                                            </select></td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Start:</td>
                                                        <td>
                                                            <input type="text" id="start" name="start" onchange="formDisable('start');" value="8:58">
                                                            &nbsp;<input onclick="setNow('start');" type="button" tabindex="-1" value="Now"></td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Finish:</td>
                                                        <td>
                                                            <input type="text" id="finish" name="finish" onchange="formDisable('finish');" value="18:32">
                                                            &nbsp;<input onclick="setNow('finish');" type="button" tabindex="-1" value="Now"></td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Duration:</td>
                                                        <td>
                                                            <input type="text" id="duration" name="duration" onchange="formDisable('duration');" value="9:34">
                                                            &nbsp;(hh:mm or 0.0h)</td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Date:</td>
                                                        <td><style>
            .dpDiv {}
            .dpTable {font-family: Tahoma, Arial, Helvetica, sans-serif; font-size: 12px; text-align: center; color: #505050; background-color: #ece9d8; border: 1px solid #AAAAAA;}
            .dpTR {}
            .dpTitleTR {}
            .dpDayTR {}
            .dpTodayButtonTR {}
            .dpTD {border: 1px solid #ece9d8;}
            .dpDayHighlightTD {background-color: #CCCCCC;border: 1px solid #AAAAAA;}
            .dpTDHover {background-color: #aca998;border: 1px solid #888888;cursor: pointer;color: red;}
            .dpTitleTD {}
            .dpButtonTD {}
            .dpTodayButtonTD {}
            .dpDayTD {background-color: #CCCCCC;border: 1px solid #AAAAAA;color: white;}
            .dpTitleText {font-size: 12px;color: gray;font-weight: bold;}
            .dpDayHighlight {color: 4060ff;font-weight: bold;}
            .dpButton {font-family: Verdana, Tahoma, Arial, Helvetica, sans-serif;font-size: 10px;color: gray;background: #d8e8ff;font-weight: bold;padding: 0px;}
            .dpTodayButton {font-family: Verdana, Tahoma, Arial, Helvetica, sans-serif;font-size: 10px;color: gray;  background: #d8e8ff;font-weight: bold;}
            </style>
                                                            <script>
            var datePickerDivID = "datepicker";
            var iFrameDivID = "datepickeriframe";

            var dayArrayShort = new Array('Su','Mo','Tu','We','Th','Fr','Sa');
            var dayArrayMed = new Array('Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat');
            var dayArrayLong = new Array('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday');
            var monthArrayShort = new Array('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec');
            var monthArrayMed = new Array('Jan', 'Feb', 'Mar', 'Apr', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec');
            var monthArrayLong = new Array('January','February','March','April','May','June','July','August','September','October','November','December');

            var defaultDateSeparator = "Y";
            var defaultDateFormat = "%Y-%m-%d";
            var dateSeparator = defaultDateSeparator;
            var dateFormat = defaultDateFormat;
            var startWeek = 0;



            function getStartWeekDayNumber(date) {
              var res = date.getDay() - startWeek;
              if (res < 0) {
                res += 7;
              }
              return res;
            }

            function displayDatePicker(dateFieldName, displayBelowThisObject, dtFormat, dtSep) {
              var targetDateField = document.getElementsByName(dateFieldName).item(0);

              if (!displayBelowThisObject) displayBelowThisObject = targetDateField;
              if (dtSep)
                dateSeparator = dtSep;
              else
                dateSeparator = defaultDateSeparator;

              if (dtFormat)
                dateFormat = dtFormat;
              else
                dateFormat = defaultDateFormat;

              var x = displayBelowThisObject.offsetLeft;
              var y = displayBelowThisObject.offsetTop + displayBelowThisObject.offsetHeight ;

              var parent = displayBelowThisObject;
              while (parent.offsetParent) {
                parent = parent.offsetParent;
                x += parent.offsetLeft;
                y += parent.offsetTop ;
              }

              drawDatePicker(targetDateField, x, y);
            }
            function drawDatePicker(targetDateField, x, y) {
              var dt = getFieldDate(targetDateField.value );

              if (!document.getElementById(datePickerDivID)) {
                var newNode = document.createElement("div");
                newNode.setAttribute("id", datePickerDivID);
                newNode.setAttribute("class", "dpDiv");
                newNode.setAttribute("style", "visibility: hidden;");
                document.body.appendChild(newNode);
              }

              var pickerDiv = document.getElementById(datePickerDivID);
              pickerDiv.style.position = "absolute";
              pickerDiv.style.left = x + "px";
              pickerDiv.style.top = (y + 3) + "px";
              pickerDiv.style.visibility = (pickerDiv.style.visibility == "visible" ? "hidden" : "visible");
              pickerDiv.style.display = (pickerDiv.style.display == "block" ? "none" : "block");
              pickerDiv.style.zIndex = 10000;

              refreshDatePicker(targetDateField.name, dt.getFullYear(), dt.getMonth(), dt.getDate());
            }
            function refreshDatePicker(dateFieldName, year, month, day) {
              var thisDay = new Date();

              if ((month >= 0) && (year > 0)) {
                thisDay = new Date(year, month, 1);
              } else {
                day = thisDay.getDate();
                thisDay.setDate(1);
              }

              var crlf = "\r\n";
              var TABLE = "<table cols=7 class='dpTable'>" + crlf;
              var xTABLE = "</table>" + crlf;
              var TR = "<tr class='dpTR'>";
              var TR_title = "<tr class='dpTitleTR' width='150' align='center'>";
              var TR_days = "<tr class='dpDayTR'>";
              var TR_todaybutton = "<tr class='dpTodayButtonTR'>";
              var xTR = "</tr>" + crlf;
              var TD = "<td class='dpTD' onMouseOut='this.className=\"dpTD\";' onMouseOver=' this.className=\"dpTDHover\";' ";
              var TD_title = "<td colspan=5 class='dpTitleTD'>";
              var TD_buttons = "<td class='dpButtonTD' width='50'>";
              var TD_todaybutton = "<td colspan=7 class='dpTodayButtonTD'>";
              var TD_days = "<td class='dpDayTD'>";
              var TD_selected = "<td class='dpDayHighlightTD' onMouseOut='this.className=\"dpDayHighlightTD\";' onMouseOver='this.className=\"dpTDHover\";' ";
              var xTD = "</td>" + crlf;
              var DIV_title = "<div class='dpTitleText'>";
              var DIV_selected = "<div class='dpDayHighlight'>";
              var xDIV = "</div>";

              var html = TABLE;

              html += TR_title + '<td colspan=7>';
              html += '<table width="250">'+ TR_title;
              html += TD_buttons + getButtonCodeYear(dateFieldName, thisDay, -1, "&lt;&lt;") + getButtonCode(dateFieldName, thisDay, -1, "&lt;") + xTD;
              html += TD_title + DIV_title + monthArrayLong[ thisDay.getMonth()] + " " + thisDay.getFullYear() + xDIV + xTD;
              html += TD_buttons + getButtonCode(dateFieldName, thisDay, 1, "&gt;") + getButtonCodeYear(dateFieldName, thisDay, 1, "&gt;&gt;") + xTD;
              html += xTR + '</table>' + xTD;
              html += xTR;

              html += TR_days;
              for(i = 0; i < dayArrayShort.length; i++)
                html += TD_days + dayArrayShort[(i + startWeek) % 7] + xTD;
              html += xTR;

              html += TR;

              //var startD = (thisDay.getDay()-startWeek<0?6:thisDay.getDay()-startWeek);
              var startD = getStartWeekDayNumber(thisDay);
              for (i = 0; i < startD; i++)
                html += TD + "&nbsp;" + xTD;

              do {
                dayNum = thisDay.getDate();
                TD_onclick = " onclick=\"updateDateField('" + dateFieldName + "', '" + getDateString(thisDay) + "');\">";

                if (dayNum == day)
                  html += TD_selected + TD_onclick + DIV_selected + dayNum + xDIV + xTD;
                else
                  html += TD + TD_onclick + dayNum + xTD;

                var startD = getStartWeekDayNumber(thisDay);

                if (startD == 6)
                  html += xTR + TR;

                thisDay.setDate(thisDay.getDate() + 1);
              } while (thisDay.getDate() > 1)

              var startD = getStartWeekDayNumber(thisDay);
              if (startD > 0) {
                for (i = 6; i >= startD; i--) {
                  html += TD + "&nbsp;" + xTD;
                }
              }
              html += xTR;

              var today = new Date();
              var todayString = "Today is " + dayArrayMed[today.getDay()] + ", " + monthArrayMed[ today.getMonth()] + " " + today.getDate();
              html += TR_todaybutton + TD_todaybutton;
              html += "<button class='dpTodayButton' onClick=\"refreshDatePicker('" + dateFieldName + "'); updateDateFieldOnly('" + dateFieldName + "', '" + getDateString(new Date()) + "');\">Today</button> ";
              html += "<button class='dpTodayButton' onClick='updateDateField(\"" + dateFieldName + "\");'>Close</button>";
              html += xTD + xTR;

              html += xTABLE;

              document.getElementById(datePickerDivID).innerHTML = html;
              adjustiFrame();
            }


            function getButtonCode(dateFieldName, dateVal, adjust, label) {
              var newMonth = (dateVal.getMonth () + adjust) % 12;
              var newYear = dateVal.getFullYear() + parseInt((dateVal.getMonth() + adjust) / 12);
              if (newMonth < 0) {
                newMonth += 12;
                newYear += -1;
              }

              return "<button class='dpButton' onClick='refreshDatePicker(\"" + dateFieldName + "\", " + newYear + ", " + newMonth + ");'>" + label + "</button>";
            }

            function getButtonCodeYear(dateFieldName, dateVal, adjust, label) {
              var newMonth = dateVal.getMonth();
              var newYear = dateVal.getFullYear() + adjust;

              return "<button class='dpButton' onClick='refreshDatePicker(\"" + dateFieldName + "\", " + newYear + ", " + newMonth + ");'>" + label + "</button>";
            }


            function getDateString(dateVal) {
dateVal.locale = "en";
return dateVal.strftime(dateFormat);
            }

            function getFieldDate(dateString) {
              try {
                var dateVal = strptime(dateString, dateFormat);
              } catch(e) {
                dateVal = new Date();
              }
              if (dateVal == null) {
                dateVal = new Date();
              }
              return dateVal;
            }

            function splitDateString(dateString) {
              var dArray;
              if (dateString.indexOf("/") >= 0)
                dArray = dateString.split("/");
              else if (dateString.indexOf(".") >= 0)
                dArray = dateString.split(".");
              else if (dateString.indexOf("-") >= 0)
                dArray = dateString.split("-");
              else if (dateString.indexOf("\\") >= 0)
                dArray = dateString.split("\\");
              else
                dArray = false;

              return dArray;
            }

            function updateDateField(dateFieldName, dateString)  {
              var targetDateField = document.getElementsByName(dateFieldName).item(0);
              if (dateString)
                targetDateField.value = dateString;

              var pickerDiv = document.getElementById(datePickerDivID);
              pickerDiv.style.visibility = "hidden";
              pickerDiv.style.display = "none";

              adjustiFrame();
              targetDateField.focus();

              if ((dateString) && (typeof(datePickerClosed) == "function"))
                datePickerClosed(targetDateField);
            }

            function updateDateFieldOnly(dateFieldName, dateString)  {
              var targetDateField = document.getElementsByName(dateFieldName).item(0);
              if (dateString)
                targetDateField.value = dateString;
            }

            function adjustiFrame(pickerDiv, iFrameDiv) {
              var is_opera = (navigator.userAgent.toLowerCase().indexOf("opera") != -1);
              if (is_opera)
                return;

              try {
                if (!document.getElementById(iFrameDivID)) {
                  var newNode = document.createElement("iFrame");
                  newNode.setAttribute("id", iFrameDivID);
                  newNode.setAttribute("src", "javascript:false;");
                  newNode.setAttribute("scrolling", "no");
                  newNode.setAttribute ("frameborder", "0");
                  document.body.appendChild(newNode);
                }

                if (!pickerDiv)
                  pickerDiv = document.getElementById(datePickerDivID);
                if (!iFrameDiv)
                  iFrameDiv = document.getElementById(iFrameDivID);

                try {
                  iFrameDiv.style.position = "absolute";
                  iFrameDiv.style.width = pickerDiv.offsetWidth;
                  iFrameDiv.style.height = pickerDiv.offsetHeight ;
                  iFrameDiv.style.top = pickerDiv.style.top;
                  iFrameDiv.style.left = pickerDiv.style.left;
                  iFrameDiv.style.zIndex = pickerDiv.style.zIndex - 1;
                  iFrameDiv.style.visibility = pickerDiv.style.visibility ;
                  iFrameDiv.style.display = pickerDiv.style.display;
                } catch(e) {
                }

              } catch (ee) {
              }
            }
</script>

                                                            <input type="text" name="date" id="date" maxlength="50" value="2018-09-17">&nbsp;<img src="/timetracker/images/calendar.gif" width="16" height="16" onclick="displayDatePicker('date');">
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td align="right">Note:</td>
                                                        <td>
                                                            <textarea name="note" id="note" style="width: 250px; height: 200px;"></textarea></td>
                                                    </tr>
                                                    <tr>
                                                        <td colspan="2">&nbsp;</td>
                                                    </tr>
                                                    <tr>
                                                        <td></td>
                                                        <td align="left">
                                                            <input type="submit" name="btn_save" id="btn_save" value="Save" onclick="browser_today.value=get_date()">
                                                            <input type="submit" name="btn_copy" id="btn_copy" value="Copy" onclick="browser_today.value=get_date()">
                                                            <input type="submit" name="btn_delete" id="btn_delete" value="Delete"></td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>


                        <input type="hidden" id="id" name="id" value="289585">

                        <input type="hidden" id="browser_today" name="browser_today" value="">
                    </form>

                </td>
            </tr>
            <tr>
                <td valign="bottom" width="100%" align="center">
                    <p>&nbsp;</p>

                    <table cellspacing="0" cellpadding="0" height="30" border="0" width="100%">
                        <tr>
                            <td width="100%" align="center" bgcolor="#eeeeee"><a href="https://www.anuko.com/lp/tt_8.htm" target="_blank">You can contribute to Time Tracker in different ways.</a></td>
                        </tr>
                    </table>
                    <br>
                    <table cellspacing="0" cellpadding="4" width="100%" border="0">
                        <tr>
                            <td align="center">&nbsp;Anuko Time Tracker 1.17.89.4270 | Copyright &copy; <a href="https://www.anuko.com/lp/tt_3.htm" target="_blank">Anuko</a> |
                                <a href="https://www.anuko.com/lp/tt_4.htm" target="_blank">Credits</a> |
                                <a href="https://www.anuko.com/lp/tt_5.htm" target="_blank">License</a> |
                                <a href="https://www.anuko.com/lp/tt_7.htm" target="_blank">Contribute</a>
                            </td>
                        </tr>
                    </table>
                    <br>

                </td>
            </tr>
        </table>
    </body>
</html>
