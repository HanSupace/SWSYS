(function () {
        var modal = document.getElementById('delete-modal');
        var openButton = document.getElementById('open-delete-modal');
        var closeButton = document.getElementById('delete-modal-cancel');
        var backdrop = document.getElementById('delete-modal-backdrop');
        var input = document.getElementById('delete-confirm-input');
        var submit = document.getElementById('delete-submit');

        function openModal() {
            modal.hidden = false;
            input.value = '';
            submit.disabled = true;
            window.setTimeout(function () {
                input.focus();
            }, 80);
        }

        function closeModal() {
            modal.hidden = true;
            openButton.focus();
        }

        openButton.addEventListener('click', openModal);
        closeButton.addEventListener('click', closeModal);
        backdrop.addEventListener('click', closeModal);
        input.addEventListener('input', function () {
            submit.disabled = input.value.trim() !== '탈퇴';
        });
        window.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !modal.hidden) {
                closeModal();
            }
        });
    })();
