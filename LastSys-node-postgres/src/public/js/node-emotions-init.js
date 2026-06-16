(function () {
  var emotions = window.PLIA_EMOTIONS || [];

  function styleFor(emotion) {
    return '--emotion-bg: ' + emotion.background + '; --emotion-border: ' + emotion.border + '; --emotion-text: ' + emotion.color + ';';
  }

  function renderMainButtons() {
    var box = document.getElementById('emotion-button-box');
    if (!box || emotions.length === 0) {
      return;
    }
    box.innerHTML = emotions.map(function (emotion) {
      return '<button class="emotion-choice" type="button" data-emotion-id="' + emotion.id + '" data-emotion-code="' + emotion.code + '" data-emotion="' + emotion.label + '" style="' + styleFor(emotion) + '" aria-pressed="false">' + emotion.label + '</button>';
    }).join('');
  }

  function renderMapPicker() {
    var picker = document.querySelector('.emotion-picker');
    if (!picker || emotions.length === 0) {
      return;
    }
    picker.innerHTML = emotions.map(function (emotion, index) {
      return '<label class="emotion-option" data-emotion-id="' + emotion.id + '" data-emotion-code="' + emotion.code + '" style="' + styleFor(emotion) + '">' +
        '<input type="radio" name="emotion" value="' + emotion.value + '" data-emotion-code="' + emotion.code + '" data-label="' + emotion.label + '" data-color="' + emotion.color + '"' + (index === 0 ? ' checked' : '') + '>' +
        '<span>' + emotion.label + '</span>' +
      '</label>';
    }).join('');
  }

  document.addEventListener('DOMContentLoaded', function () {
    renderMainButtons();
    renderMapPicker();
  });
})();
