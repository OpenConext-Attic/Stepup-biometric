$(function () {

  $("form.reset-secret").on("submit", function (e) {
    var clientId = $(this).data("client");
    return confirm("Are you sure you want to reset the secret for client: " + clientId + "?");
  });
});
