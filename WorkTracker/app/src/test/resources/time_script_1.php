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
  project_names[584] = "Playstudios";
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
  projects[idx] = new Array("584", "Playstudios");
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
  task_ids[584] = "1,5";
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
