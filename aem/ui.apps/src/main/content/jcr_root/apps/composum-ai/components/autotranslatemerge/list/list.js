document.addEventListener("DOMContentLoaded", function () {
    const rteContainers = document.querySelectorAll(".rte-container");

    rteContainers.forEach(container => {
        const editor = container.querySelector(".rte-editor");
        const toolbar = container.querySelector(".rte-toolbar");

        toolbar.addEventListener("click", event => {
            const command = event.target.dataset.command;
            if (command) {
                document.execCommand(command, false, null);
                editor.focus(); // Refocus the editor after the action
            }
        });

        /* editor.addEventListener("blur", function () {
            const updatedContent = editor.innerHTML;
            console.log("Updated content:", updatedContent);
            // Save the content (custom implementation needed)
        }); */
    });
});
