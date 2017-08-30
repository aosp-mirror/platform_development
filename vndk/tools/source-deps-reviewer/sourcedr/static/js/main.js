(function () {
  'use strict';

  var ccounter = 0;
  var counter = 0;
  var current_item = null;

  function getSelText() {
    let txt = window.getSelection();
    $('#selected_text').val(txt);
    $('#code_file_path').val($('#browsing_file_path').text());
    return txt;
  }

  function taskHtml(text, cnt) {
    return '<li><span class="display" id="dep' + cnt + '">' + text +
      '</span>' + '<input type="text" class="edit" style="display:none"/>' +
      '<input type="submit" class="delete" value="X">' +'</li>';
  }

  function codeHtml(text, cnt) {
    return '<li><span id="code' + cnt + '">' + text +
      '</span><input type="submit" class="delete" value="X">' + '</li>';
  }

  function itemHtml(done, text) {
    let atag = document.createElement('a');
    atag.innerText = text;
    if (done) {
      atag.className = 'list-group-item list-group-item-success';
    } else {
      atag.className = 'list-group-item list-group-item-danger';
    }
    let pretag = document.createElement('pre');
    pretag.appendChild(atag);
    pretag.onclick = setItem;
    return pretag;
  }

  function grepResultHtml(items) {
    let ret = document.createElement('p');
    for (let i = 0; i < items.length; i++) {
      let path = document.createElement('span');
      path.style.color = 'purple';
      path.style.fontSize = '20px';
      path.innerHTML = items[i][0];
      ret.appendChild(path);
      ret.appendChild(document.createElement('br'));
      for (let j = 0; j < items[0][1].length; j++) {
        let line_no = items[i][1][j][0];
        let content = items[i][1][j][1];
        let line_html = document.createElement('font');
        line_html.style.color = 'green';
        line_html.style.fontSize = '18px';
        line_html.innerHTML = line_no + ':';
        ret.appendChild(line_html);
        let content_html = document.createElement('span');
        content_html.style.fontSize = '18px';
        content_html.appendChild(document.createTextNode(content));
        ret.appendChild(content_html);
        ret.appendChild(document.createElement('br'));
      }
    }
    return ret;
  }

  function enterTask() {
    let text = $('#enter_deps').val();
    $('#deps_list').append(taskHtml(text, counter));
    $('.delete').click(function () {
      $(this).parent().remove();
    });
    counter++;
    return false;
  }

  function setTask(deps) {
    $('#deps_list').empty();
    counter = 0;
    let len = deps.length;
    for (let i = 0; i < len; i++) {
      let text = deps[i];
      $('#deps_list').append(taskHtml(text, counter));
      $('.delete').click(function () {
        $(this).parent().remove();
      });
      counter++;
    }
  }

  function enterCode() {
    let text = $('#code_file_path').val() + ':' + $('#selected_text').val();
    $('#code_list').append(codeHtml(text, ccounter));
    $('.delete').click(function () {
      $(this).parent().remove();
    });
    ccounter++;
    return false;
  }

  function setCode(codes) {
    $('#code_list').empty();
    ccounter = 0;
    let len = codes.length;
    for (let i = 0; i < len; i++) {
      let text = codes[i];
      $('#code_list').append(codeHtml(text, ccounter));
      $('.delete').click(function () {
        $(this).parent().remove();
      });
      ccounter++;
    }
  }

  $(document).ready(onLoad);

  function onLoad() {
    $.getJSON('/get_started', {
    }, function (data) {
      $('#item_list').empty();
      $('#pattern_list').empty();

      const lst = JSON.parse(data.lst);
      const done = JSON.parse(data.done);
      const pattern_lst = JSON.parse(data.pattern_lst);
      let len = done.length;
      for (let i = 0; i < len; i++) {
        $('#item_list').append(itemHtml(done[i], lst[i]));
      }
      len = pattern_lst.length;
      for (let i = 0; i < len; i++) {
        $('#pattern_list').append('<li>' + pattern_lst[i] + '</li>');
      }
      $('#path_prefix').text(data.path_prefix);
    });
  }

  function saveAll() {
    let path = $('#file_path').text();
    let line_no = $('#line_no').text();

    let deps = new Array();
    for (let i = 0; i < counter; i++) {
      if ($('#dep' + i).length) {
        deps.push($('#dep' + i).text());
      }
    }
    let codes = new Array();
    for (let i = 0; i < ccounter; i++) {
      if ($('#code' + i).length) {
        codes.push($('#code' + i).text());
      }
    }

    if (path == '' || line_no == '') {
      return false;
    }
    if (deps.length > 0) {
        current_item.className = 'list-group-item list-group-item-success';
    } else {
        current_item.className = 'list-group-item list-group-item-danger';
    }
    $.getJSON('/save_all', {
      label: $(current_item).text(),
      deps: JSON.stringify(deps),
      codes: JSON.stringify(codes)
    });
    return false;
  }

  function setBrowsingFile(path) {
    $('#browsing_file_path').text(path);
    $.getJSON('/get_file', {
      path: path
    }, function (data) {
      $('#browsing_file').children().first().text(data.result);
      let obj = $('#browsing_file').children().first();
      Prism.highlightElement($('#code')[0]);
    });
  }

  function setHighlightLine(line_no) {
    $('#browsing_file').attr('data-line', line_no);
  }

  function setGotoPatternLine(line_no) {
    $('#goto_pattern_line').attr('href', '#browsing_file.' + line_no);
  }

  function unsetHighlightLine() {
    $('#browsing_file').removeAttr('data-line');
  }

  function removeAnchor() {
    // Remove the # from the hash,
    // as different browsers may or may not include it
    var hash = location.hash.replace('#','');
    if (hash != '') {
      // Clear the hash in the URL
      location.hash = '';
    }
  };

  function setItem(evt) {
    removeAnchor();
    let item = evt.target;
    current_item = item;
    let name = $(item).text().split(':');
    let file = name[0];
    let line_no = name[1];
    $('#file_path').text(file);
    $('#line_no').text(line_no);

    $.getJSON('/load_file', {
      path: $(item).text()
    }, function (data) {
      let deps = JSON.parse(data.deps);
      let codes = JSON.parse(data.codes);
      setTask(deps);
      setCode(codes);
    });

    setBrowsingFile(file);
    setHighlightLine(line_no);
    setGotoPatternLine(line_no);
    $('#selected_text').val('');
    $('#code_file_path').val('');
    $('#enter_deps').val('');
    $('html,body').scrollTop(0);
    return false;
  }

  $('#go_form').submit(function () {
    // get all the inputs into an array.
    const $inputs = $('#go_form :input');
    let values = {};
    $inputs.each(function () {
      values[this.name] = $(this).val();
    });
    let path = $('#path_prefix').text() +
               $('input[name="browsing_path"]').val();
    setBrowsingFile(path);
    unsetHighlightLine();
    return false;
  });

  $('#add_pattern').submit(function () {
    const $inputs = $('#add_pattern :input');
    let values = {};
    $inputs.each(function () {
      values[this.name] = $(this).val();
    });
    $.getJSON('/add_pattern', {
      pattern: values['pattern'],
      is_regex: $('#is_regex').is(':checked') ? 1 : 0
    });
    return true;
  });

  $('#temporary_search').submit(function() {
    const $inputs = $('#temporary_search :input');
    let values = {};
    $inputs.each(function () {
      values[this.name] = $(this).val();
    });
    $('#modal_title').text(values['pattern']);
    $.getJSON('/temporary_search', {
      path: $('#file_path').text(),
      pattern: values['pattern'],
      is_regex: $('#is_regex2').is(':checked') ? 1 : 0
    }, function (data) {
        $('#modal_body').append(grepResultHtml(JSON.parse(data.result)));
        $('#myModal').modal('show');
    });
    return false;
  });
  // clear previous html code in modal on hide
  $('#myModal').on('hidden.bs.modal', function () {
    $('#modal_body').empty();
  })

  $('#add_deps').submit(enterTask);
  $('#add_code').submit(enterCode);
  $('#save_all').submit(saveAll);
  $('#get_selection').click(getSelText);
})();
