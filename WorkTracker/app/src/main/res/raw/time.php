<html>
    <head>
      <meta http-equiv="content-type" content="text/html; charset=utf-8">
      <link rel="icon" href="favicon.ico" type="image/x-icon">
      <link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
      <link href="default.css" rel="stylesheet" type="text/css">
      <title>Time Tracker - Time</title>
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
            <tr><td class="sectionHeader"><div class="pageTitle">Time: 2018-06-19</div></td></tr>
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
    <style>
    .not_billable td {
      color: #ff6666;
    }
    </style>
    <form name="timeRecordForm" method="post">
    <table cellspacing="4" cellpadding="0" border="0">
      <tr>
        <td valign="top">
          <table>
            <tr>
              <td align="right">Project (*):</td>
              <td>
    	<select name="project" id="project" onchange="fillTaskDropdown(this.value);" style="width: 250px;">
    <option value="">--- select ---</option>
    <option value="486">HumanEyes</option>
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
    <option value="1">Consulting</option>
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
    	<input type="text" id="start" name="start" onchange="formDisable('start');" value="">
    &nbsp;<input onclick="setNow('start');" type="button" tabindex="-1" value="Now"></td>
            </tr>
            <tr>
              <td align="right">Finish:</td>
              <td>
    	<input type="text" id="finish" name="finish" onchange="formDisable('finish');" value="">
    &nbsp;<input onclick="setNow('finish');" type="button" tabindex="-1" value="Now"></td>
            </tr>
            <tr>
              <td align="right">Duration:</td>
              <td>
    	<input type="text" id="duration" name="duration" onchange="formDisable('duration');" value="">
    &nbsp;(hh:mm or 0.0h)</td>
            </tr>
          </table>
        </td>
        <td valign="top">
          <table>
            <tr><td><style>
    .CalendarHeader {padding: 5px; font-size: 8pt; color: #333333; background-color: #d9d9d9;}
    .CalendarDay {padding: 5px; border: 1px solid silver; font-size: 8pt; color: #333333; background-color: #ffffff;}
    .CalendarDaySelected {padding: 5px; border: 1px solid silver; font-size: 8pt; color: #666666; background-color: #a6ccf7;}
    .CalendarDayWeekend {padding: 5px; border: 1px solid silver; font-size: 8pt; color: #666666; background-color: #f7f7f7;}
    .CalendarDayHoliday {padding: 5px; border: 1px solid silver; font-size: 8pt; color: #666666; background-color: #f7f7f7;}
    .CalendarDayHeader {padding: 5px; border: 1px solid white; font-size: 8pt; color: #333333;}
    .CalendarDayHeaderWeekend {padding: 5px; border: 1px solid white; font-size: 8pt; color: #999999;}
    .CalendarLinkWeekend {color: #999999;}
    .CalendarLinkHoliday {color: #999999;}
    .CalendarLinkRecordsExist {color: #FF0000;}
    </style>
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr><td align="center"><div class="CalendarHeader"><a href="?date=2018-05-01" tabindex="-1">&lt;&lt;&lt;</a>  June&nbsp;2018  <a href="?date=2018-07-01" tabindex="-1">&gt;&gt;&gt;</a></div></td></tr>
              </table><center>
              <table border="0" cellpadding="1" cellspacing="1" width="100%">
              <tr><tr><td class="CalendarDayHeader">Su</td><td class="CalendarDayHeader">Mo</td><td class="CalendarDayHeader">Tu</td><td class="CalendarDayHeader">We</td><td class="CalendarDayHeader">Th</td><td class="CalendarDayHeaderWeekend">Fr</td><td class="CalendarDayHeaderWeekend">Sa</td></tr>
    <TR>
    <TD>&nbsp;</TD>
    <TD>&nbsp;</TD>
    <TD>&nbsp;</TD>
    <TD>&nbsp;</TD>
    <TD>&nbsp;</TD>
    <td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-01" tabindex="-1">01</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-02" tabindex="-1">02</a></TD></TR>
    <TR>
    <td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-03" tabindex="-1">03</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-04" tabindex="-1">04</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-05" tabindex="-1">05</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-06" tabindex="-1">06</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-07" tabindex="-1">07</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-08" tabindex="-1">08</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-09" tabindex="-1">09</a></TD></TR>
    <TR>
    <td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-10" tabindex="-1">10</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-11" tabindex="-1">11</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-12" tabindex="-1">12</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-13" tabindex="-1">13</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-14" tabindex="-1">14</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-15" tabindex="-1">15</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-16" tabindex="-1">16</a></TD></TR>
    <TR>
    <td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-17" tabindex="-1">17</a></TD><td class="CalendarDay"><a class="CalendarLinkRecordsExist" href="?date=2018-06-18" tabindex="-1">18</a></TD><td class="CalendarDaySelected"><a href="?date=2018-06-19" tabindex="-1">19</a></TD><td class="CalendarDay"><a href="?date=2018-06-20" tabindex="-1">20</a></TD><td class="CalendarDay"><a href="?date=2018-06-21" tabindex="-1">21</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-22" tabindex="-1">22</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-23" tabindex="-1">23</a></TD></TR>
    <TR>
    <td class="CalendarDay"><a href="?date=2018-06-24" tabindex="-1">24</a></TD><td class="CalendarDay"><a href="?date=2018-06-25" tabindex="-1">25</a></TD><td class="CalendarDay"><a href="?date=2018-06-26" tabindex="-1">26</a></TD><td class="CalendarDay"><a href="?date=2018-06-27" tabindex="-1">27</a></TD><td class="CalendarDay"><a href="?date=2018-06-28" tabindex="-1">28</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-29" tabindex="-1">29</a></TD><td class="CalendarDayWeekend"><a class="CalendarLinkWeekend" href="?date=2018-06-30" tabindex="-1">30</a></TD></TR>
    <tr><td colspan="7" align="center"><a id="today_link" href="?date=2018-06-19" tabindex="-1">Today</a></td></tr>
    </table>
    <input type="hidden" name="date" value="2018-06-19">
    <script>
    function adjustToday() {
      var browser_today = new Date();
      document.getElementById('today_link').href = '?date='+browser_today.strftime('%Y-%m-%d');
    }
    adjustToday();
    </script>
    </td></tr>
          </table>
        </td>
      </tr>
    </table>
    <table>
      <tr>
        <td align="right">Note:</td>
        <td align="left">
    	<textarea name="note" id="note" style="width: 600px; height:40px;"></textarea></td>
      </tr>
      <tr>
        <td align="center" colspan="2">
    	<input type="submit" name="btn_submit" id="btn_submit" value="Submit" onclick="browser_today.value=get_date()"></td>
      </tr>
    </table>
    <table width="720">
    <tr>
      <td valign="top">
      </td>
    </tr>
    </table>
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