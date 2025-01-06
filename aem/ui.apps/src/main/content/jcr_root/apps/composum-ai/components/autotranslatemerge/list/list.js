/* AIGenVersion(56b7dc3a, list.js.prompt-23a21ed0, 8.2AutomaticTranslationMergeTool.md-4fe76a18, list.html-691163c8) */

document.addEventListener("DOMContentLoaded", function () {
    const rteContainers = document.querySelectorAll(".rte-container");

    rteContainers.forEach(container => {
        const editor = container.querySelector(".rte-editor");
        const toolbar = container.querySelector(".rte-toolbar");
        const resetButton = container.querySelector(".reset-editor");
        const saveButton = container.querySelector(".save-editor");

        toolbar.addEventListener("click", event => {
            const command = event.target.dataset.command;
            if (command) {
                document.execCommand(command, false, null);
                editor.focus(); // Refocus the editor after the action
            }
        });

        // Handle Reset button
        resetButton.addEventListener("click", () => {
            const originalContent = editor.getAttribute('data-original-content');
            if (originalContent !== null) {
                editor.innerHTML = originalContent;
                saveButton.disabled = true;
            }
        });

        // Handle Save button
        editor.addEventListener("input", () => {
            const currentContent = editor.innerHTML;
            const originalContent = editor.getAttribute('data-original-content');
            saveButton.disabled = (currentContent === originalContent);
        });

        saveButton.addEventListener("click", () => {
            const updatedContent = editor.innerHTML;
            // Implement save functionality here
            editor.setAttribute('data-original-content', updatedContent);
            saveButton.disabled = true;
        });

        /* editor.addEventListener("blur", function () {
            const updatedContent = editor.innerHTML;
            console.log("Updated content:", updatedContent);
            // Save the content (custom implementation needed)
        }); */
    });

    const tableBody = document.querySelector(".propertiestable");
    tableBody.addEventListener("click", function (event) {
        const target = event.target;
        if (target.matches(".copy-to-editor, .append-to-editor, .intelligent-merge")) {
            const row = target.closest("coral-table-row");
            const newTransCell = row.querySelector(".newtrans");
            const editorContainer = row.querySelector(".rte-container");
            const editor = editorContainer.querySelector(".rte-editor");
            const newText = newTransCell.textContent.trim();

            if (target.classList.contains("copy-to-editor")) {
                editor.innerHTML = newText;
                editor.setAttribute('data-original-content', newText);
                row.querySelector(".save-editor").disabled = true;
            } else if (target.classList.contains("append-to-editor")) {
                editor.innerHTML += newText;
                row.querySelector(".save-editor").disabled = false;
            } else if (target.classList.contains("intelligent-merge")) {
                // Placeholder for intelligent merge functionality
                // This should be replaced with actual AI merge logic
                const mergedText = intelligentMerge(editor.innerHTML, newText);
                editor.innerHTML = mergedText;
                editor.setAttribute('data-original-content', mergedText);
                row.querySelector(".save-editor").disabled = false;
            }
        }
    });

    function intelligentMerge(currentText, newText) {
        // Implement AI-based merge logic here
        // For now, it simply appends the new text
        return currentText + newText;
    }
});

console.log("AutoTranslateMerge list.js loaded");
