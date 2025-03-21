document.addEventListener("DOMContentLoaded", () => {
    // Get DOM elements
    const selectAllBtn = document.getElementById("select-all");
    const clearAllBtn = document.getElementById("clear-all");
    const checkboxes = document.querySelectorAll('input[name="selectedModels"]');
    const translationText = document.getElementById("translation-text");
    const targetLanguageInput = document.getElementById("target-language");
    const submitButton = document.getElementById("submit-button");

    // Restore textarea state from localStorage
    const savedText = localStorage.getItem("composumAI-autotranslatemodelcompare-translationText");
    if (savedText !== null) {
        translationText.value = savedText;
    }

    // Restore target language state from localStorage
    const savedTargetLanguage = localStorage.getItem("composumAI-autotranslatemodelcompare-targetLanguage");
    if (savedTargetLanguage !== null) {
        targetLanguageInput.value = savedTargetLanguage;
    }

    // Restore checkbox state from localStorage
    const savedModels = localStorage.getItem("composumAI-autotranslatemodelcompare-selectedModels");
    if (savedModels) {
        const savedArray = JSON.parse(savedModels);
        checkboxes.forEach(chk => {
            chk.checked = savedArray.includes(chk.value);
        });
    }

    // Update localStorage when the textarea value changes
    translationText.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-translationText", translationText.value);
    });

    // Update localStorage when the target language value changes
    targetLanguageInput.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-targetLanguage", targetLanguageInput.value);
    });

    // Update localStorage when any checkbox is toggled
    checkboxes.forEach(chk => {
        chk.addEventListener("change", () => {
            const selected = Array.from(checkboxes)
                .filter(c => c.checked)
                .map(c => c.value);
            localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(selected));
        });
    });

    // Select All button functionality
    selectAllBtn.addEventListener("click", () => {
        checkboxes.forEach(chk => (chk.checked = true));
        const allSelected = Array.from(checkboxes).map(chk => chk.value);
        localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(allSelected));
    });

    // Clear All button functionality
    clearAllBtn.addEventListener("click", () => {
        checkboxes.forEach(chk => (chk.checked = false));
        localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify([]));
    });

    // The submit button should refresh the cq_csrf_token input and then submit the form
    submitButton.addEventListener("click", (event) => {
        event.preventDefault();
        fetch('/libs/granite/csrf/token.json').then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            document.getElementById('loading-spinner').style.display='inline';
            return response.json();
        }).then(data => {
            const csrfTokenInput = document.querySelector('input[name=":cq_csrf_token"]');
            csrfTokenInput.value = data.token;
            document.forms[0].submit();
        }).catch(error => {
            document.getElementById('error-message').innerText = "Error: " + error.message;
            console.error('There was a problem with the fetch operation:', error);
        });
    });

});
