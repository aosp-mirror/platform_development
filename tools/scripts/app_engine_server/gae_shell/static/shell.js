// Copyright 2007 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview
 * Javascript code for the interactive AJAX shell.
 *
 * Part of http://code.google.com/p/google-app-engine-samples/.
 *
 * Includes a function (shell.runStatement) that sends the current python
 * statement in the shell prompt text box to the server, and a callback
 * (shell.done) that displays the results when the XmlHttpRequest returns.
 *
 * Also includes cross-browser code (shell.getXmlHttpRequest) to get an
 * XmlHttpRequest.
 */

/**
 * Shell namespace.
 * @type {Object}
 */
var shell = {}

/**
 * The shell history. history is an array of strings, ordered oldest to
 * newest. historyCursor is the current history element that the user is on.
 *
 * The last history element is the statement that the user is currently
 * typing. When a statement is run, it's frozen in the history, a new history
 * element is added to the end of the array for the new statement, and
 * historyCursor is updated to point to the new element.
 *
 * @type {Array}
 */
shell.history = [''];

/**
 * See {shell.history}
 * @type {number}
 */
shell.historyCursor = 0;

/**
 * A constant for the XmlHttpRequest 'done' state.
 * @type Number
 */
shell.DONE_STATE = 4;

/**
 * A cross-browser function to get an XmlHttpRequest object.
 *
 * @return {XmlHttpRequest?} a new XmlHttpRequest
 */
shell.getXmlHttpRequest = function() {
  if (window.XMLHttpRequest) {
    return new XMLHttpRequest();
  } else if (window.ActiveXObject) {
    try {
      return new ActiveXObject('Msxml2.XMLHTTP');
    } catch(e) {
      return new ActiveXObject('Microsoft.XMLHTTP');
    }
  }

  return null;
};

/**
 * This is the prompt textarea's onkeypress handler. Depending on the key that
 * was pressed, it will run the statement, navigate the history, or update the
 * current statement in the history.
 *
 * @param {Event} event the keypress event
 * @return {Boolean} false to tell the browser not to submit the form.
 */
shell.onPromptKeyPress = function(event) {
  var statement = document.getElementById('statement');

  if (this.historyCursor == this.history.length - 1) {
    // we're on the current statement. update it in the history before doing
    // anything.
    this.history[this.historyCursor] = statement.value;
  }

  // should we pull something from the history?
  if (event.ctrlKey && event.keyCode == 38 /* up arrow */) {
    if (this.historyCursor > 0) {
      statement.value = this.history[--this.historyCursor];
    }
    return false;
  } else if (event.ctrlKey && event.keyCode == 40 /* down arrow */) {
    if (this.historyCursor < this.history.length - 1) {
      statement.value = this.history[++this.historyCursor];
    }
    return false;
  } else if (!event.altKey) {
    // probably changing the statement. update it in the history.
    this.historyCursor = this.history.length - 1;
    this.history[this.historyCursor] = statement.value;
  }

  // should we submit?
  var ctrlEnter = (document.getElementById('submit_key').value == 'ctrl-enter');
  if (event.keyCode == 13 /* enter */ && !event.altKey && !event.shiftKey &&
      event.ctrlKey == ctrlEnter) {
    return this.runStatement();
  }
};

/**
 * The XmlHttpRequest callback. If the request succeeds, it adds the command
 * and its resulting output to the shell history div.
 *
 * @param {XmlHttpRequest} req the XmlHttpRequest we used to send the current
 *     statement to the server
 */
shell.done = function(req) {
  if (req.readyState == this.DONE_STATE) {
    var statement = document.getElementById('statement')
    statement.className = 'prompt';

    // add the command to the shell output
    var output = document.getElementById('output');

    output.value += '\n>>> ' + statement.value;
    statement.value = '';

    // add a new history element
    this.history.push('');
    this.historyCursor = this.history.length - 1;

    // add the command's result
    var result = req.responseText.replace(/^\s*|\s*$/g, '');  // trim whitespace
    if (result != '')
      output.value += '\n' + result;

    // scroll to the bottom
    output.scrollTop = output.scrollHeight;
    if (output.createTextRange) {
      var range = output.createTextRange();
      range.collapse(false);
      range.select();
    }
  }
};

/**
 * This is the form's onsubmit handler. It sends the python statement to the
 * server, and registers shell.done() as the callback to run when it returns.
 *
 * @return {Boolean} false to tell the browser not to submit the form.
 */
shell.runStatement = function() {
  var form = document.getElementById('form');

  // build a XmlHttpRequest
  var req = this.getXmlHttpRequest();
  if (!req) {
    document.getElementById('ajax-status').innerHTML =
        "<span class='error'>Your browser doesn't support AJAX. :(</span>";
    return false;
  }

  req.onreadystatechange = function() { shell.done(req); };

  // build the query parameter string
  var params = '';
  for (i = 0; i < form.elements.length; i++) {
    var elem = form.elements[i];
    if (elem.type != 'submit' && elem.type != 'button' && elem.id != 'caret') {
      var value = escape(elem.value).replace(/\+/g, '%2B'); // escape ignores +
      params += '&' + elem.name + '=' + value;
    }
  }

  // send the request and tell the user.
  document.getElementById('statement').className = 'prompt processing';
  req.open(form.method, form.action + '?' + params, true);
  req.setRequestHeader('Content-type',
                       'application/x-www-form-urlencoded;charset=UTF-8');
  req.send(null);

  return false;
};
