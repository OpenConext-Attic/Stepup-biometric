var currentLocale = $('input[name=locale]').val();

var biometricTranslations = {
  'en': {
    'expired': '<p>Your Biometric session has expired.</p><p>Refresh the page for a new QR code or cancel the operation to return to the Strong Authentication overview.</p>',
    'complete': 'Je Biometric account is gereed voor gebruik. Je gaat automatisch terug naar het Sterke Authenticatie overzicht'
  },
  'nl': {
    'expired': '<p>Je Biometric sessie is verlopen.</p><p>Ververs de pagina voor een nieuwe QR code of annulleer het authenticeren om terug te gaan naar het Sterke Authenticatie overzicht.</p>',
    'complete': '<p>Your Biometric account is ready to use. You wil be automatically be redirected to the Strong Authentication overview<p>'
  }
};

$('#countdown').pietimer({
    seconds: 10 * 60,
    color: '#4DB3CF',
    height: 60,
    width: 60
  },
  function () {
  });

$('#countdown').pietimer('start');

$.ajaxSetup({
  headers: {'X-POLLING': 'true'}
});

function pollStatus() {
  $.get('/poll', function (data) {
    switch (data.status) {
      case 'pending':
        window.setTimeout(pollStatus, 1000);
        break;
      case 'complete':
        $('#status').html(biometricTranslations[currentLocale][status]);
        window.setTimeout(function () {
          $.post('/done');
        }, 1000)
        break;
      case 'expired' :
        $('#status').html(biometricTranslations[currentLocale][status]);
        break;
    }
  })
}

window.setTimeout(pollStatus, 1000);

