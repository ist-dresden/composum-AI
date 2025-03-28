document.addEventListener("DOMContentLoaded", () => {
    // Prevent form submission via Enter key on non-textarea inputs
    const formElement = document.querySelector('form');
    formElement.addEventListener("keydown", function(e) {
        if (e.key === "Enter" && e.target.tagName !== "TEXTAREA") {
            e.preventDefault();
        }
    });

    // Get DOM elements
    const selectAllBtn = document.getElementById("select-all");
    const clearAllBtn = document.getElementById("clear-all");
    const checkboxes = document.querySelectorAll('input[name="selectedModels"]');
    const translationText = document.getElementById("translation-text");
    const targetLanguageInput = document.getElementById("target-language");
    const submitButton = document.getElementById("submit-button");
    const additionalModelsInput = document.getElementById("additional-models");
    const additionalModelsContainer = document.getElementById("additional-models-container");

    // Restore translation text from localStorage
    const savedText = localStorage.getItem("composumAI-autotranslatemodelcompare-translationText");
    if (savedText !== null) {
        translationText.value = savedText;
    }

    // Restore target language from localStorage
    const savedTargetLanguage = localStorage.getItem("composumAI-autotranslatemodelcompare-targetLanguage");
    if (savedTargetLanguage !== null) {
        targetLanguageInput.value = savedTargetLanguage;
    }

    // Restore additional models from localStorage
    const savedAdditionalModels = localStorage.getItem("composumAI-autotranslatemodelcompare-additionalModels");
    if (savedAdditionalModels !== null) {
        additionalModelsInput.value = savedAdditionalModels;
        updateAdditionalModelsCheckboxes();
    }

    // Restore additional instructions from localStorage and unfold details if non-empty
    const additionalInstructions = document.getElementById("additional-instructions");
    const additionalInstructionsDetails = document.getElementById("additional-instructions-details");
    const savedInstructions = localStorage.getItem("composumAI-autotranslatemodelcompare-instructions");
    if (savedInstructions !== null) {
        additionalInstructions.value = savedInstructions;
        if (savedInstructions.trim() !== "") {
            additionalInstructionsDetails.open = true;
        }
    }

    // Restore static checkbox state from localStorage
    const savedModels = localStorage.getItem("composumAI-autotranslatemodelcompare-selectedModels");
    if (savedModels) {
        const savedArray = JSON.parse(savedModels);
        checkboxes.forEach(chk => {
            chk.checked = savedArray.includes(chk.value);
        });
    }

    // Event listener for static checkboxes
    checkboxes.forEach(chk => {
        chk.addEventListener("change", () => {
            const allCheckboxes = document.querySelectorAll('input[name="selectedModels"]');
            const selected = Array.from(allCheckboxes)
                .filter(c => c.checked)
                .map(c => c.value);
            localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(selected));
        });
    });

    // Save translation text on input
    translationText.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-translationText", translationText.value);
    });

    // Save target language on input
    targetLanguageInput.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-targetLanguage", targetLanguageInput.value);
    });

    // Save additional instructions on input
    additionalInstructions.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-instructions", additionalInstructions.value);
    });

    function updateAdditionalModelsCheckboxes() {
        const models = additionalModelsInput.value.split(",")
            .map(m => m.trim())
            .filter(m => m.length > 0);
        additionalModelsContainer.innerHTML = "";
        models.forEach((model, index) => {
            const checkboxWrapper = document.createElement("div");
            checkboxWrapper.classList.add("form-check", "form-check-inline");

            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.classList.add("form-check-input");
            checkbox.name = "selectedModels";
            checkbox.value = model;
            checkbox.id = "additional-model-" + index;

            const savedSelectedModels = JSON.parse(localStorage.getItem("composumAI-autotranslatemodelcompare-selectedModels") || "[]");
            if (savedSelectedModels.includes(model)) {
                checkbox.checked = true;
            }

            const label = document.createElement("label");
            label.classList.add("form-check-label");
            label.htmlFor = checkbox.id;
            label.textContent = model;

            checkboxWrapper.appendChild(checkbox);
            checkboxWrapper.appendChild(label);
            additionalModelsContainer.appendChild(checkboxWrapper);

            checkbox.addEventListener("change", () => {
                const allCheckboxes = document.querySelectorAll('input[name="selectedModels"]');
                const selected = Array.from(allCheckboxes)
                    .filter(c => c.checked)
                    .map(c => c.value);
                localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(selected));
            });
        });
        
        // After updating additional models, remove any selected models that no longer exist in the checkboxes
        const allCheckboxes = document.querySelectorAll('input[name="selectedModels"]');
        const validModels = Array.from(allCheckboxes).map(chk => chk.value);
        let savedSelectedModels = JSON.parse(localStorage.getItem("composumAI-autotranslatemodelcompare-selectedModels") || "[]");
        const filteredSelectedModels = savedSelectedModels.filter(value => validModels.includes(value));
        localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(filteredSelectedModels));
        
        // Update all checkboxes to reflect the filtered selection
        allCheckboxes.forEach(chk => {
            chk.checked = filteredSelectedModels.includes(chk.value);
        });
    }

    // Event listener for additional models input
    additionalModelsInput.addEventListener("input", () => {
        localStorage.setItem("composumAI-autotranslatemodelcompare-additionalModels", additionalModelsInput.value);
        updateAdditionalModelsCheckboxes();
    });

    // Select All button functionality
    selectAllBtn.addEventListener("click", () => {
        const allCheckboxes = document.querySelectorAll('input[name="selectedModels"]');
        allCheckboxes.forEach(chk => chk.checked = true);
        const selected = Array.from(allCheckboxes).map(chk => chk.value);
        localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify(selected));
    });

    // Clear All button functionality
    clearAllBtn.addEventListener("click", () => {
        const allCheckboxes = document.querySelectorAll('input[name="selectedModels"]');
        allCheckboxes.forEach(chk => chk.checked = false);
        localStorage.setItem("composumAI-autotranslatemodelcompare-selectedModels", JSON.stringify([]));
    });

    // The submit button should refresh the cq_csrf_token input and then submit the form
    submitButton.addEventListener("click", (event) => {
        event.preventDefault();
        fetch('/libs/granite/csrf/token.json').then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            document.getElementById('loading-spinner').style.display = 'inline';
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
