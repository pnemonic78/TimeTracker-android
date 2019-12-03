<script>
// We need a few arrays to populate project dropdown.
// When client selection changes, the project dropdown must be re-populated with only relevant projects.
// Format:
// project_ids[143] = "325,370,390,400";  // Comma-separated list of project ids for client.
// project_names[325] = "Time Tracker";   // Project name.

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

// We need a couple of array-like objects, one for associated task ids, another for task names.
// For performance, and because associated arrays are frowned upon in JavaScript, we'll use a simple object
// with properties for project tasks. Format:

// obj_tasks.p325 = "100,101,302,303,304"; // Tasks ids for project 325 are "100,101,302,303,304".
// obj_tasks.p408 = "100,302";  // Tasks ids for project 408 are "100,302".

// Create an object for task ids.
obj_tasks = {};
var project_prefix = "p"; // Prefix for project property.
var project_property;

// Populate obj_tasks with task ids for each relevant project.
  project_property = project_prefix + 486;
  obj_tasks[project_property] = "1,5";
  project_property = project_prefix + 568;
  obj_tasks[project_property] = "1,5";
  project_property = project_prefix + 537;
  obj_tasks[project_property] = "1,5";
  project_property = project_prefix + 584;
  obj_tasks[project_property] = "1,5";
  project_property = project_prefix + 496;
  obj_tasks[project_property] = "1,5";
  project_property = project_prefix + 14;
  obj_tasks[project_property] = "20,7,5,4,16,9,6,21,13,23,10,12,14,3,11,8";

// Prepare an array of task names.
// Format: task_names[0] = Array(100, 'Coding'), task_names[1] = Array(302, 'Debugging'), etc...
// First element = task_id, second element = task name.
task_names = new Array();
var idx = 0;
  task_names[idx] = new Array(20, "Accounting");
  idx++;
  task_names[idx] = new Array(7, "Army Service");
  idx++;
  task_names[idx] = new Array(1, "Consulting");
  idx++;
  task_names[idx] = new Array(5, "Development");
  idx++;
  task_names[idx] = new Array(4, "General");
  idx++;
  task_names[idx] = new Array(16, "HR");
  idx++;
  task_names[idx] = new Array(9, "Illness");
  idx++;
  task_names[idx] = new Array(6, "Management");
  idx++;
  task_names[idx] = new Array(21, "Marketing");
  idx++;
  task_names[idx] = new Array(13, "Meeting");
  idx++;
  task_names[idx] = new Array(23, "Personal Absence");
  idx++;
  task_names[idx] = new Array(10, "Sales");
  idx++;
  task_names[idx] = new Array(12, "Subscription");
  idx++;
  task_names[idx] = new Array(3, "Training");
  idx++;
  task_names[idx] = new Array(11, "Transport");
  idx++;
  task_names[idx] = new Array(8, "Vacation");
  idx++;

// empty_label is the mandatory top option in dropdowns.
empty_label = '--- all ---';

// inArray - determines whether needle is in haystack array.
function inArray(needle, haystack) {
  var length = haystack.length;
  for(var i = 0; i < length; i++) {
    if(haystack[i] == needle) return true;
  }
  return false;
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
  dropdown.options[0] = new Option(empty_label, '', true);

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
    dropdown.options[0] = new Option(empty_label, '', true);
  }
}


// The fillTaskDropdown function populates the task combo box with
// tasks associated with a selected project_id.
function fillTaskDropdown(project_id) {
  var str_task_ids;
  // Get a string of comma-separated task ids.
  if (project_id) {
    var property = "p" + project_id;
    str_task_ids = obj_tasks[property];
  }
  if (str_task_ids) {
    var task_ids = new Array(); // Array of task ids.
    task_ids = str_task_ids.split(",");
  }

  var dropdown = document.getElementById("task");
  // Determine previously selected item.
  var selected_item = dropdown.options[dropdown.selectedIndex].value;

  // Remove existing content.
  dropdown.length = 0;
  // Add mandatory top option.
  dropdown.options[0] = new Option(empty_label, '', true);

  // Populate the dropdown with associated tasks.
  len = task_names.length;
  var dropdown_idx = 0;
  for (var i = 0; i < len; i++) {
    if (!project_id) {
      // No project is selected. Fill in all tasks.
      dropdown.options[dropdown_idx+1] = new Option(task_names[i][1], task_names[i][0]);
      dropdown_idx++;
    } else if (str_task_ids) {
      // Project is selected and has associated tasks. Fill them in.
      if (inArray(task_names[i][0], task_ids)) {
        dropdown.options[dropdown_idx+1] = new Option(task_names[i][1], task_names[i][0]);
        dropdown_idx++;
      }
    }
  }

  // If a previously selected item is still in dropdown - select it.
  if (dropdown.options.length > 0) {
    for (var i = 0; i < dropdown.options.length; i++) {
      if (dropdown.options[i].value == selected_item)  {
        dropdown.options[i].selected = true;
      }
    }
  }
}

// Build JavaScript array for assigned projects out of passed in PHP array.
var assigned_projects = new Array();

// selectAssignedUsers is called when a project is changed in project dropdown.
// It selects users on the form who are assigned to this project.
function selectAssignedUsers(project_id) {
  var user_id;
  var len;

  for (var i = 0; i < document.reportForm.elements.length; i++) {
    if ((document.reportForm.elements[i].type == 'checkbox') && (document.reportForm.elements[i].name == 'users[]')) {
      user_id = document.reportForm.elements[i].value;
      if (project_id)
        document.reportForm.elements[i].checked = false;
      else
        document.reportForm.elements[i].checked = true;

      if(assigned_projects[user_id] != undefined)
        len = assigned_projects[user_id].length;
      else
        len = 0;

      if (project_id != '')
        for (var j = 0; j < len; j++) {
          if (project_id == assigned_projects[user_id][j]) {
            document.reportForm.elements[i].checked = true;
            break;
          }
        }
    }
  }
}

// handleCheckboxes - unmarks and hides the "Totals only" checkbox when
// "no grouping" is selected in the associated group by dropdowns.
function handleCheckboxes() {
  var totalsOnlyCheckbox = document.getElementById("chtotalsonly");
  var totalsOnlyLabel = document.getElementById("totals_only_label");
  var groupBy1 = document.getElementById("group_by1");
  var groupBy2 = document.getElementById("group_by2");
  var groupBy3 = document.getElementById("group_by3");
  var grouping = false;
  if ((groupBy1 != null && "no_grouping" != groupBy1.value) ||
      (groupBy2 != null && "no_grouping" != groupBy2.value) ||
      (groupBy3 != null && "no_grouping" != groupBy3.value)) {
    grouping = true;
  }
  if (grouping) {
    // Show the "Totals only" checkbox.
    totalsOnlyCheckbox.style.visibility = "visible";
    totalsOnlyLabel.style.visibility = "visible";
  } else {
    // Unmark and hide the "Totals only" checkbox.
    totalsOnlyCheckbox.checked = false;
    totalsOnlyCheckbox.style.visibility = "hidden";
    totalsOnlyLabel.style.visibility = "hidden";
  }
}
</script>
