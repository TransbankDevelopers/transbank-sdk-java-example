document.addEventListener('DOMContentLoaded', function () {
  const forms = document.querySelectorAll('[data-loading-form="true"]');
  const buttons = document.querySelectorAll('[data-loading-button="true"]');

  forms.forEach(function (form) {
    form.addEventListener('submit', function () {
      buttons.forEach(function (btn) {
        btn.disabled = true;
        btn.classList.add('is-disabled');
      });

      const btn = form.querySelector('[data-loading-button="true"]');
      if (btn) {
        btn.classList.add('is-loading');
      }
    });
  });
});
