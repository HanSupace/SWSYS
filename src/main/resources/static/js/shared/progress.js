(function () {
    function clampPercent(value) {
        return Math.max(0, Math.min(100, Number(value) || 0));
    }

    function applyProgressWidths(root) {
        (root || document).querySelectorAll('[data-progress]').forEach(function (element) {
            element.style.width = clampPercent(element.dataset.progress) + '%';
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        applyProgressWidths(document);
    });

    window.PLIA_PROGRESS = {
        apply: applyProgressWidths
    };
}());
