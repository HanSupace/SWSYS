document.addEventListener('DOMContentLoaded', () => {
        const modeInputs = document.querySelectorAll('input[name="mode"]');
        const ruleFields = document.querySelectorAll('[data-rule-field]');
        const ruleFieldGroup = document.getElementById('mission-rule-fields');

        const updateRuleFields = () => {
            const selectedMode = document.querySelector('input[name="mode"]:checked')?.value || 'PLAIN';
            const disabled = selectedMode !== 'RULE_BASED';

            ruleFields.forEach((field) => field.disabled = disabled);
            ruleFieldGroup?.classList.toggle('is-disabled', disabled);
        };

        modeInputs.forEach((input) => input.addEventListener('change', updateRuleFields));
        updateRuleFields();
    });
